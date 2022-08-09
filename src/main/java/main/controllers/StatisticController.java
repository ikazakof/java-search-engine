package main.controllers;

import main.data.repository.SiteRepository;
import main.services.StatisticControllerEntityLoader;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class StatisticController {

    final
    SiteRepository siteRepository;
    final
    StatisticControllerEntityLoader statisticControllerEntityLoader;
    @Autowired
    public StatisticController(SiteRepository siteRepository, StatisticControllerEntityLoader statisticControllerEntityLoader) {
        this.siteRepository = siteRepository;
        this.statisticControllerEntityLoader = statisticControllerEntityLoader;
    }

    @GetMapping("/statistics")
    public ResponseEntity<JSONObject> statistics() {
        if(siteRepository.count() == 0){
           return statisticControllerEntityLoader.getEmptyStatisticsEntity();
        }
        return statisticControllerEntityLoader.getStatisticsEntity();
    }

}
