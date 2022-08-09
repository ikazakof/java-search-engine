package main.services;

import lombok.NoArgsConstructor;
import main.data.model.Site;
import main.data.model.Status;
import main.data.repository.SiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@NoArgsConstructor
public class SiteConditionsChanger {

    SiteRepository siteRepository;

    @Autowired
    public SiteConditionsChanger(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    public void changeSiteConditionsStartIndexing(Site site){
        site.setStatus(Status.INDEXING);
        site.setLastError(null);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }
    
    public void changeSiteConditionsEmptyPages(Site site){
            site.setStatus(Status.FAILED);
            site.setLastError("Страницы отсутсвуют");
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
    }

    public void changeSiteConditionsEmptyLemmas(Site site){
            site.setStatus(Status.FAILED);
            site.setLastError("Леммы отсутсвуют");
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
    }

    public void changeSiteConditionsEmptyIndex(Site site){
            site.setStatus(Status.FAILED);
            site.setLastError("Индексы отсутсвуют");
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
    }

    public void changeSiteConditionsSuccessIndexed(Site site){
        site.setStatus(Status.INDEXED);
        site.setStatusTime(LocalDateTime.now());
        site.setLastError(null);
        siteRepository.save(site);
    }

    public void changeSitesConditionStopIndex(){
        for (Site site : siteRepository.findAll()){
            if(site.getStatus().equals(Status.INDEXING)){
                site.setStatus(Status.FAILED);
                site.setLastError("Индексация принудительно остановлена");
                site.setStatusTime(LocalDateTime.now());
                siteRepository.save(site);
            }
        }
    }

    public void cloneSiteConditionPageIndexing(Site site, String url){

        for(Site siteFromDB : siteRepository.findAll()) {
            if (url.matches(siteFromDB.getUrl() + ".*")) {
                site.setId(siteFromDB.getId());
                site.setUrl(siteFromDB.getUrl());
                site.setName(siteFromDB.getName());
                site.setStatus(Status.INDEXING);
                site.setLastError(null);
                site.setStatusTime(LocalDateTime.now());
                siteRepository.save(site);
                break;
            }
        }
    }

    public void changeSiteConditionsEmptyLemmasOnPage(Site site){
        site.setStatus(Status.FAILED);
        site.setLastError("На индексируемой странице леммы отсутсвуют");
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }

    public void changeSiteConditionsEmptyIndexOnPage(Site site){
        site.setStatus(Status.FAILED);
        site.setLastError("На индексируемой странице индексы отсутсвуют");
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }

    public void cloneSiteFromDB(Site site, String url){

        for(Site siteFromDB : siteRepository.findAll()) {
            if (url.matches(siteFromDB.getUrl())) {
                site.setId(siteFromDB.getId());
                site.setUrl(siteFromDB.getUrl());
                site.setName(siteFromDB.getName());
                site.setStatus(siteFromDB.getStatus());
                site.setLastError(siteFromDB.getLastError());
                site.setStatusTime(siteFromDB.getStatusTime());
                break;
            }
        }
    }

}
