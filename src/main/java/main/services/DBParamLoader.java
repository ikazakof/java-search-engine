package main.services;

import lombok.NoArgsConstructor;
import main.data.model.StartParamList;
import main.data.repository.FieldRepository;
import main.data.repository.SiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class DBParamLoader {
    SiteRepository siteRepository;
    FieldRepository fieldRepository;
    StartParamList startParamList;

    @Autowired
    public DBParamLoader(SiteRepository siteRepository, FieldRepository fieldRepository, StartParamList startParamList) {
        this.siteRepository = siteRepository;
        this.fieldRepository = fieldRepository;
        this.startParamList = startParamList;
    }

    public void loadStartParam(){
        fieldRepository.saveAll(startParamList.getField());
        siteRepository.saveAll(startParamList.getSites());
    }

}
