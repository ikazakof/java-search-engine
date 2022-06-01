package main.controllers;

import main.data.dto.FoundPage;
import main.data.model.*;
import main.data.repository.*;
import main.services.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Connection;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;



@RestController
public class IndexController {
    
    @Autowired
    StartParamList startParamList;

    @Value("${user-agent.name}")
    private String userAgent;

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

        for (Site site : siteRepository.findAll()) {
            if (site.getStatus().equals(Status.INDEXING)) {
                try {
                    return new ResponseEntity (parser.parse("{\n\"result\": false,\n\"error\": \"Индексация уже запущена\"\n}"), HttpStatus.OK);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
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
                    siteFromDB.setStatus(Status.FAILED);
                    siteFromDB.setLastError(siteRepository.findById(siteFromDB.getId()).get().getLastError().compareTo("Индексация принудительно остановлена") == 0 ? "Индексация принудительно остановлена" : "Сайт пуст");
                    siteFromDB.setStatusTime(LocalDateTime.now());
                    siteRepository.save(siteFromDB);

                    results.clear();
            } else {
                    synchronized (pageRepository) {
                    pageRepository.saveAll(results.values());
                } }

                List<Page> resultPagesList = new ArrayList<>();
                results.values().forEach(page -> {
                    if(!(page.getAnswerCode() >= 400 && page.getAnswerCode() <= 417) && !(page.getAnswerCode() >= 500 && page.getAnswerCode() <= 505)){
                        resultPagesList.add(page);
                    }
                });

                List<Field> fieldsList = new ArrayList<>();
                if(fieldRepository.count() != 0){
                    fieldRepository.findAll().forEach(fieldsList::add);
            }

                ForkJoinPool lemmaPool = new ForkJoinPool();
                Lemmatizer lemmatizer = new Lemmatizer(resultPagesList, fieldsList, siteFromDB.getId());
                lemmaPool.execute(lemmatizer);
                lemmaPool.shutdown();
                TreeMap<Integer, TreeMap<Lemma, Float>> lemmResult = lemmatizer.join();

                if (lemmResult.isEmpty() || siteRepository.findById(siteFromDB.getId()).get().getStatus().equals(Status.FAILED)) {
                    siteFromDB.setStatus(Status.FAILED);
                    siteFromDB.setLastError(siteRepository.findById(siteFromDB.getId()).get().getLastError().compareTo("Индексация принудительно остановлена") == 0 ? "Индексация принудительно остановлена" : "Леммы не найдены");
                    siteFromDB.setStatusTime(LocalDateTime.now());
                    siteRepository.save(siteFromDB);
                    lemmResult.clear();
                }

                TreeMap<String, Lemma> lemmaResultToDB = new TreeMap<>();

                if(lemmaRepository.count() != 0 && lemmResult.size() != 0){
                    lemmaRepository.findAll().forEach(lemma -> {
                        lemmaResultToDB.put(lemma.getLemma(), lemma);
                    });
                }
                for (Map.Entry<Integer, TreeMap<Lemma, Float>> entry : lemmResult.entrySet()) {
                entry.getValue().forEach((lemma, rank) -> {
                    if (lemmaResultToDB.containsKey(lemma.getLemma())) {
                        lemmaResultToDB.get(lemma.getLemma()).increaseFrequency();
                    } else {
                        lemmaResultToDB.put(lemma.getLemma(), lemma);
                    }
                });
            }

                    if(lemmResult.size() != 0) {
                        synchronized (lemmaRepository) {
                        lemmaRepository.saveAll(lemmaResultToDB.values());
                    }
                }

                ArrayList<Index> indexResult = new ArrayList<>();

                for(Map.Entry<Integer, TreeMap<Lemma, Float>> page : lemmResult.entrySet()){
                   for (Map.Entry<Lemma, Float> entry : page.getValue().entrySet()){
                       indexResult.add(new Index(page.getKey(), lemmaResultToDB.get(entry.getKey().getLemma()).getId(), entry.getValue()));
                       siteFromDB.setStatusTime(LocalDateTime.now());
                       siteRepository.save(siteFromDB);
                   }
                }

                if (indexResult.isEmpty() || siteRepository.findById(siteFromDB.getId()).get().getStatus().equals(Status.FAILED)) {
                    siteFromDB.setStatus(Status.FAILED);
                    siteFromDB.setLastError(siteRepository.findById(siteFromDB.getId()).get().getLastError().compareTo("Индексация принудительно остановлена") == 0 ? "Индексация принудительно остановлена" : "Индексы отсутсвуют");
                    siteFromDB.setStatusTime(LocalDateTime.now());
                } else {
                    indexRepository.saveAll(indexResult);
                    siteFromDB.setStatus(Status.INDEXED);
                    siteFromDB.setStatusTime(LocalDateTime.now());
                    siteFromDB.setLastError(null);
                }
                siteRepository.save(siteFromDB);
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
        boolean indexingStart = false;
        for (Site site : siteRepository.findAll()){
            if (site.getStatus().equals(Status.INDEXING)){
                indexingStart = true;
                break;
            }
        }
        if(!indexingStart){
            try {
                return new ResponseEntity (parser.parse("{\n\"result\": false,\n\"error\": \"Индексация не запущена\"\n}"), HttpStatus.OK);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        for (Site site : siteRepository.findAll()){
            if(site.getStatus().equals(Status.INDEXING)){
                site.setStatus(Status.FAILED);
                site.setLastError("Индексация принудительно остановлена");
                siteRepository.save(site);
            }
        }
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
        boolean isPageInSiteRange = false;
        for(Site site : siteRepository.findAll()) {
            if (url.matches(site.getUrl() + ".*")) {
                isPageInSiteRange = true;
                break;
            }
        }
        if(!isPageInSiteRange){
            try {
               return new ResponseEntity (parser.parse("{\n\"result\": false,\n\"error\": \"Данная страница находится за пределами сайтов,указанных в конфигурационном файле\"\n}"), HttpStatus.OK);
            } catch (ParseException e) {
                e.printStackTrace();
            }

        }

        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(()-> {
            Site targetSite = new Site();
            for(Site site : siteRepository.findAll()) {
                if (url.matches(site.getUrl() + ".*")) {
                   targetSite = site;
                   break;
                }
            }
            targetSite.setStatus(Status.INDEXING);
            targetSite.setStatusTime(LocalDateTime.now());
            siteRepository.save(targetSite);

            String targetUrl = "";
            if(targetSite.getUrl().equals(url)){
                targetUrl = "/";
            } else {
                targetUrl = url.replaceAll(targetSite.getUrl(), "");
            }

            Page targetPage = new Page();

            for(Page page : pageRepository.findAll()){
                Comparator<Page> comparator = Comparator.comparing(Page::getSiteId)
                        .thenComparing(Page::getPath);
                if(comparator.compare(page, new Page(targetUrl, targetSite.getId())) == 0){
                    targetPage = page;
                    break;
                }
            }

            TreeMap<String, Lemma> existingLemmas = new TreeMap<>();
            lemmaRepository.findAll().forEach(lemma -> {
                existingLemmas.put(lemma.getLemma(), lemma);
            });

            if(targetPage.getId() != 0){
                TreeMap<Integer, Index> existingIndexes = new TreeMap<>();
                for (Index indexFromDB : indexRepository.findAll()){
                    if(indexFromDB.getPageId() == targetPage.getId()){
                        existingIndexes.put(indexFromDB.getLemmaId(), indexFromDB);
                    }
                }
                indexRepository.deleteAll(existingIndexes.values());

                existingLemmas.forEach((lemmaName, lemma) -> {
                    if(existingIndexes.containsKey(lemma.getId())){
                        lemma.decreaseFrequency();
                        if(lemma.getFrequency() == 0){
                            lemmaRepository.deleteById(lemma.getId());
                            existingLemmas.remove(lemma.getLemma());
                        }
                    }
                });
            }

            Connection.Response cachedResource = null;

            try {
//                cachedResource = Jsoup.connect(url).userAgent(userAgent).referrer("http://www.google.com").ignoreHttpErrors(true).maxBodySize(0).execute();
                targetPage.setAnswerCode(cachedResource.statusCode());
                targetPage.setPageContent(cachedResource.parse().toString());
                targetPage.setSiteId(targetSite.getId());
                targetPage.setPath(targetUrl);
            } catch (IOException exception) {
                exception.printStackTrace();
            }

            List<Page> resultPagesList = new ArrayList<>();
            if (!(targetPage.getAnswerCode() >= 400 && targetPage.getAnswerCode() <= 417) && !(targetPage.getAnswerCode() >= 500 && targetPage.getAnswerCode() <= 505) && !siteRepository.findById(targetSite.getId()).get().getStatus().equals(Status.FAILED))
            {
                resultPagesList.add(targetPage);
                pageRepository.save(targetPage);
            }

            List<Field> fieldsList = new ArrayList<>();
            if(fieldRepository.count() != 0){
                fieldRepository.findAll().forEach(fieldsList::add);
            }

            ForkJoinPool lemmaPool = new ForkJoinPool();
            Lemmatizer lemmatizer = new Lemmatizer(resultPagesList, fieldsList, targetSite.getId());
            lemmaPool.execute(lemmatizer);
            lemmaPool.shutdown();
            TreeMap<Integer, TreeMap<Lemma, Float>> lemmResult = lemmatizer.join();

            if (lemmResult.isEmpty() || siteRepository.findById(targetSite.getId()).get().getStatus().equals(Status.FAILED)) {
                targetSite.setStatus(Status.FAILED);
                targetSite.setLastError(siteRepository.findById(targetSite.getId()).get().getLastError().compareTo("Индексация принудительно остановлена") == 0 ? "Индексация принудительно остановлена" : "Леммы не найдены");
                siteRepository.save(targetSite);
                lemmResult.clear();
            }

            for (Map.Entry<Integer, TreeMap<Lemma, Float>> entry : lemmResult.entrySet()) {
                entry.getValue().forEach((lemma, rank) -> {
                    if (existingLemmas.containsKey(lemma.getLemma())) {
                        existingLemmas.get(lemma.getLemma()).increaseFrequency();
                    } else {
                        existingLemmas.put(lemma.getLemma(), lemma);
                    }
                });
            }
            if(!lemmResult.isEmpty()) {
                synchronized (lemmaRepository) {
                    lemmaRepository.saveAll(existingLemmas.values());
                }
            }

            ArrayList<Index> indexResult = new ArrayList<>();

            for(Map.Entry<Integer, TreeMap<Lemma, Float>> page : lemmResult.entrySet()){
                for (Map.Entry<Lemma, Float> entry : page.getValue().entrySet()){
                    indexResult.add(new Index(page.getKey(), existingLemmas.get(entry.getKey().getLemma()).getId(), entry.getValue()));
                    targetSite.setStatusTime(LocalDateTime.now());
                    siteRepository.save(targetSite);
                }
            }

            if(indexResult.isEmpty() || siteRepository.findById(targetSite.getId()).get().getStatus().equals(Status.FAILED)){
                targetSite.setStatus(Status.FAILED);
                targetSite.setLastError(siteRepository.findById(targetSite.getId()).get().getLastError().compareTo("Индексация принудительно остановлена") == 0 ? "Индексация принудительно остановена остановлена" : "Индексы отсутсвуют");
                targetSite.setStatusTime(LocalDateTime.now());
            } else {
                indexRepository.saveAll(indexResult);
                targetSite.setStatus(Status.INDEXED);
                targetSite.setStatusTime(LocalDateTime.now());
                targetSite.setLastError(null);
            }
            siteRepository.save(targetSite);

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


