package com.localstocks.intelligence;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.database.Cursor;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private final ExecutorService workers=Executors.newSingleThreadExecutor();
    private Store store;private LinearLayout body;private EditText search;private String tab="Discover";
    private static final int PICK_CSV=42;
    private static final int INK=Color.rgb(230,236,247),MUTED=Color.rgb(156,173,194),BG=Color.rgb(10,16,29),CARD=Color.rgb(23,34,51),ACCENT=Color.rgb(79,185,215);
    @Override public void onCreate(Bundle state){super.onCreate(state);store=new Store(this);show("Discover");}
    @Override protected void onDestroy(){workers.shutdown();store.close();super.onDestroy();}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private LinearLayout vertical(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private void label(LinearLayout parent,String txt,int size,int color){
        TextView t=new TextView(this);t.setText(txt);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(5),0,dp(5));parent.addView(t);
    }
    private Button button(LinearLayout p,String text,Runnable callback){
        Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setOnClickListener(v->callback.run());p.addView(b);return b;
    }
    private LinearLayout card(LinearLayout parent){
        LinearLayout v=vertical();v.setPadding(dp(15),dp(13),dp(15),dp(13));v.setBackgroundColor(CARD);
        LinearLayout.LayoutParams l=new LinearLayout.LayoutParams(-1,-2);l.setMargins(0,0,0,dp(12));parent.addView(v,l);return v;
    }
    private EditText field(LinearLayout p,String hint,String val){
        EditText e=new EditText(this);e.setSingleLine(true);e.setTextColor(INK);e.setHintTextColor(MUTED);
        e.setHint(hint);e.setText(val);p.addView(e,new LinearLayout.LayoutParams(-1,dp(52)));return e;
    }
    private String now(){return OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);}
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    private void openUrl(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception e){toast("No browser available");}}
    private void show(String next){
        tab=next;
        LinearLayout root=vertical();root.setBackgroundColor(BG);root.setPadding(dp(16),dp(20),dp(16),0);
        label(root,"STOCK INTELLIGENCE",23,INK);
        label(root,"Local-first · Indian equities · Research only",12,MUTED);
        LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);root.addView(nav);
        for(String name:new String[]{"Discover","Signals","Performance"}){
            Button b=new Button(this);b.setText(name);b.setAllCaps(false);b.setTextSize(11);
            nav.addView(b,new LinearLayout.LayoutParams(0,dp(52),1));b.setOnClickListener(v->show(name));
        }
        ScrollView scroll=new ScrollView(this);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        body=vertical();body.setPadding(0,dp(12),0,dp(24));scroll.addView(body);
        setContentView(root);
        if(next.equals("Discover"))discover();else if(next.equals("Signals"))signals();else performance();
    }
    private void discover(){
        LinearLayout panel=card(body);label(panel,"Find public coverage",18,INK);
        label(panel,"Manual RSS search finds leads, not verified trade calls. Review the original source before adding any recommendation.",13,MUTED);
        search=field(panel,"Symbol or company (e.g. TCS)","");
        button(panel,"Search news coverage",()->refreshSearch(search.getText().toString()));
        button(panel,"Add a verified recommendation manually",()->entryDialog(null));
        List<Store.Lead> leads=store.leads();
        label(body,"Discovery leads ("+leads.size()+")",18,INK);
        if(leads.isEmpty())label(body,"No collected coverage yet. Search a stock to begin.",14,MUTED);
        for(Store.Lead lead:leads){
            LinearLayout c=card(body);
            label(c,lead.symbol+" · "+lead.title,15,INK);
            label(c,(lead.publisher==null?"Unknown publisher":lead.publisher)+" · "+(lead.published==null?"Unknown original date":lead.published),12,MUTED);
            button(c,"Read original article",()->openUrl(lead.url));
            button(c,"Review and record trade levels",()->entryDialog(lead));
        }
    }
    private void refreshSearch(String symbol){
        symbol=symbol.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9.& -]","");
        if(symbol.length()<2||symbol.length()>40){toast("Enter a company name or symbol");return;}
        final String s=symbol;toast("Searching public RSS coverage for "+s);
        workers.execute(()->{
            int added=0;String error=null;
            try{
                String query=URLEncoder.encode(s+" NSE stock buy sell recommendation when:14d",StandardCharsets.UTF_8.name());
                String url="https://news.google.com/rss/search?q="+query+"&hl=en-IN&gl=IN&ceid=IN:en";
                HttpURLConnection conn=(HttpURLConnection)new URL(url).openConnection();
                conn.setConnectTimeout(12000);conn.setReadTimeout(16000);
                conn.setRequestProperty("User-Agent","StockIntelligence/0.1 personal RSS reader");
                try{
                    if(conn.getResponseCode()!=200)throw new Exception("HTTP "+conn.getResponseCode());
                    XmlPullParser parser=Xml.newPullParser();parser.setInput(conn.getInputStream(),"UTF-8");
                    int event=parser.getEventType();boolean item=false;String title="",link="",pub="",publisher="";
                    int count=0;
                    while(event!=XmlPullParser.END_DOCUMENT&&count<60){
                        if(event==XmlPullParser.START_TAG){
                            String tag=parser.getName();
                            if("item".equalsIgnoreCase(tag)){item=true;title="";link="";pub="";publisher="";}
                            else if(item && ("title".equals(tag)||"link".equals(tag)||"pubDate".equals(tag)||"source".equals(tag))){
                                String text=parser.nextText();
                                if("title".equals(tag))title=text;else if("link".equals(tag))link=text;
                                else if("pubDate".equals(tag))pub=text;else publisher=text;
                            }
                        } else if(event==XmlPullParser.END_TAG && "item".equalsIgnoreCase(parser.getName())){
                            item=false;count++;
                            String iso=pub;
                            try{iso=ZonedDateTime.parse(pub,DateTimeFormatter.RFC_1123_DATE_TIME).withZoneSameInstant(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);}catch(Exception ignored){}
                            if(link.startsWith("https://")&&store.saveLead(s,title,link,publisher,iso,now()))added++;
                        }
                        event=parser.next();
                    }
                } finally{conn.disconnect();}
            }catch(Exception e){error=e.getMessage();}
            final int n=added;final String failure=error;
            runOnUiThread(()->{toast(failure==null?"Found "+n+" new source leads":"Search unavailable: "+failure);show("Discover");});
        });
    }
    private void entryDialog(Store.Lead lead){
        LinearLayout form=vertical();form.setPadding(dp(16),dp(8),dp(16),0);
        EditText symbol=field(form,"NSE/BSE symbol",lead==null?"":lead.symbol);
        EditText analyst=field(form,"Named analyst (required)","");
        EditText firm=field(form,"Brokerage / institution (optional)","");
        EditText url=field(form,"Original source URL (https://...)",lead==null?"":lead.url);
        String date=lead!=null&&lead.published!=null&&lead.published.length()>=10?lead.published.substring(0,10):LocalDate.now().toString();
        EditText published=field(form,"Publication date YYYY-MM-DD",date);
        EditText entry=field(form,"Entry price ₹","");
        EditText target=field(form,"Target price ₹","");
        EditText stop=field(form,"Stop-loss price ₹","");
        final String[] side={"BUY"};
        LinearLayout options=new LinearLayout(this);form.addView(options);
        button(options,"BUY",()->{side[0]="BUY";toast("Direction BUY");});
        button(options,"SELL",()->{side[0]="SELL";toast("Direction SELL");});
        ScrollView sc=new ScrollView(this);sc.addView(form);
        new AlertDialog.Builder(this).setTitle("Verified recommendation · "+side[0])
          .setView(sc).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{
            try{
                String s=symbol.getText().toString().trim().toUpperCase(Locale.ROOT);
                String a=analyst.getText().toString().trim();
                String u=url.getText().toString().trim();
                String pub=published.getText().toString().trim();
                double e=Double.parseDouble(entry.getText().toString()),t=Double.parseDouble(target.getText().toString()),sl=Double.parseDouble(stop.getText().toString());
                LocalDate.parse(pub);
                if(!s.matches("[A-Z0-9.&-]{2,30}")||a.isEmpty()||!u.startsWith("https://")||e<=0||t<=0||sl<=0)throw new IllegalArgumentException("Missing or invalid fields");
                if(side[0].equals("BUY")&&!(sl<e&&e<t))throw new IllegalArgumentException("BUY requires stop < entry < target");
                if(side[0].equals("SELL")&&!(t<e&&e<sl))throw new IllegalArgumentException("SELL requires target < entry < stop");
                if(LocalDate.parse(pub).isAfter(LocalDate.now()))throw new IllegalArgumentException("Future publication date");
                boolean saved=store.saveCall(s,side[0],a,firm.getText().toString(),u,pub,now(),e,t,sl);
                toast(saved?"Verified call saved":"Duplicate analyst/day call ignored");show("Signals");
            }catch(Exception ex){toast("Not saved: "+ex.getMessage());}
          }).show();
    }
    private void signals(){
        List<Store.Call> all=store.calls();Set<String> symbols=new HashSet<>();
        for(Store.Call c:all)symbols.add(c.symbol);
        label(body,"Verified calls · "+all.size(),19,INK);
        label(body,"Evidence confidence ≠ probability of profitable trading. Only manually verified calls are scored.",13,MUTED);
        if(all.isEmpty())label(body,"No verified calls yet. Use Discover to record source-backed trades.",14,MUTED);
        List<String> sorted=new ArrayList<>(symbols);java.util.Collections.sort(sorted);
        for(String symbol:sorted){
            Analytics.Score score=Analytics.score(symbol,all);
            LinearLayout panel=card(body);
            label(panel,symbol+"  ·  "+score.direction+"  ·  "+score.value+"/75 provisional evidence points",17,INK);
            label(panel,score.explanation,12,MUTED);
            button(panel,"Investigate "+symbol+" in news",()->{show("Discover");refreshSearch(symbol);});
            for(Store.Call call:all)if(call.symbol.equals(symbol)){
                label(panel,call.direction+"  ₹"+call.entry+" → ₹"+call.target+"  SL ₹"+call.stop,14,call.direction.equals("BUY")?ACCENT:Color.rgb(242,163,148));
                label(panel,call.analyst+(call.institution.isEmpty()?"":" · "+call.institution)+" · Published "+call.published,12,MUTED);
                Analytics.Outcome o=Analytics.evaluate(call,store.bars(symbol,call.published));
                label(panel,"Outcome: "+o.status+(o.pct==null?"":String.format(Locale.US," (%.2f%% before costs)",o.pct)),12,MUTED);
                button(panel,"View source",()->openUrl(call.url));
            }
        }
    }
    private void performance(){
        LinearLayout p=card(body);label(p,"Historical outcome tracking",19,INK);
        label(p,"Import your own legal daily OHLC data. Prices stay on your phone. Only bars AFTER the published date are eligible for trade assessment.",13,MUTED);
        button(p,"Import daily prices CSV",()->{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,PICK_CSV);
        });
        label(p,"CSV columns: symbol,date,open,high,low,close (YYYY-MM-DD). No mock market prices are bundled into live results.",12,MUTED);
        List<Store.Call> all=store.calls();int target=0,stop=0,open=0,other=0;
        for(Store.Call c:all){
            Analytics.Outcome o=Analytics.evaluate(c,store.bars(c.symbol,c.published));
            if(o.status.equals("TARGET"))target++;else if(o.status.equals("STOP"))stop++;else if(o.status.startsWith("OPEN"))open++;else other++;
        }
        LinearLayout summary=card(body);
        label(summary,"Target hit "+target+"   ·   Stopped "+stop+"   ·   Open "+open+"   ·   Other "+other,15,INK);
        label(summary,"Same-bar target and stop is ambiguous. Entry-day exits are not assumed. Returns exclude fees, slippage and taxes.",12,MUTED);
        button(body,"See individual outcomes",()->show("Signals"));
    }
    @Override protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);
        if(req!=PICK_CSV||res!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        workers.execute(()->{
            int ok=0,bad=0;
            try(BufferedReader r=new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri),StandardCharsets.UTF_8))){
                String line;boolean first=true;
                while((line=r.readLine())!=null){
                    if(first){first=false;if(line.toLowerCase(Locale.ROOT).startsWith("symbol,"))continue;}
                    String[] v=line.split(",",-1);
                    try {
                        if(v.length<6)throw new IllegalArgumentException();
                        String s=v[0].trim().toUpperCase(Locale.ROOT),d=v[1].trim();
                        LocalDate.parse(d);
                        double o=Double.parseDouble(v[2]),h=Double.parseDouble(v[3]),l=Double.parseDouble(v[4]),c=Double.parseDouble(v[5]);
                        if(!s.matches("[A-Z0-9.&-]{2,30}")||l<=0||l>h||o<l||o>h||c<l||c>h)throw new IllegalArgumentException();
                        store.upsertPrice(s,d,o,h,l,c);ok++;
                    }catch(Exception ignored){bad++;}
                }
            }catch(Exception e){bad++;}final int a=ok,b=bad;
            runOnUiThread(()->{toast("Imported "+a+" price bars; skipped "+b+" invalid lines");show("Performance");});
        });
    }
}
