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
    @Autowired
    SiteRepository siteRepository;
    @Autowired
    PageRepository pageRepository;
    @Autowired
    ResponseEntityLoader responseEntityLoader;
    @Autowired
    SiteConditionsChanger siteConditionsChanger;
    @Autowired
    SiteStatusChecker siteStatusChecker;
    @Autowired
    IndexLoader indexLoader;
    @Autowired
    LemmasLoader lemmasLoader;


    public ResponseEntity<JSONObject> getMatchesInSite(String site, String query){
        ArrayList<Index> indexesFromDB = new ArrayList<>();
        HashMap<Integer, Page> targetSitePages = new HashMap<>();
        HashMap<Integer, Lemma> targetLemmas = new HashMap<>();

        Site targetSite = new Site();
        siteConditionsChanger.cloneSiteFromDB(targetSite, site);
        if(targetSite.getId() == 0){
            return responseEntityLoader.getSiteNotFoundResponse();
        }

        targetSitePages.putAll(PageLoader.loadSitePagesFromDB(targetSite.getId(), pageRepository));
        if(targetSitePages.size() == 0){
            return responseEntityLoader.getSiteIndexingOrEmptyPagesResponse(targetSite);
        }

        targetLemmas.putAll(lemmasLoader.loadSiteLemmasFromDBWithFreq(targetSite.getId(), pageRepository.count()));

        if(targetLemmas.size() == 0){
            return  responseEntityLoader.getSiteIndexingOrEmptyLemmasResponse(targetSite);
        }

        indexesFromDB.addAll(indexLoader.loadIndexFromDBByPageIdAndLemmas(targetSitePages.keySet(), targetLemmas.keySet()));
        if(indexesFromDB.size() == 0){
            return  responseEntityLoader.getSiteIndexingOrEmptyIndexesResponse(targetSite);
        }
        Search search = new Search(query, targetLemmas.values(), indexesFromDB);
        if (search.getFoundPages().isEmpty()){
            return responseEntityLoader.getSearchMatchesNotFoundResponse();
        }
        ArrayList<Page> relevantPages = new ArrayList<>();
        relevantPages.addAll(PageLoader.loadPagesByIdFromTargetPages(targetSitePages, search.getFoundPages().keySet()));

        ArrayList<Lemma> relevantLemmas = new ArrayList<>();
        relevantLemmas.addAll(search.getSearchLemmas());

        RelevantPageLoader relevantPageLoader = new RelevantPageLoader(relevantPages, relevantLemmas, search.getFoundPages());
        if (relevantPageLoader.getRelevantPages().isEmpty()){
            return  responseEntityLoader.getRelevantPagesNotFoundResponse();
        }
        SearchResultEntityLoader searchResultEntityLoader = new SearchResultEntityLoader(siteRepository);
        return searchResultEntityLoader.getSearchResultJson(relevantPageLoader.getRelevantPages());
    }

    public ResponseEntity<JSONObject> getMatchesInSites(String query){
        ArrayList<Index> indexesFromDB = new ArrayList<>();
        HashMap<Integer, Lemma> targetLemmas = new HashMap<>();

        if(!siteStatusChecker.indexedSitesExist()){
            return responseEntityLoader.getIndexedSitesNotFoundResponse();
        }

        targetLemmas.putAll(lemmasLoader.loadLemmasFromDBWithFreqAndIndexedSites(pageRepository.count()));
        indexesFromDB.addAll(indexLoader.loadIndexFromDBByLemmas(targetLemmas.keySet()));

        Search search = new Search(query, targetLemmas.values(), indexesFromDB);
        if (search.getFoundPages().isEmpty()){
            return responseEntityLoader.getSearchMatchesNotFoundResponse();
        }

        ArrayList<Page> relevantPages = new ArrayList<>();
        relevantPages.addAll(PageLoader.loadPagesByIDFromPagesRepository(pageRepository, search.getFoundPages().keySet()));

        ArrayList<Lemma> relevantLemmas = new ArrayList<>();
        relevantLemmas.addAll(search.getSearchLemmas());

        RelevantPageLoader relevantPageLoader = new RelevantPageLoader(relevantPages, relevantLemmas, search.getFoundPages());

        if (relevantPageLoader.getRelevantPages().isEmpty()){
            return  responseEntityLoader.getRelevantPagesNotFoundResponse();
        }
        SearchResultEntityLoader searchResultEntityLoader = new SearchResultEntityLoader(siteRepository);
        return searchResultEntityLoader.getSearchResultJson(relevantPageLoader.getRelevantPages());
    }


}
