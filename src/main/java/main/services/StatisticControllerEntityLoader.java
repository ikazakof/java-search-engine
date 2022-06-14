package main.services;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class StatisticControllerEntityLoader {

    public static ResponseEntity<JSONObject> getEmptyStatisticsEntity(){
        JSONParser parser = new JSONParser();
        ResponseEntity<JSONObject> resultJson = null;

        StringBuilder result = new StringBuilder();
        result.append("{\n\"result\": true,\n\"statistics\": {\n\"total\": {\n\"sites\": ").append(0).append(",\n\"pages\": ").append(0).append(",\n\"lemmas\": ").append(0);
        result.append(",\n\"isIndexing\": ").append(false).append("\n},\n\"detailed\": [\n");
        result.append("{\n\"url\": \"").append(0).append("\",\n");
        result.append("\"name\": \"").append(0).append("\",\n");
        result.append("\"status\": \"").append(0).append("\",\n");
        result.append("\"statusTime\": ").append(0);
        result.append("\"error\": \"").append(0).append("\",\n");
        result.append("\"pages\": ").append(0).append(",\n");
        result.append("\"lemmas\": ").append(0).append("\n}");
        result.append("\n]\n}}");
        try {
            resultJson = new ResponseEntity<> ((JSONObject) parser.parse(result.toString()), HttpStatus.OK);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return resultJson;
    }

    public static ResponseEntity<JSONObject> getStatisticsEntity(SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository) {
        JSONParser parser = new JSONParser();
        ResponseEntity<JSONObject> resultJson = null;

        boolean isIndexing = SiteStatusChecker.indexingSitesExist(siteRepository);

        StringBuilder result = new StringBuilder();
        result.append("{\n\"result\": true,\n\"statistics\": {\n\"total\": {\n\"sites\": ").append(siteRepository.count()).append(",\n\"pages\": ").append(pageRepository.count()).append(",\n\"lemmas\": ").append(lemmaRepository.count());
        result.append(",\n\"isIndexing\": ").append(isIndexing).append("\n},\n\"detailed\": [\n");

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
            resultJson = new ResponseEntity<> ((JSONObject) parser.parse(result.toString()), HttpStatus.OK);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return resultJson;
    }
}
