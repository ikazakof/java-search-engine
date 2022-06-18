package main.controllers;

import main.data.model.*;
import main.data.repository.*;
import main.services.*;
import org.springframework.beans.factory.annotation.Autowired;
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
        if (query == null || query.isEmpty()){
            return ResponseEntityLoader.getEmptySearchQueryResponse();
        }
        ArrayList<Index> indexesFromDB = new ArrayList<>();
        HashMap<Integer, Page> targetSitePages = new HashMap<>();
        HashMap<Integer, Lemma> targetLemmas = new HashMap<>();

        if(site != null && !site.isEmpty()){
            Site targetSite = new Site();
            SiteConditionsChanger.cloneSiteFromDB(targetSite, site, siteRepository);

            if(targetSite.getId() == 0){
                return ResponseEntityLoader.getSiteNotFoundResponse();
            }

            targetSitePages.putAll(PageLoader.loadSitePagesFromDB(targetSite.getId(), pageRepository));
            if(targetSitePages.size() == 0){
               return ResponseEntityLoader.getSiteIndexingOrEmptyPagesResponse(siteRepository, targetSite);
            }

            targetLemmas.putAll(LemmasLoader.loadSiteLemmasFromDBWithFreq(targetSite.getId(), lemmaRepository, pageRepository.count()));

            if(targetLemmas.size() == 0){
                return  ResponseEntityLoader.getSiteIndexingOrEmptyLemmasResponse(siteRepository, targetSite);
            }

            indexesFromDB.addAll(IndexLoader.loadIndexFromDBByPageIdAndLemmas(targetSitePages.keySet(), indexRepository, targetLemmas.keySet()));
            if(indexesFromDB.size() == 0){
                return  ResponseEntityLoader.getSiteIndexingOrEmptyIndexesResponse(siteRepository, targetSite);
            }
        }

        if(indexesFromDB.isEmpty() && !SiteStatusChecker.indexedSitesExist(siteRepository)){
            return ResponseEntityLoader.getIndexedSitesNotFoundResponse();
        }

        if(targetLemmas.isEmpty()){
            targetLemmas.putAll(LemmasLoader.loadLemmasFromDBWithFreqAndIndexedSites(siteRepository, lemmaRepository, pageRepository.count()));
            indexesFromDB.addAll(IndexLoader.loadIndexFromDBByLemmas(indexRepository, targetLemmas.keySet()));
        }

        Search search = new Search(query, targetLemmas.values(), indexesFromDB);
        if (search.getFoundPages().isEmpty()){
            return ResponseEntityLoader.getSearchMatchesNotFoundResponse();
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
            return  ResponseEntityLoader.getRelevantPagesNotFoundResponse();
        }

        SearchResultEntityLoader searchResultEntityLoader = new SearchResultEntityLoader(siteRepository);
        return searchResultEntityLoader.getSearchResultJson(limit, offset, relevantPageLoader.getRelevantPages());
    }
}
