package main;

import main.model.Page;

import main.model.Site;
import main.model.SiteRepository;
import main.model.Status;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;


import java.io.IOException;
import java.util.*;

import java.util.concurrent.RecursiveTask;

public class SiteCrawler extends RecursiveTask<TreeMap<String, Page>> {

    private String siteUrl;
    private int parentSiteId;
    private String parentSiteUrl;
    private String userAgent;
    private SiteRepository siteRepository;

    public SiteCrawler(String siteUrl, String userAgent, SiteRepository siteRepository){
        this.siteUrl = siteUrl;
        this.userAgent = userAgent;
        this.siteRepository = siteRepository;
        for(Site siteFromDB : siteRepository.findAll()) {
            if (siteUrl.contains(siteFromDB.getUrl())) {
                this.parentSiteId = siteFromDB.getId();
                this.parentSiteUrl = siteFromDB.getUrl();
                break;
            }
        }
    }


    protected TreeMap<String, Page> compute(){
        TreeMap<String, Page> result = new TreeMap<>();
        List<SiteCrawler> tasks = new ArrayList<>();
        TreeMap<String, Page> siteUrlsAndPages = getUrlsAndPages(siteUrl);

        if(!siteUrlsAndPages.isEmpty()){
            for (Map.Entry <String, Page> urlAndPage :  siteUrlsAndPages.entrySet()){
                if(!urlAndPage.getKey().equals(siteUrl) && !result.containsKey(urlAndPage.getKey()) && getUrlsAndPages(urlAndPage.getKey()).size() > 0) {
                    result.put(urlAndPage.getKey(), new Page(urlAndPage.getValue().getPath(), urlAndPage.getValue().getAnswerCode(), urlAndPage.getValue().getPageContent(), parentSiteId));
                    SiteCrawler task = new SiteCrawler(urlAndPage.getKey(), userAgent, siteRepository);
                    task.fork();
                    tasks.add(task);
                } else {
                    result.put(urlAndPage.getKey(), new Page(urlAndPage.getValue().getPath(), urlAndPage.getValue().getAnswerCode(), urlAndPage.getValue().getPageContent(), parentSiteId));
                }
                try {
                    Thread.sleep(800);
                } catch (InterruptedException exception){
                    exception.printStackTrace();
                }
            }
        }
        addResultFromTasks(result, tasks);
        return result;
    }

    private void addResultFromTasks(TreeMap<String, Page> result, List<SiteCrawler> tasks){
        tasks.forEach(task -> result.putAll(task.join()));
    }

    private TreeMap<String, Page> getUrlsAndPages(String url){
        TreeMap<String, Page> urlsAndPages = new TreeMap<>();
        Connection.Response cachedResource = null;
        Document site = null;

        try {
            cachedResource = Jsoup.connect(url).userAgent(userAgent).referrer("http://www.google.com").ignoreHttpErrors(true).maxBodySize(0).execute();
            site = cachedResource.parse();
            for(Site siteFromDB : siteRepository.findAll()){
                if(siteFromDB.getUrl().equals(url)){
                    urlsAndPages.put(url, new Page("/", cachedResource.statusCode(), site.toString(), parentSiteId));
                    break;
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
            System.out.println(url.toString() + " parent URL");
        }

        if( site != null && !siteRepository.findById(parentSiteId).get().getStatus().equals(Status.FAILED) && site.body().select("a[href]").size() > 0){
            for(Element href : site.body().select("a[href]")){
                if(checkHref(href) && !urlsAndPages.containsKey(href.attr("abs:href") )){
                    int responseCode = 0;
                    Connection.Response childCachedResource = null;
                    try {
                        childCachedResource = Jsoup.connect(href.attr("abs:href")).userAgent(userAgent).referrer("http://www.google.com").ignoreHttpErrors(true).maxBodySize(0).execute();
                        site = childCachedResource.parse();

                        responseCode =  childCachedResource.statusCode();
                        Thread.sleep(   800);
                    } catch (IOException | InterruptedException exception) {
                        exception.printStackTrace();
                        System.out.println(href.toString() + " child URL");
                    }

                    if(href.attr("abs:href").compareTo(href.attr("href")) == 0){
                        urlsAndPages.put(href.attr("abs:href"), new Page(href.attr("href").replaceAll(siteRepository.findById(parentSiteId).get().getUrl(), ""), responseCode, site.toString(), parentSiteId));
                    } else {
                        urlsAndPages.put(href.attr("abs:href"), new Page(href.attr("href"), responseCode, site.toString(), parentSiteId));
                    }
                }
            }
            return urlsAndPages;
        }
        return new TreeMap<>();
    }

    private boolean checkHref(Element href){
        return href.attr("abs:href").matches(parentSiteUrl + ".{2,}") && href.attr("abs:href").matches(siteUrl + ".{2,}") && !href.attr("abs:href").equals(siteUrl)   && !href.attr("abs:href").contains("#") && !href.attr("abs:href").contains("?method") && !href.attr("abs:href").contains("vkontakte") && !href.attr("abs:href").toLowerCase().matches(".*(.jpg|.png|.jpeg|.pdf|.pptx|.docx|.txt|.svg|.xlsx|.xls|.avi|.mpeg|.doc|.ppt|.rtf).*");
    }



    public String getSiteUrl() {
        return siteUrl;
    }

    public void setSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }

}
