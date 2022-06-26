package main.services;

import main.data.model.Page;
import main.data.repository.PageRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class PageLoader {

    public static HashMap<Integer, Page> loadSitePagesFromDB(int siteId, PageRepository pageRepository){
        HashMap<Integer, Page> result = new HashMap<>();
        for (Page pageFromDb : pageRepository.findAll()) {
            if (pageFromDb.getSiteId() == siteId){
                result.put(pageFromDb.getId(), pageFromDb);
            }
        }
        return result;
    }

    public static ArrayList<Page> loadPagesByIdFromTargetPages(HashMap<Integer, Page> targetSitePages, Set<Integer> pageIds){
        ArrayList<Page> result = new ArrayList<>();
        pageIds.forEach(id -> result.add(targetSitePages.get(id)));
        return result;
    }

    public static ArrayList<Page> loadPagesByIDFromPagesRepository(PageRepository pageRepository, Set<Integer> pageIds){
        ArrayList<Page> result = new ArrayList<>();
        pageIds.forEach(id -> result.add(pageRepository.findById(id).get()));
        return result;
    }
}
