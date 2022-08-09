package main.services;

import lombok.NoArgsConstructor;
import main.data.model.Index;
import main.data.model.Lemma;
import main.data.model.Site;
import main.data.model.Status;
import main.data.repository.LemmaRepository;
import main.data.repository.SiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;

@Component
@NoArgsConstructor
public class LemmasLoader {

    @Value("${lemma-frequency.percent}")
    int percent;

    LemmaRepository lemmaRepository;
    SiteRepository siteRepository;

    @Autowired
    public LemmasLoader(LemmaRepository lemmaRepository, SiteRepository siteRepository) {
        this.lemmaRepository = lemmaRepository;
        this.siteRepository = siteRepository;
    }

    public HashMap<String, Lemma> loadSiteLemmasFromDB(int siteId){
    HashMap<String, Lemma> tempLemmas = new HashMap<>();
    lemmaRepository.findAll().forEach(lemma -> {
        if(lemma.getSiteId() == siteId) {
            tempLemmas.put(lemma.getLemma(), lemma);
        }
    });
    return tempLemmas;
}

public HashMap<String, Lemma> loadLemmasFromDBWithIndex(HashMap<Integer, Index> existingIndexes){
    HashMap<String, Lemma> tempLemmas = new HashMap<>();
    ArrayList<Integer> existLemmasId = new ArrayList<>();
    existingIndexes.forEach((indexId, index) -> {
            if(!existLemmasId.contains(index.getLemmaId())){
                existLemmasId.add(index.getLemmaId());
            }});
    lemmaRepository.findAll().forEach(lemma -> {
        if(existLemmasId.contains(lemma.getId())) {
            tempLemmas.put(lemma.getLemma(), lemma);
        }
    });
    return tempLemmas;
}

    public HashMap<Integer, Lemma> loadSiteLemmasFromDBWithFreq(int siteId, long allPageCount) {
        HashMap<Integer, Lemma> tempLemmas = new HashMap<>();
        lemmaRepository.findAll().forEach(lemma -> {
            if (lemma.getSiteId() == siteId && !lemmaFrequencyIsOften(lemma, allPageCount)) {
                tempLemmas.put(lemma.getId(), lemma);
            }
        });
        return tempLemmas;
    }

    public HashMap<Integer, Lemma> loadLemmasFromDBWithFreqAndIndexedSites(long allPageCount) {
        HashMap<Integer, Lemma> tempLemmas = new HashMap<>();
        for(Site siteFromDb : siteRepository.findAll()) {
            if (!siteFromDb.getStatus().equals(Status.INDEXED)) {
                continue;
            }
            for (Lemma lemmaFromDB : lemmaRepository.findAll()){
                if(lemmaFromDB.getSiteId() == siteFromDb.getId() && !lemmaFrequencyIsOften(lemmaFromDB, allPageCount)){
                    tempLemmas.put(lemmaFromDB.getId(), lemmaFromDB);
                }
            }
        }
        return tempLemmas;
    }

    private boolean lemmaFrequencyIsOften(Lemma lemma, long allPageCount){
        if(percent >= 100){
            return false;
        }
        return lemma.getFrequency() > (allPageCount) - (allPageCount / 100.00) * (100 - percent);
    }

}
