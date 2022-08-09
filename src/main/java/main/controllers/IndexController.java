package main.controllers;

import main.data.model.*;
import main.data.repository.*;
import main.services.*;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class IndexController {
    final
    SiteRepository siteRepository;
    final
    ResponseEntityLoader responseEntityLoader;
    final
    DBCleaner dbCleaner;
    final
    DBParamLoader dbParamLoader;
    final
    SiteStatusChecker siteStatusChecker;
    final
    IndexingServices indexingServices;
    final
    SiteConditionsChanger siteConditionsChanger;
    final
    IndexingPageChecker indexingPageChecker;
    @Autowired
    public IndexController(SiteRepository siteRepository, ResponseEntityLoader responseEntityLoader, DBCleaner dbCleaner, DBParamLoader dbParamLoader, SiteStatusChecker siteStatusChecker, IndexingServices indexingServices, SiteConditionsChanger siteConditionsChanger, IndexingPageChecker indexingPageChecker) {
        this.siteRepository = siteRepository;
        this.responseEntityLoader = responseEntityLoader;
        this.dbCleaner = dbCleaner;
        this.dbParamLoader = dbParamLoader;
        this.siteStatusChecker = siteStatusChecker;
        this.indexingServices = indexingServices;
        this.siteConditionsChanger = siteConditionsChanger;
        this.indexingPageChecker = indexingPageChecker;
    }


    @GetMapping("/startIndexing")
    public ResponseEntity<JSONObject> startIndexing() {
        if (siteStatusChecker.indexingSitesExist()) {
            return responseEntityLoader.getIndexingAlreadyStartResponse();
        }
        dbCleaner.cleanDB();
        dbParamLoader.loadStartParam();
        for (Site siteFromDB : siteRepository.findAll()) {
            siteConditionsChanger.changeSiteConditionsStartIndexing(siteFromDB);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(()-> indexingServices.indexTargetSite(siteFromDB));
            executor.shutdown();
        }
        return responseEntityLoader.getControllerMethodStartResponse();
}

    @GetMapping("/stopIndexing")
    public ResponseEntity<JSONObject> stopIndexing() {
        if(!siteStatusChecker.indexingSitesExist()){
          return responseEntityLoader.getIndexingNotStartResponse();
        }
        siteConditionsChanger.changeSitesConditionStopIndex();
        return responseEntityLoader.getControllerMethodStartResponse();
    }

    @PostMapping("/indexPage")
    public ResponseEntity<JSONObject> indexPage(@RequestParam String url){
        if(!indexingPageChecker.indexingPageInRange(url)){
            return responseEntityLoader.getPageOutOfRangeResponse();
        }
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(()-> indexingServices.indexTargetPage(url));
        executor.shutdown();
       return responseEntityLoader.getControllerMethodStartResponse();
    }
}


