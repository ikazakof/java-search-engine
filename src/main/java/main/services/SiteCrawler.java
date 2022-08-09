package main.services;


import main.data.model.Page;
import main.data.model.Site;
import main.data.repository.SiteRepository;
import main.data.model.Status;
import org.jsoup.nodes.Element;

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

        if(siteUrlsAndPages.isEmpty()){
            return result;
        }
        for (Map.Entry <String, Page> urlAndPage :  siteUrlsAndPages.entrySet()){
            if(!urlAndPage.getKey().equals(siteUrl) && !result.containsKey(urlAndPage.getKey()) && getUrlsAndPages(urlAndPage.getKey()).size() > 0) {
                result.put(urlAndPage.getKey(), new Page(urlAndPage.getValue().getPath(), urlAndPage.getValue().getAnswerCode(), urlAndPage.getValue().getPageContent(), parentSiteId));
                SiteCrawler task = new SiteCrawler(urlAndPage.getKey(), userAgent, siteRepository);
                task.fork();
                tasks.add(task);
            } else {
                result.put(urlAndPage.getKey(), new Page(urlAndPage.getValue().getPath(), urlAndPage.getValue().getAnswerCode(), urlAndPage.getValue().getPageContent(), parentSiteId));
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
        SiteConnector parentSiteConnector = new SiteConnector(userAgent, url);
        if(parentSiteConnector.getCachedResource() == null){
            return new TreeMap<>();
        }
        for(Site siteFromDB : siteRepository.findAll()){
            if(siteFromDB.getUrl().equals(url)){
            urlsAndPages.put(url, new Page("/", parentSiteConnector.getStatusCode(), parentSiteConnector.getSiteDocument().toString(), parentSiteId));
            break;
            }
        }
        if(siteRepository.findById(parentSiteId).get().getStatus().equals(Status.FAILED) || parentSiteConnector.getSiteDocument().body().select("a[href]").size() == 0){
            return new TreeMap<>();
        }
        for(Element href : parentSiteConnector.getSiteDocument().body().select("a[href]")) {
            if (!checkHref(href) || urlsAndPages.containsKey(href.attr("abs:href"))) {
                continue;
            }

            SiteConnector childSiteConnector = new SiteConnector(userAgent, href.attr("abs:href"));
            if (childSiteConnector.getCachedResource() == null || siteRepository.findById(parentSiteId).get().getStatus().equals(Status.FAILED) || childSiteConnector.getSiteDocument().body().select("a[href]").size() == 0) {
                continue;
            }
            if (href.attr("abs:href").compareTo(href.attr("href")) == 0) {
                urlsAndPages.put(href.attr("abs:href"), new Page(href.attr("href").replaceAll(siteRepository.findById(parentSiteId).get().getUrl(), ""), childSiteConnector.getStatusCode(),childSiteConnector.getSiteDocument().toString(), parentSiteId));
            } else {
                urlsAndPages.put(href.attr("abs:href"), new Page(href.attr("href"), childSiteConnector.getStatusCode(), childSiteConnector.getSiteDocument().toString(), parentSiteId));
            }
        }
            return urlsAndPages;
    }

    private boolean checkHref(Element href){
        return href.attr("abs:href").matches(parentSiteUrl + ".{2,}") && href.attr("abs:href").matches(siteUrl + ".{2,}") && !href.attr("abs:href").equals(siteUrl)   && !href.attr("abs:href").contains("#") && !href.attr("abs:href").contains("?method") && !href.attr("abs:href").contains("go?") && !href.attr("abs:href").contains("vkontakte") && !href.attr("abs:href").toLowerCase().matches(".*(.jpg|.png|.jpeg|.pdf|.pptx|.docx|.txt|.svg|.xlsx|.xls|.xml|.avi|.mpeg|.doc|.ppt|.rtf|.gif).*");
    }


}
