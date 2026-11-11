package com.localstocks.intelligence;

import org.junit.Test;
import static org.junit.Assert.*;

public class PublicArticleReaderTest {
    private static final String ROBOTS=
        "User-Agent: *\nAllow: /\nDisallow: /api/\nDisallow: /private/\n"+
        "User-agent: Googlebot-News\nAllow: /\n";
    @Test public void allowsReviewedPublicArticlePaths(){
        assertTrue(PublicArticleReader.allowedByRobots(ROBOTS,"/markets/stocks/story/test"));
    }
    @Test public void rejectsDisallowedAndUnknownRobots(){
        assertFalse(PublicArticleReader.allowedByRobots(ROBOTS,"/api/internal"));
        assertFalse(PublicArticleReader.allowedByRobots("", "/markets/stocks"));
        assertFalse(PublicArticleReader.allowedByRobots(null, "/markets/stocks"));
    }
    @Test public void onlyReviewedHostsAreEligible(){
        assertTrue(PublicArticleReader.supportsUrl("https://www.businesstoday.in/markets/stocks/story/article"));
        assertTrue(PublicArticleReader.supportsUrl("https://www.business-standard.com/markets/news/article"));
        assertFalse(PublicArticleReader.supportsUrl("https://www.not-businesstoday.in/article"));
        assertFalse(PublicArticleReader.supportsUrl("http://www.businesstoday.in/article"));
        assertFalse(PublicArticleReader.supportsUrl("https://www.moneycontrol.com/news/article"));
    }
}
