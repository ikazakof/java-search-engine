package main.controllers;

import main.data.repository.LemmaRepository;
import main.data.repository.PageRepository;
import main.data.repository.SiteRepository;
import main.services.StatisticControllerEntityLoader;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class StatisticController {

    @Autowired
    SiteRepository siteRepository;
    @Autowired
    PageRepository pageRepository;
    @Autowired
    LemmaRepository lemmaRepository;


    @GetMapping("/statistics")
    public ResponseEntity<JSONObject> statistics() {
        if(siteRepository.count() == 0){
           return StatisticControllerEntityLoader.getEmptyStatisticsEntity();
        }
        return StatisticControllerEntityLoader.getStatisticsEntity(siteRepository, pageRepository, lemmaRepository);
    }

}
