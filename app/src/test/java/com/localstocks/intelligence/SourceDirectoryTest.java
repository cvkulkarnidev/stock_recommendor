package com.localstocks.intelligence;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.HashSet;
import java.util.Set;

public class SourceDirectoryTest {
    @Test public void tracksMoreThanHundredUniqueCandidateDomains(){
        assertTrue(SourceDirectory.all().size()>=120);
        assertTrue(SourceDirectory.searchable().size()>=100);
        Set<String> domains=new HashSet<>();
        for(SourceDirectory.Site site:SourceDirectory.all()){
            assertTrue(site.host.matches("[a-z0-9.-]+"));
            assertTrue("Duplicate domain: "+site.host,domains.add(site.host));
        }
    }
    @Test public void directoryIsNotAWhitelistOfVerifiedAnalysts(){
        assertTrue(SourceDirectory.containsDomain("https://www.icicidirect.com/research"));
        assertFalse(SourceDirectory.containsDomain("https://www.evil-icicidirect.com/research"));
        assertFalse(SourceDirectory.containsDomain("https://totally-unknown.example"));
    }
}
