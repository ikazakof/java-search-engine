package main.services;

import main.data.model.Site;
import main.data.repository.SiteRepository;

public class IndexingPageChecker {

    public static boolean indexingPageInRange(SiteRepository siteRepository, String url){
        for(Site site : siteRepository.findAll()) {
            if (url.matches(site.getUrl() + ".*")) {
                return true;
            }
        }
        return false;
    }
}
