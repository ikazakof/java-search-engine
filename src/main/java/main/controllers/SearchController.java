package main.controllers;

import main.data.dto.FoundPage;
import main.data.model.*;
import main.data.repository.*;
import main.services.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;

@RestController
public class SearchController {

    @Autowired
    SiteRepository siteRepository;
    @Autowired
    PageRepository pageRepository;
    @Autowired
    LemmaRepository lemmaRepository;
    @Autowired
    IndexRepository indexRepository;


    @GetMapping("/search")
    public ResponseEntity search(@RequestParam String query, @RequestParam(required = false)  String site, @RequestParam int offset, @RequestParam int limit) {
        JSONParser parser = new JSONParser();
        JSONObject resultJson = new JSONObject();

        if (query == null || query.isEmpty()){
            try {
                return new ResponseEntity (parser.parse("{\n\"result\": false,\n\"error\": \"Отсутствует поисковый запрос\"\n}"), HttpStatus.OK);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        ArrayList<Index> indexesFromDB = new ArrayList<>();
        HashMap<Integer, Page> targetSitePages = new HashMap<>();
        HashMap<Integer, Lemma> targetLemmas = new HashMap<>();

        if(site != null && !site.isEmpty()){
            Site targetSite = new Site();
            SiteConditionsChanger.cloneSiteFromDB(targetSite, site, siteRepository);

            if(targetSite.getId() == 0){
                try {
                    return new ResponseEntity (parser.parse("{\n\"result\": false,\n\"error\": \"Запрашиваемый сайт отсутсвует в базе данных\"\n}"), HttpStatus.OK);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            targetSitePages.putAll(PageLoader.loadSitePagesFromDB(targetSite.getId(), pageRepository));
            if(targetSitePages.size() == 0){
                try {
                    return new ResponseEntity (parser.parse( siteRepository.findById(targetSite.getId()).get().getStatus().compareTo(Status.INDEXING) == 0 ? "{\n\"result\": false,\n\"error\": \"Запрашиваемый сайт индексируется, попробуйте позже\"\n}" : "{\n\"result\": false,\n\"error\": \"Запрашиваемый сайт не имеет страниц в базе данных\"\n}"), HttpStatus.OK);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            targetLemmas.putAll(LemmasLoader.loadSiteLemmasFromDBWithFreq(targetSite.getId(), lemmaRepository, pageRepository.count()));

            if(targetLemmas.size() == 0){
                try {
                    return new ResponseEntity (parser.parse(siteRepository.findById(targetSite.getId()).get().getStatus().compareTo(Status.INDEXING) == 0 ? "{\n\"result\": false,\n\"error\": \"Запрашиваемый сайт индексируется, попробуйте позже\"\n}" : "{\n\"result\": false,\n\"error\": \"Запрашиваемый сайт не имеет лемм в базе данных\"\n}"), HttpStatus.OK);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            indexesFromDB.addAll(IndexLoader.loadIndexFromDBByPageIdAndLemmas(targetSitePages.keySet(), indexRepository, targetLemmas.keySet()));
            if(indexesFromDB.size() == 0){
                try {
                    return new ResponseEntity (parser.parse(siteRepository.findById(targetSite.getId()).get().getStatus().compareTo(Status.INDEXING) == 0 ? "{\n\"result\": false,\n\"error\": \"Запрашиваемый сайт индексируется, попробуйте позже\"\n}" : "{\n\"result\": false,\n\"error\": \"Запрашиваемый сайт не имеет индексов в базе данных\"\n}"), HttpStatus.OK);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }

        if(indexesFromDB.isEmpty() && !SiteStatusChecker.indexedSitesExist(siteRepository)){
            try {
                return new ResponseEntity (parser.parse("{\n\"result\": false,\n\"error\": \"Отсутсвуют проиндексированные сайты\"\n}"), HttpStatus.OK);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        if(targetLemmas.isEmpty()){
            targetLemmas.putAll(LemmasLoader.loadLemmasFromDBWithFreqAndIndexedSites(siteRepository, lemmaRepository, pageRepository.count()));
        }

        if(indexesFromDB.isEmpty()){
            indexesFromDB.addAll(IndexLoader.loadIndexFromDBByLemmas(indexRepository, targetLemmas.keySet()));
        }

        Search search = new Search(query, targetLemmas.values(), indexesFromDB);
        if (search.getFoundPages().isEmpty()){
            try {
                return new ResponseEntity (parser.parse("{\n\"result\": false,\n\"error\": \"Отсутсвуют совпадения\"\n}"), HttpStatus.OK);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        ArrayList<Page> relevantPages = new ArrayList<>();

        if(!targetSitePages.isEmpty()){
            relevantPages.addAll(PageLoader.loadPagesByIdFromTargetPages(targetSitePages, search.getFoundPages().keySet()));
        } else {
            relevantPages.addAll(PageLoader.loadPagesByIDFromPagesRepository(pageRepository, search.getFoundPages().keySet()));
        }

        ArrayList<Lemma> relevantLemmas = new ArrayList<>();
        relevantLemmas.addAll(search.getSearchLemmas());

        RelevantPageLoader relevantPageLoader = new RelevantPageLoader(relevantPages, relevantLemmas, search.getFoundPages());

        if (relevantPageLoader.getRelevantPages().isEmpty()){
            try {
                return new ResponseEntity (parser.parse("{\n\"result\": false,\n\"error\": \"Отсутсвует вывод найденных совпадений\"\n}"), HttpStatus.OK);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        ArrayList<FoundPage> foundPages = new ArrayList<>(relevantPageLoader.getRelevantPages());

        StringBuilder result = new StringBuilder();
        result.append("{\n\"result\": true,\n \"count\": ").append(relevantPageLoader.getRelevantPages().size()).append(",\n \"data\": [\n");

        int pageLimit = (limit == 0 ? 20 + offset : limit + offset);
        if(pageLimit > relevantPageLoader.getRelevantPages().size()){
            pageLimit = relevantPageLoader.getRelevantPages().size();
        }
        if(offset > relevantPageLoader.getRelevantPages().size()){
            offset = 0;
        }

        for (; offset < pageLimit; offset++){

            result.append("{\n \"site\": \"").append(siteRepository.findById(foundPages.get(offset).getSiteId()).get().getUrl()).append("\",\n");
            result.append("\"siteName\": \"").append(siteRepository.findById(foundPages.get(offset).getSiteId()).get().getName()).append("\",\n");
            result.append("\"uri\": \"").append(foundPages.get(offset).getUri()).append("\",\n");
            result.append("\"title\": \"").append(foundPages.get(offset).getTitle()).append("\",\n");
            result.append("\"snippet\": \"").append(foundPages.get(offset).getSnippet()).append("\",\n");
            result.append("\"relevance\": \"").append(foundPages.get(offset).getRelevance()).append("\"\n}");
            if(offset != pageLimit - 1){
                result.append(",\n");
            }
        }
        result.append("\n]\n}");

        try {
            resultJson = (JSONObject) parser.parse(result.toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new ResponseEntity(resultJson, HttpStatus.OK);
    }


}
