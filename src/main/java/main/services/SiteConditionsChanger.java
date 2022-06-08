package main.services;

import lombok.AllArgsConstructor;
import main.data.model.Site;
import main.data.model.Status;
import main.data.repository.SiteRepository;

import java.time.LocalDateTime;


public class SiteConditionsChanger {


    public static void changeSiteConditionsEmptyPages(Site site, SiteRepository siteRepository){
            site.setStatus(Status.FAILED);
            site.setLastError("Страницы отсутсвуют");
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
    }

    public static void changeSiteConditionsEmptyLemmas(Site site, SiteRepository siteRepository){
            site.setStatus(Status.FAILED);
            site.setLastError("Леммы отсутсвуют");
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
    }

    public static void changeSiteConditionsEmptyIndex(Site site, SiteRepository siteRepository){
            site.setStatus(Status.FAILED);
            site.setLastError("Индексы отсутсвуют");
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
    }

    public static void changeSiteConditionsSuccessIndexed(Site site, SiteRepository siteRepository){
        site.setStatus(Status.INDEXED);
        site.setStatusTime(LocalDateTime.now());
        site.setLastError(null);
        siteRepository.save(site);
    }

    public static void changeSitesConditionStopIndex(SiteRepository siteRepository){
        for (Site site : siteRepository.findAll()){
            if(site.getStatus().equals(Status.INDEXING)){
                site.setStatus(Status.FAILED);
                site.setLastError("Индексация принудительно остановлена");
                site.setStatusTime(LocalDateTime.now());
                siteRepository.save(site);
            }
        }
    }

    public static void cloneSiteConditionPageIndexing(Site site, String url, SiteRepository siteRepository){

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

    public static void changeSiteConditionsEmptyLemmasOnPage(Site site, SiteRepository siteRepository){
        site.setStatus(Status.FAILED);
        site.setLastError("На индексируемой странице леммы отсутсвуют");
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }

    public static void changeSiteConditionsEmptyIndexOnPage(Site site, SiteRepository siteRepository){
        site.setStatus(Status.FAILED);
        site.setLastError("На индексируемой странице индексы отсутсвуют");
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }

}
