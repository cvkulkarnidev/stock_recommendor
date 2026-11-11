package com.localstocks.intelligence;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.net.Uri;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MainActivity extends Activity {
    private WebView web;
    private DiscoveryEngine discovery;
    private final ExecutorService jobs=Executors.newSingleThreadExecutor();
    private final AtomicBoolean refreshing=new AtomicBoolean(false);
    private volatile String lastMessage="";
    private volatile String lastError="";
    private volatile boolean pageReady=false;
    private volatile int windowDays=7;
    private volatile String deepStock="";
    private volatile String deepReport="";

    @SuppressLint("SetJavaScriptEnabled")
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(9,15,28));
        getWindow().setNavigationBarColor(Color.rgb(9,15,28));
        discovery=new DiscoveryEngine(this);
        web=new WebView(this);web.setBackgroundColor(Color.rgb(9,15,28));
        WebSettings settings=web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.addJavascriptInterface(new Bridge(),"StockNative");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView view,String url){
                pageReady=true;sendSnapshot();
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v,android.webkit.WebResourceRequest request){
                String url=request.getUrl().toString();
                if(url.startsWith("file:///android_asset/"))return false;
                openUrl(url);return true;
            }
        });
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }
    private void openUrl(String url){
        if(!url.startsWith("https://"))return;
        runOnUiThread(()->{
            try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}
            catch(Exception e){sendStatus(false,"Could not open external browser",true);}
        });
    }
    private void send(String message) {
        if(!pageReady||isFinishing())return;
        runOnUiThread(()->{
            if(web!=null)web.evaluateJavascript("window.receiveNative("+JSONObject.quote(message)+");",null);
        });
    }
    private void sendSnapshot(){
        JSONObject payload=new JSONObject();
        try{
            payload.put("kind","snapshot");
            payload.put("payload",discovery.snapshot(windowDays));
            payload.put("lastRefresh",getPreferences(MODE_PRIVATE).getString("lastRefresh",""));
            payload.put("deepStock",deepStock);
            payload.put("deepReport",deepReport);
            payload.put("refreshing",refreshing.get());
            payload.put("message",lastMessage);
            payload.put("error",lastError);
        }catch(Exception ignored){}
        send(payload.toString());
    }
    private void sendStatus(boolean busy,String text,boolean error){
        lastMessage=text;lastError=error?text:"";
        JSONObject payload=new JSONObject();
        try{payload.put("kind","status");payload.put("refreshing",busy);payload.put("message",text);payload.put("error",error?text:"");}
        catch(Exception ignored){}
        send(payload.toString());
    }
    private void start(boolean full,String symbol) {
        if(!refreshing.compareAndSet(false,true)){sendStatus(true,"A refresh is already running",false);return;}
        sendStatus(true,full?"Searching Indian market research across public feeds…":"Investigating "+symbol+" across public sources…",false);
        jobs.execute(()->{
            try{
                DiscoveryEngine.RefreshReport r=full?discovery.refresh():discovery.investigate(symbol);
                String result="Checked "+r.requests+" feed searches · "+r.articles+" headlines · "+r.inserted+" new stock mentions";
                if(r.failures>0)result+=" · "+r.failures+" sources unavailable";
                if(r.failures==r.requests)sendStatus(false,"All sources unavailable. Check internet/network permissions. "+(r.errors.isEmpty()?"":r.errors.get(0)),true);
                else {
                    getPreferences(MODE_PRIVATE).edit().putString("lastRefresh",java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).toString()).apply();
                    sendStatus(false,result,false);
                }
            }catch(Exception e){sendStatus(false,"Refresh failed: "+e.getMessage(),true);}
            finally{refreshing.set(false);sendSnapshot();}
        });
    }
    private void deepSearch(String input) {
        String symbol=discovery.resolveStock(input);
        if(symbol.isEmpty()){
            String candidate=input==null?"":input.trim().toUpperCase(java.util.Locale.ROOT);
            if(candidate.matches("[A-Z0-9.&-]{2,30}"))symbol=candidate;
        }
        if(symbol.isEmpty()){sendStatus(false,"Enter a recognized company name or valid NSE ticker",true);return;}
        if(!refreshing.compareAndSet(false,true)){sendStatus(true,"Search already running",false);return;}
        final String s=symbol;
        deepStock=s;deepReport="";
        sendStatus(true,"Deep Search "+s+": scanning brokerages, analysts, publications and public indexes…",false);
        jobs.execute(()->{
            try{
                String company=discovery.symbolName(s);
                if(company.isEmpty())company=s;
                DiscoveryEngine.DeepReport r=discovery.deepSearch(s,company);
                deepReport="Deep Search "+s+" · "+r.successes+"/"+r.queries+
                    " portal queries succeeded · "+r.found+" stock-matched hits · "+r.saved+
                    " new saved sources · "+r.failures+" unavailable";
                sendStatus(false,deepReport,r.successes==0);
            }catch(Exception e){
                deepReport="Deep Search failed: "+e.getMessage();
                sendStatus(false,deepReport,true);
            }finally{refreshing.set(false);sendSnapshot();}
        });
    }
    private final class Bridge {
        @JavascriptInterface public void load(){sendSnapshot();}
        @JavascriptInterface public void setWindow(int days){
            if(days==1||days==2||days==3||days==7||days==14||days==30){windowDays=days;sendSnapshot();}
        }
        @JavascriptInterface public void refresh(){start(true,"");}
        @JavascriptInterface public void deepSearch(String ticker){MainActivity.this.deepSearch(ticker);}
        @JavascriptInterface public void investigate(String symbol){
            String s=symbol==null?"":symbol.trim().toUpperCase(java.util.Locale.ROOT);
            if(s.matches("[A-Z0-9.&-]{2,30}")&&!discovery.symbolName(s).isEmpty())start(false,s);
        }
        @JavascriptInterface public void open(String url){if(url!=null)openUrl(url);}
    }
    @Override public void onBackPressed(){
        if(web!=null)web.evaluateJavascript("window.appBack && window.appBack();",null);
        else super.onBackPressed();
    }
    @Override protected void onDestroy(){
        pageReady=false;
        jobs.shutdownNow();
        if(web!=null){web.removeJavascriptInterface("StockNative");web.destroy();web=null;}
        discovery.close();
        super.onDestroy();
    }
}
