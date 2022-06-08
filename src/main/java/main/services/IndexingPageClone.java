package main.services;

import main.data.model.Page;
import main.data.repository.PageRepository;

public class IndexingPageClone {

    public static void partiallyCloneTargetIndexingPage(Page targetPage, Page searchPage, PageRepository pageRepository){
        for(Page page : pageRepository.findAll()){
            if(page.compareTo(searchPage) == 0){
                targetPage.setId(page.getId());
                targetPage.setPath(page.getPath());
                targetPage.setSiteId(page.getSiteId());
                break;
            }
        }
    }

}
