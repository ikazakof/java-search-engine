package main.services;

import lombok.NoArgsConstructor;
import main.data.model.Site;
import main.data.repository.SiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class IndexingPageChecker {

    SiteRepository siteRepository;

    @Autowired
    public IndexingPageChecker(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    public boolean indexingPageInRange(String url){
        for(Site site : siteRepository.findAll()) {
            if (url.matches(site.getUrl() + ".*")) {
                return true;
            }
        }
        return false;
    }
}
