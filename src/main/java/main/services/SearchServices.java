package main.services;

import lombok.NoArgsConstructor;
import main.data.model.Index;
import main.data.model.Lemma;
import main.data.model.Page;
import main.data.model.Site;
import main.data.repository.PageRepository;
import main.data.repository.SiteRepository;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;

@Service
@NoArgsConstructor
public class SearchServices {
    SiteRepository siteRepository;
    PageRepository pageRepository;
    ResponseEntityLoader responseEntityLoader;
    SiteConditionsChanger siteConditionsChanger;
    SiteStatusChecker siteStatusChecker;
    IndexLoader indexLoader;
    LemmasLoader lemmasLoader;
    PageLoader pageLoader;

    @Autowired
    public SearchServices(SiteRepository siteRepository, PageRepository pageRepository, ResponseEntityLoader responseEntityLoader, SiteConditionsChanger siteConditionsChanger, SiteStatusChecker siteStatusChecker, IndexLoader indexLoader, LemmasLoader lemmasLoader, PageLoader pageLoader) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.responseEntityLoader = responseEntityLoader;
        this.siteConditionsChanger = siteConditionsChanger;
        this.siteStatusChecker = siteStatusChecker;
        this.indexLoader = indexLoader;
        this.lemmasLoader = lemmasLoader;
        this.pageLoader = pageLoader;
    }


    public ResponseEntity<JSONObject> getMatchesInSite(String site, String query){
        Site targetSite = new Site();
        siteConditionsChanger.cloneSiteFromDB(targetSite, site);
        if(targetSite.getId() == 0){
            return responseEntityLoader.getSiteNotFoundResponse();
        }

        HashMap<Integer, Page> targetSitePages = new HashMap<>(pageLoader.loadSitePagesFromDB(targetSite.getId()));
        if(targetSitePages.size() == 0){
            return responseEntityLoader.getSiteIndexingOrEmptyPagesResponse(targetSite);
        }

        HashMap<Integer, Lemma> targetLemmas = new HashMap<>(lemmasLoader.loadSiteLemmasFromDBWithFreq(targetSite.getId(), pageRepository.count()));

        if(targetLemmas.size() == 0){
            return  responseEntityLoader.getSiteIndexingOrEmptyLemmasResponse(targetSite);
        }

        ArrayList<Index> indexesFromDB = new ArrayList<>(indexLoader.loadIndexFromDBByPageIdAndLemmas(targetSitePages.keySet(), targetLemmas.keySet()));
        if(indexesFromDB.size() == 0){
            return  responseEntityLoader.getSiteIndexingOrEmptyIndexesResponse(targetSite);
        }
        Search search = new Search(query, targetLemmas.values(), indexesFromDB);
        if (search.getFoundPages().isEmpty()){
            return responseEntityLoader.getSearchMatchesNotFoundResponse();
        }
        ArrayList<Page> relevantPages = new ArrayList<>(pageLoader.loadPagesByIdFromTargetPages(targetSitePages, search.getFoundPages().keySet()));

        ArrayList<Lemma> relevantLemmas = new ArrayList<>(search.getSearchLemmas());

        RelevantPageLoader relevantPageLoader = new RelevantPageLoader(relevantPages, relevantLemmas, search.getFoundPages());
        if (relevantPageLoader.getRelevantPages().isEmpty()){
            return  responseEntityLoader.getRelevantPagesNotFoundResponse();
        }
        SearchResultEntityLoader searchResultEntityLoader = new SearchResultEntityLoader(siteRepository);
        return searchResultEntityLoader.getSearchResultJson(relevantPageLoader.getRelevantPages());
    }

    public ResponseEntity<JSONObject> getMatchesInSites(String query){
        if(!siteStatusChecker.indexedSitesExist()){
            return responseEntityLoader.getIndexedSitesNotFoundResponse();
        }

        HashMap<Integer, Lemma> targetLemmas = new HashMap<>(lemmasLoader.loadLemmasFromDBWithFreqAndIndexedSites(pageRepository.count()));
        ArrayList<Index> indexesFromDB = new ArrayList<>(indexLoader.loadIndexFromDBByLemmas(targetLemmas.keySet()));

        Search search = new Search(query, targetLemmas.values(), indexesFromDB);
        if (search.getFoundPages().isEmpty()){
            return responseEntityLoader.getSearchMatchesNotFoundResponse();
        }

        ArrayList<Page> relevantPages = new ArrayList<>(pageLoader.loadPagesByIDFromPagesRepository(search.getFoundPages().keySet()));

        ArrayList<Lemma> relevantLemmas = new ArrayList<>(search.getSearchLemmas());

        RelevantPageLoader relevantPageLoader = new RelevantPageLoader(relevantPages, relevantLemmas, search.getFoundPages());

        if (relevantPageLoader.getRelevantPages().isEmpty()){
            return  responseEntityLoader.getRelevantPagesNotFoundResponse();
        }
        SearchResultEntityLoader searchResultEntityLoader = new SearchResultEntityLoader(siteRepository);
        return searchResultEntityLoader.getSearchResultJson(relevantPageLoader.getRelevantPages());
    }


}
