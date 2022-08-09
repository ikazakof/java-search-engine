package main.services;


import lombok.NoArgsConstructor;
import main.data.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class DBCleaner {
    SiteRepository siteRepository;
    PageRepository pageRepository;
    LemmaRepository lemmaRepository;
    IndexRepository indexRepository;
    FieldRepository fieldRepository;

    @Autowired
    public DBCleaner(SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository, FieldRepository fieldRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.fieldRepository = fieldRepository;
    }

    public void cleanDB(){
        siteRepository.deleteAll();
        pageRepository.deleteAll();
        lemmaRepository.deleteAll();
        indexRepository.deleteAll();
        fieldRepository.deleteAll();
    }

}
