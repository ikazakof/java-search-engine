package main.services;

import main.data.model.Site;
import main.data.model.Status;
import main.data.repository.SiteRepository;

public class SiteStatusChecker {

    public static boolean indexingSitesExist(SiteRepository siteRepository){
        for (Site site : siteRepository.findAll()) {
            if (site.getStatus().equals(Status.INDEXING)) {
                return true;
            }
        }
        return false;
    }


}
