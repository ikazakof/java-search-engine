package main.controllers;

import main.data.model.Lemma;
import main.data.model.Page;
import main.data.model.Site;
import main.data.model.Status;
import main.data.repository.LemmaRepository;
import main.data.repository.PageRepository;
import main.data.repository.SiteRepository;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;

@RestController
public class StatisticController {

    @Autowired
    SiteRepository siteRepository;
    @Autowired
    PageRepository pageRepository;
    @Autowired
    LemmaRepository lemmaRepository;


    @GetMapping("/statistics")
    public ResponseEntity statistics() {
        JSONParser parser = new JSONParser();
        JSONObject resultJson = new JSONObject();

        boolean isIndexing = false;
        for (Site site : siteRepository.findAll()) {
            if (site.getStatus().equals(Status.INDEXING)) {
                isIndexing = true;
                break;
            }
        }
        StringBuilder result = new StringBuilder();
        result.append("{\n\"result\": true,\n\"statistics\": {\n\"total\": {\n\"sites\": ").append(siteRepository.count()).append(",\n\"pages\": ").append(pageRepository.count()).append(",\n\"lemmas\": ").append(lemmaRepository.count());
        result.append(",\n\"isIndexing\": ").append(isIndexing).append("\n},\n\"detailed\": [\n");

        ArrayList<Site> sitesFromDB = new ArrayList<>();
        sitesFromDB.addAll((Collection<? extends Site>) siteRepository.findAll());
        if(sitesFromDB.isEmpty()){
            result.append("{\n\"url\": \"").append(0).append("\",\n");
            result.append("\"name\": \"").append(0).append("\",\n");
            result.append("\"status\": \"").append(0).append("\",\n");
            result.append("\"statusTime\": ").append(0);
            result.append("\"error\": \"").append(0).append("\",\n");
            result.append("\"pages\": ").append(0).append(",\n");
            result.append("\"lemmas\": ").append(0).append("\n}");
            result.append("\n]\n}}");
            try {
                resultJson = (JSONObject) parser.parse(result.toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return new ResponseEntity(resultJson, HttpStatus.OK);
        }
        int siteCounter = 0;
        for (Site site : siteRepository.findAll()) {
            siteCounter++;
            result.append("{\n\"url\": \"").append(site.getUrl()).append("\",\n");
            result.append("\"name\": \"").append(site.getName()).append("\",\n");
            result.append("\"status\": \"").append(site.getStatus()).append("\",\n");
            result.append("\"statusTime\": ").append(ZonedDateTime.of(site.getStatusTime(), ZoneId.systemDefault()).toInstant().toEpochMilli()).append(",\n");
            result.append("\"error\": \"").append(site.getLastError()).append("\",\n");
            int pagesCounter = 0;
            for (Page page : pageRepository.findAll()){
                if (page.getSiteId() == site.getId()){
                    pagesCounter++;
                }
            }
            result.append("\"pages\": ").append(pagesCounter).append(",\n");
            int lemmasCounter = 0;
            for (Lemma lemma : lemmaRepository.findAll()){
                if (lemma.getSiteId() == site.getId()){
                    lemmasCounter++;
                }
            }
            result.append("\"lemmas\": ").append(lemmasCounter).append("\n}");
            if (siteCounter != siteRepository.count()){
                result.append(",\n");
            } else {
                result.append("\n]\n}}");
            }
        }


        try {
            resultJson = (JSONObject) parser.parse(result.toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return new ResponseEntity(resultJson, HttpStatus.OK);
    }

}
