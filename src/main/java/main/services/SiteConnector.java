package main.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;


public class SiteConnector {

    private Connection.Response cachedResource;
    private String userAgent;
    private String siteUrl;

    public SiteConnector(String userAgent, String siteUrl) {
        this.userAgent = userAgent;
        this.siteUrl = siteUrl;
        getCachedResource();
    }

    private void getCachedResource(){
        try {
           cachedResource = Jsoup.connect(siteUrl).userAgent(userAgent).referrer("http://www.google.com").ignoreHttpErrors(true).maxBodySize(0).execute();
           Thread.sleep(   650);
        } catch (IOException | InterruptedException exception) {
            exception.printStackTrace();
        }
    }

    public int getStatusCode(){
        return this.cachedResource.statusCode();
    }

    public Document getSiteDocument(){
        if(this.cachedResource == null){
            return null;
        }
        Document result = null;
        try {
            result = this.cachedResource.parse();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return result;
    }

}
