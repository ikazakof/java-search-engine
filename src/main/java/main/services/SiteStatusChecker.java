package main.services;

import lombok.NoArgsConstructor;
import main.data.model.Site;
import main.data.model.Status;
import main.data.repository.SiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class SiteStatusChecker {

    SiteRepository siteRepository;

    @Autowired
    public SiteStatusChecker(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    public boolean indexingSitesExist(){
        for (Site site : siteRepository.findAll()) {
            if (site.getStatus().equals(Status.INDEXING)) {
                return true;
            }
        }
        return false;
    }

    public boolean indexedSitesExist(){
        for (Site site : siteRepository.findAll()) {
            if (site.getStatus().equals(Status.INDEXED)) {
                return true;
            }
        }
        return false;
    }

}
