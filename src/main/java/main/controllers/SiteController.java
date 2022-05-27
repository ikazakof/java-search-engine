package main.controllers;

import main.data.dto.FoundPage;
import main.data.model.*;
import main.data.repository.*;
import main.services.Lemmatizer;
import main.services.RelevantPageLoader;
import main.services.Search;
import main.services.SiteCrawler;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;



@RestController
public class SiteController {


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
                cachedResource = Jsoup.connect(url).userAgent(userAgent).referrer("http://www.google.com").ignoreHttpErrors(true).maxBodySize(0).execute();
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

    @GetMapping("/search")
    public ResponseEntity search(@RequestParam String query,@RequestParam(required = false)  String site, @RequestParam int offset, @RequestParam int limit) {

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


