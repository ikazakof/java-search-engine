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
        setCachedResource();
    }

    private void setCachedResource(){
        try {
           cachedResource = Jsoup.connect(siteUrl).userAgent(userAgent).referrer("http://www.google.com").ignoreHttpErrors(true).maxBodySize(0).execute();
           Thread.sleep(650);
        } catch (Exception exception ) {
            exception.printStackTrace();
        }
    }

    public Connection.Response getCachedResource() {
        return cachedResource;
    }

    public int getStatusCode(){
        int stat = 0;
        try {
            if (this.cachedResource != null){
                stat = this.cachedResource.statusCode();
            }
        } catch (Exception ex){
            ex.printStackTrace();
        }
        return stat;
    }

    public Document getSiteDocument(){
        if(this.cachedResource == null){
            return new Document("");
        }
        Document result = null;
        try {
            result = this.cachedResource.parse();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return result;
    }

}
