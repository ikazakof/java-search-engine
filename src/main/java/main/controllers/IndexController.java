package main.controllers;

import main.data.model.*;
import main.data.repository.*;
import main.services.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;



@RestController
public class IndexController {

    @Autowired
    StartParamList startParamList;

    @Value("${user-agent.name}")
    String userAgent;

    @Autowired
    SiteRepository siteRepository;
    @Autowired
    PageRepository pageRepository;
    @Autowired
    LemmaRepository lemmaRepository;
    @Autowired
    IndexRepository indexRepository;
    @Autowired
    FieldRepository fieldRepository;



    @GetMapping("/startIndexing")
    public ResponseEntity startIndexing() {
        JSONParser parser = new JSONParser();
        JSONObject result = new JSONObject();

        if (SiteStatusChecker.indexingSitesExist(siteRepository)) {
            try {
                return new ResponseEntity (parser.parse("{\n\"result\": false,\n\"error\": \"Индексация уже запущена\"\n}"), HttpStatus.OK);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        siteRepository.deleteAll();
        pageRepository.deleteAll();
        lemmaRepository.deleteAll();
        indexRepository.deleteAll();
        fieldRepository.deleteAll();
        fieldRepository.saveAll(startParamList.getField());
        siteRepository.saveAll(startParamList.getSites());

        for (Site siteFromDB : siteRepository.findAll()) {

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(()-> {
                ForkJoinPool pagingPool = new ForkJoinPool();
                SiteCrawler siteCrawler = new SiteCrawler(siteFromDB.getUrl(), userAgent, siteRepository);
                pagingPool.execute(siteCrawler);
                pagingPool.shutdown();
                TreeMap<String, Page> results = siteCrawler.join();

                if (results.isEmpty() || siteRepository.findById(siteFromDB.getId()).get().getStatus().equals(Status.FAILED)) {
                    if(siteRepository.findById(siteFromDB.getId()).get().getLastError() == null){
                        SiteConditionsChanger.changeSiteConditionsEmptyPages(siteFromDB, siteRepository);
                    }
                    results.clear();
                } else {
                    synchronized (pageRepository) {
                        pageRepository.saveAll(results.values());
                    } }

                ForkJoinPool lemmaPool = new ForkJoinPool();
                Lemmatizer lemmatizer = new Lemmatizer(ResultPageLoader.getCorrectlyResponsivePages(results.values()), fieldRepository.findAll(), siteFromDB.getId());
                lemmaPool.execute(lemmatizer);
                lemmaPool.shutdown();
                TreeMap<Integer, TreeMap<Lemma, Float>> lemmasResult = lemmatizer.join();

                if (lemmasResult.isEmpty() || siteRepository.findById(siteFromDB.getId()).get().getStatus().equals(Status.FAILED)) {
                    if(siteRepository.findById(siteFromDB.getId()).get().getLastError() == null){
                        SiteConditionsChanger.changeSiteConditionsEmptyLemmas(siteFromDB, siteRepository);
                    }
                    lemmasResult.clear();
                }
                ResultLemmaLoader resultLemmaLoader = new ResultLemmaLoader(lemmasResult.values());
                if(lemmasResult.size() != 0) {
                synchronized (lemmaRepository) {
                    lemmaRepository.saveAll(resultLemmaLoader.getLemmaResultToDB().values());
                }
                }
                ArrayList<Index> indexResult = new ArrayList<>(Indexer.getIndexes(lemmasResult, resultLemmaLoader.getLemmaResultToDB() , siteRepository));

                if (indexResult.isEmpty() || siteRepository.findById(siteFromDB.getId()).get().getStatus().equals(Status.FAILED)) {
                if(siteRepository.findById(siteFromDB.getId()).get().getLastError() == null){
                    SiteConditionsChanger.changeSiteConditionsEmptyIndex(siteFromDB, siteRepository);
                }
                } else {
                    indexRepository.saveAll(indexResult);
                    SiteConditionsChanger.changeSiteConditionsSuccessIndexed(siteFromDB, siteRepository);
                }
            });
            executor.shutdown();
        }
        try {
            result = (JSONObject) parser.parse("{\n\"result\": true\n}");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new ResponseEntity(result, HttpStatus.OK);
}

    @GetMapping("/stopIndexing")
    public ResponseEntity stopIndexing() {
        JSONParser parser = new JSONParser();
        JSONObject result = new JSONObject();
        if(!SiteStatusChecker.indexingSitesExist(siteRepository)){
            try {
                return new ResponseEntity (parser.parse("{\n\"result\": false,\n\"error\": \"Индексация не запущена\"\n}"), HttpStatus.OK);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        SiteConditionsChanger.changeSitesConditionStopIndex(siteRepository);
        try {
            result = (JSONObject) parser.parse("{\n\"result\": true\n}");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new ResponseEntity(result, HttpStatus.OK);
    }

    @PostMapping("/indexPage")
    public ResponseEntity indexPage(@RequestParam String url){
        JSONParser parser = new JSONParser();
        JSONObject result = new JSONObject();

        if(!IndexingPageChecker.indexingPageInRange(siteRepository, url)){
            try {
                return new ResponseEntity (parser.parse("{\n\"result\": false,\n\"error\": \"Данная страница находится за пределами сайтов,указанных в конфигурационном файле\"\n}"), HttpStatus.OK);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(()-> {
            Site targetSite = new Site();
            SiteConditionsChanger.cloneSiteConditionPageIndexing(targetSite, url, siteRepository);

            String targetUrl = "";
            if(targetSite.getUrl().equals(url)){
                targetUrl = "/";
            } else {
                targetUrl = url.replaceAll(targetSite.getUrl(), "");
            }

            Page targetPage = new Page();
            IndexingPageClone.partiallyCloneTargetIndexingPage(targetPage, new Page(targetUrl, targetSite.getId()), pageRepository);

            if(targetPage.getId() != 0){
                TreeMap<Integer, Index> existingIndexes = new TreeMap<>(IndexLoader.loadIndexFromDB(targetPage.getId(), indexRepository));
                TreeMap<String, Lemma> existingLemmas = new TreeMap<>(LemmasLoader.loadLemmasFromDBWithIndex(existingIndexes, lemmaRepository));
                LemmasFrequencyReducer.reduceLemmasFrequency(existingLemmas, lemmaRepository);
                indexRepository.deleteAll(existingIndexes.values());
            }

            SiteConnector siteConnector = new SiteConnector(userAgent, url);
            targetPage.setAnswerCode(siteConnector.getStatusCode());
            targetPage.setPageContent(siteConnector.getSiteDocument().toString());

            List<Page> resultPagesList = new ArrayList<>();
            resultPagesList.add(ResultPageLoader.getCorrectlyResponsivePage(targetPage));
            if(ResultPageLoader.getCorrectlyResponsivePage(targetPage).getId() != 0) {
                pageRepository.save(targetPage);
            }

            ForkJoinPool lemmaPool = new ForkJoinPool();
            Lemmatizer lemmatizer = new Lemmatizer(resultPagesList, fieldRepository.findAll(), targetSite.getId());
            lemmaPool.execute(lemmatizer);
            lemmaPool.shutdown();
            TreeMap<Integer, TreeMap<Lemma, Float>> lemmasResult = lemmatizer.join();

            if (lemmasResult.isEmpty() || siteRepository.findById(targetSite.getId()).get().getStatus().equals(Status.FAILED)) {
                if(siteRepository.findById(targetSite.getId()).get().getLastError() == null){
                    SiteConditionsChanger.changeSiteConditionsEmptyLemmasOnPage(targetSite, siteRepository);
                }
                lemmasResult.clear();
            }

            ResultLemmaLoader resultLemmaLoader = new ResultLemmaLoader(lemmasResult.values());
            ResultLemmasNormalizer resultLemmasNormalizer = new ResultLemmasNormalizer(resultLemmaLoader.getLemmaResultToDB(), LemmasLoader.loadLemmasFromDB(targetSite.getId(), lemmaRepository));
            if(lemmasResult.size() != 0) {
                synchronized (lemmaRepository) {
                    lemmaRepository.saveAll(resultLemmasNormalizer.getLemmaNormalizedResult().values());
                }
            }
            ArrayList<Index> indexResult = new ArrayList<>(Indexer.getIndexes(lemmasResult, resultLemmasNormalizer.getLemmaNormalizedResult(), siteRepository));

            if(indexResult.isEmpty() || siteRepository.findById(targetSite.getId()).get().getStatus().equals(Status.FAILED)){
                if(siteRepository.findById(targetSite.getId()).get().getLastError() == null){
                    SiteConditionsChanger.changeSiteConditionsEmptyIndexOnPage(targetSite, siteRepository);
                }
            } else {
                indexRepository.saveAll(indexResult);
                SiteConditionsChanger.changeSiteConditionsSuccessIndexed(targetSite, siteRepository);
            }
        });
        executor.shutdown();
        try {
            result = (JSONObject) parser.parse("{\n\"result\": true\n}");
        } catch (ParseException e) {
            e.printStackTrace();
        }
       return new ResponseEntity(result, HttpStatus.OK);
    }
}


