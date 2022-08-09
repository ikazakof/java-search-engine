package main.services;

import lombok.NoArgsConstructor;
import main.data.model.*;
import main.data.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ForkJoinPool;

@Service
@NoArgsConstructor
public class IndexingServices {

    @Value("${user-agent.name}")
    String userAgent;

    SiteRepository siteRepository;
    PageRepository pageRepository;
    LemmaRepository lemmaRepository;
    IndexRepository indexRepository;
    FieldRepository fieldRepository;
    SiteConditionsChanger siteConditionsChanger;
    ResultPageLoader resultPageLoader;
    Indexer indexer;
    IndexingPageClone indexingPageClone;
    IndexLoader indexLoader;
    LemmasLoader lemmasLoader;
    LemmasFrequencyReducer lemmasFrequencyReducer;

    @Autowired
    public IndexingServices(IndexingPageClone indexingPageClone, SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository, FieldRepository fieldRepository, SiteConditionsChanger siteConditionsChanger, ResultPageLoader resultPageLoader, LemmasFrequencyReducer lemmasFrequencyReducer, Indexer indexer, IndexLoader indexLoader, LemmasLoader lemmasLoader) {
        this.indexingPageClone = indexingPageClone;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.fieldRepository = fieldRepository;
        this.siteConditionsChanger = siteConditionsChanger;
        this.resultPageLoader = resultPageLoader;
        this.lemmasFrequencyReducer = lemmasFrequencyReducer;
        this.indexer = indexer;
        this.indexLoader = indexLoader;
        this.lemmasLoader = lemmasLoader;
    }

    public void indexTargetSite(Site targetSite){
        ForkJoinPool pagingPool = new ForkJoinPool();
        SiteCrawler siteCrawler = new SiteCrawler(targetSite.getUrl(), userAgent, siteRepository);
        pagingPool.execute(siteCrawler);
        pagingPool.shutdown();
        TreeMap<String, Page> results = siteCrawler.join();

        if (results.isEmpty() || siteRepository.findById(targetSite.getId()).get().getStatus().equals(Status.FAILED)) {
            if(siteRepository.findById(targetSite.getId()).get().getLastError() == null){
                siteConditionsChanger.changeSiteConditionsEmptyPages(targetSite);
            }
            results.clear();
        } else {
            synchronized (pageRepository) {
                pageRepository.saveAll(results.values());
            }
        }

        ForkJoinPool lemmaPool = new ForkJoinPool();
        Lemmatizer lemmatizer = new Lemmatizer(resultPageLoader.getCorrectlyResponsivePages(results.values()), fieldRepository.findAll(), targetSite.getId());
        lemmaPool.execute(lemmatizer);
        lemmaPool.shutdown();
        TreeMap<Integer, TreeMap<Lemma, Float>> lemmasResult = lemmatizer.join();

        if (lemmasResult.isEmpty() || siteRepository.findById(targetSite.getId()).get().getStatus().equals(Status.FAILED)) {
            if(siteRepository.findById(targetSite.getId()).get().getLastError() == null){
                siteConditionsChanger.changeSiteConditionsEmptyLemmas(targetSite);
            }
            lemmasResult.clear();
        }
        ResultLemmaLoader resultLemmaLoader = new ResultLemmaLoader(lemmasResult.values());
        if(lemmasResult.size() != 0) {
            synchronized (lemmaRepository) {
                lemmaRepository.saveAll(resultLemmaLoader.getLemmaResultToDB().values());
            }
        }
        ArrayList<Index> indexResult = new ArrayList<>(indexer.getIndexes(lemmasResult, resultLemmaLoader.getLemmaResultToDB()));

        if (indexResult.isEmpty() || siteRepository.findById(targetSite.getId()).get().getStatus().equals(Status.FAILED)) {
            if(siteRepository.findById(targetSite.getId()).get().getLastError() == null){
                siteConditionsChanger.changeSiteConditionsEmptyIndex(targetSite);
            }
        } else {
            indexRepository.saveAll(indexResult);
            siteConditionsChanger.changeSiteConditionsSuccessIndexed(targetSite);
        }
    }

    public void indexTargetPage(String url){
        Site targetSite = new Site();
        siteConditionsChanger.cloneSiteConditionPageIndexing(targetSite, url);

        String targetUrl = targetSite.getUrl().equals(url) ?  "/" : url.replaceAll(targetSite.getUrl(), "");

        Page targetPage = new Page();
        indexingPageClone.partiallyCloneTargetIndexingPage(targetPage, new Page(targetUrl, targetSite.getId()));

        if(targetPage.getId() != 0){
            HashMap<Integer, Index> existingIndexes = new HashMap<>(indexLoader.loadIndexFromDB(targetPage.getId()));
            lemmasFrequencyReducer.reduceLemmasFrequency(lemmasLoader.loadLemmasFromDBWithIndex(existingIndexes));
            indexRepository.deleteAll(existingIndexes.values());
        }

        SiteConnector siteConnector = new SiteConnector(userAgent, url);
        targetPage.setAnswerCode(siteConnector.getStatusCode());
        targetPage.setPageContent(siteConnector.getSiteDocument().toString());
        targetPage.setSiteId(targetSite.getId());
        targetPage.setPath(targetUrl);
        pageRepository.save(targetPage);

        List<Page> resultPagesList = new ArrayList<>();
        resultPagesList.add(resultPageLoader.getCorrectlyResponsivePage(targetPage));
        if(resultPageLoader.getCorrectlyResponsivePage(targetPage).getId() != 0) {
            pageRepository.save(targetPage);
        }

        ForkJoinPool lemmaPool = new ForkJoinPool();
        Lemmatizer lemmatizer = new Lemmatizer(resultPagesList, fieldRepository.findAll(), targetSite.getId());
        lemmaPool.execute(lemmatizer);
        lemmaPool.shutdown();
        TreeMap<Integer, TreeMap<Lemma, Float>> lemmasResult = lemmatizer.join();

        if (lemmasResult.isEmpty() || siteRepository.findById(targetSite.getId()).get().getStatus().equals(Status.FAILED)) {
            if(siteRepository.findById(targetSite.getId()).get().getLastError() == null){
                siteConditionsChanger.changeSiteConditionsEmptyLemmasOnPage(targetSite);
            }
            lemmasResult.clear();
        }

        ResultLemmasNormalizer resultLemmasNormalizer = new ResultLemmasNormalizer(new ResultLemmaLoader(lemmasResult.values()).getLemmaResultToDB(), lemmasLoader.loadSiteLemmasFromDB(targetSite.getId()));
        if(lemmasResult.size() != 0) {
            synchronized (lemmaRepository) {
                lemmaRepository.saveAll(resultLemmasNormalizer.getLemmaNormalizedResult().values());
            }
        }
        ArrayList<Index> indexResult = new ArrayList<>(indexer.getIndexes(lemmasResult, resultLemmasNormalizer.getLemmaNormalizedResult()));

        if(indexResult.isEmpty() || siteRepository.findById(targetSite.getId()).get().getStatus().equals(Status.FAILED)){
            if(siteRepository.findById(targetSite.getId()).get().getLastError() == null){
                siteConditionsChanger.changeSiteConditionsEmptyIndexOnPage(targetSite);
            }
        } else {
            indexRepository.saveAll(indexResult);
            siteConditionsChanger.changeSiteConditionsSuccessIndexed(targetSite);
        }
    }
}
