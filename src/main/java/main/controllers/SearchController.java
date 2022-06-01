package main.controllers;

import main.data.dto.FoundPage;
import main.data.model.*;
import main.data.repository.*;
import main.services.RelevantPageLoader;
import main.services.Search;
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
import java.util.TreeMap;

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
        TreeMap<Integer, Page> targetSitePages = new TreeMap<>();
        TreeMap<Integer, Lemma> targetSiteLemmas = new TreeMap<>();

        if(site != null && !site.isEmpty()){

            Site targetSite = new Site();
            for (Site siteFromDB : siteRepository.findAll()) {
                if (siteFromDB.getUrl().equals(site)) {
                    targetSite = siteFromDB;
                }
            }
            if(targetSite.getId() == 0){
                try {
                    return new ResponseEntity (parser.parse("{\n\"result\": false,\n\"error\": \"Запрашиваемый сайт отсутсвует в базе данных\"\n}"), HttpStatus.OK);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            for (Page pageFromDb : pageRepository.findAll()) {
                if (pageFromDb.getSiteId() == targetSite.getId()){
                    targetSitePages.put(pageFromDb.getId(), pageFromDb);
                }
            }

            if(targetSitePages.size() == 0){
                try {
                    return new ResponseEntity (parser.parse( siteRepository.findById(targetSite.getId()).get().getStatus().compareTo(Status.INDEXING) == 0 ? "{\n\"result\": false,\n\"error\": \"Запрашиваемый сайт индексируется, попробуйте позже\"\n}" : "{\n\"result\": false,\n\"error\": \"Запрашиваемый сайт не имеет страниц в базе данных\"\n}"), HttpStatus.OK);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }


            for(Lemma lemmaFromDB : lemmaRepository.findAll()){
                if(lemmaFromDB.getSiteId() == targetSite.getId() && !lemmaFrequencyIsOften(lemmaFromDB)){
                    targetSiteLemmas.put(lemmaFromDB.getId(), lemmaFromDB);
                }
            }

            if(targetSiteLemmas.size() == 0){
                try {
                    return new ResponseEntity (parser.parse(siteRepository.findById(targetSite.getId()).get().getStatus().compareTo(Status.INDEXING) == 0 ? "{\n\"result\": false,\n\"error\": \"Запрашиваемый сайт индексируется, попробуйте позже\"\n}" : "{\n\"result\": false,\n\"error\": \"Запрашиваемый сайт не имеет лемм в базе данных\"\n}"), HttpStatus.OK);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }


            for(Index indexFromDB : indexRepository.findAll()) {
                if (targetSitePages.containsKey(indexFromDB.getPageId())){
                    indexesFromDB.add(indexFromDB);
                }
            }

            if(indexesFromDB.size() == 0){
                try {
                    return new ResponseEntity (parser.parse(siteRepository.findById(targetSite.getId()).get().getStatus().compareTo(Status.INDEXING) == 0 ? "{\n\"result\": false,\n\"error\": \"Запрашиваемый сайт индексируется, попробуйте позже\"\n}" : "{\n\"result\": false,\n\"error\": \"Запрашиваемый сайт не имеет индексов в базе данных\"\n}"), HttpStatus.OK);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }

        boolean atLeastOneSiteIndexed = false;
        for (Site siteFromDB : siteRepository.findAll()){
            if (siteFromDB.getStatus().equals(Status.INDEXED)) {
                atLeastOneSiteIndexed = true;
                break;
            }
        }


        if(indexesFromDB.isEmpty() && !atLeastOneSiteIndexed){
            try {
                return new ResponseEntity (parser.parse("{\n\"result\": false,\n\"error\": \"Отсутсвуют проиндексированные сайты\"\n}"), HttpStatus.OK);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        if(targetSiteLemmas.isEmpty()){
            for(Site siteFromDb : siteRepository.findAll()) {
                if (!siteFromDb.getStatus().equals(Status.INDEXED)) {
                    continue;
                }
                for (Lemma lemmaFromDB : lemmaRepository.findAll()){
                    if(lemmaFromDB.getSiteId() == siteFromDb.getId() && !lemmaFrequencyIsOften(lemmaFromDB)){
                        targetSiteLemmas.put(lemmaFromDB.getId(), lemmaFromDB);
                    }
                }
            }
        }

        if(indexesFromDB.isEmpty()){
            indexRepository.findAll().forEach(indexFromDB -> {
                if(targetSiteLemmas.containsKey(indexFromDB.getLemmaId())){
                    indexesFromDB.add(indexFromDB);
                }
            });
        }

        Search search = new Search(query, targetSiteLemmas.values(), indexesFromDB);
        if (search.getFoundPages().isEmpty()){
            try {
                return new ResponseEntity (parser.parse("{\n\"result\": false,\n\"error\": \"Отсутсвуют совпадения\"\n}"), HttpStatus.OK);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        ArrayList<Page> relevantPages = new ArrayList<>();

        if(!targetSitePages.isEmpty()){
            targetSitePages.values().forEach(pageFromDB -> {
                if(search.getFoundPages().containsKey(pageFromDB .getId())){
                    relevantPages.add(pageFromDB);
                }
            });
        } else {
            pageRepository.findAll().forEach(pageFromDB -> {
                if(search.getFoundPages().containsKey(pageFromDB.getId())){
                    relevantPages.add(pageFromDB);
                }
            });
        }

        ArrayList<Lemma> relevantLemmas = new ArrayList<>();

        if(!targetSiteLemmas.isEmpty()){
            targetSiteLemmas.values().forEach(lemmaFromDB -> {
                search.getFoundPages().firstEntry().getValue().forEach(relevantIndex -> {
                    if(lemmaFromDB.getId() == relevantIndex.getLemmaId()){
                        relevantLemmas.add(lemmaFromDB);
                    }
                });
            });
        } else {
            lemmaRepository.findAll().forEach(lemmaFromDB -> {
                search.getFoundPages().firstEntry().getValue().forEach(relevantIndex -> {
                    if(lemmaFromDB.getId() == relevantIndex.getLemmaId()){
                        relevantLemmas.add(lemmaFromDB);
                    }
                });
            });
        }

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


    private boolean lemmaFrequencyIsOften(Lemma lemma){
        return lemma.getFrequency() >= (pageRepository.count()) - (pageRepository.count() / 100) * 70;
    }

}
