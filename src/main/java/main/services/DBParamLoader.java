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
    @Autowired
    SiteRepository siteRepository;
    @Autowired
    FieldRepository fieldRepository;
    @Autowired
    StartParamList startParamList;

    public void loadStartParam(){
        fieldRepository.saveAll(startParamList.getField());
        siteRepository.saveAll(startParamList.getSites());
    }

}
