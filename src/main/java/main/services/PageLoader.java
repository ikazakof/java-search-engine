package main.services;

import lombok.NoArgsConstructor;
import main.data.model.Page;
import main.data.repository.PageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

@Component
@NoArgsConstructor
public class PageLoader {

    PageRepository pageRepository;

    @Autowired
    public PageLoader(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    public HashMap<Integer, Page> loadSitePagesFromDB(int siteId){
        HashMap<Integer, Page> result = new HashMap<>();
        for (Page pageFromDb : pageRepository.findAll()) {
            if (pageFromDb.getSiteId() == siteId){
                result.put(pageFromDb.getId(), pageFromDb);
            }
        }
        return result;
    }

    public ArrayList<Page> loadPagesByIdFromTargetPages(HashMap<Integer, Page> targetSitePages, Set<Integer> pageIds){
        ArrayList<Page> result = new ArrayList<>();
        pageIds.forEach(id -> result.add(targetSitePages.get(id)));
        return result;
    }

    public ArrayList<Page> loadPagesByIDFromPagesRepository(Set<Integer> pageIds){
        ArrayList<Page> result = new ArrayList<>();
        pageIds.forEach(id -> result.add(pageRepository.findById(id).get()));
        return result;
    }
}
