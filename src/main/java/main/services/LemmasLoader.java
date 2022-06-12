package main.services;

import main.data.model.Index;
import main.data.model.Lemma;
import main.data.model.Site;
import main.data.model.Status;
import main.data.repository.LemmaRepository;
import main.data.repository.SiteRepository;

import java.util.ArrayList;
import java.util.HashMap;

public class LemmasLoader {

public static HashMap<String, Lemma> loadSiteLemmasFromDB(int siteId, LemmaRepository lemmaRepository){
    HashMap<String, Lemma> tempLemmas = new HashMap<>();
    lemmaRepository.findAll().forEach(lemma -> {
        if(lemma.getSiteId() == siteId) {
            tempLemmas.put(lemma.getLemma(), lemma);
        }
    });
    return tempLemmas;
}

public static HashMap<String, Lemma> loadLemmasFromDBWithIndex(HashMap<Integer, Index> existingIndexes, LemmaRepository lemmaRepository){
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

    public static HashMap<Integer, Lemma> loadSiteLemmasFromDBWithFreq(int siteId, LemmaRepository lemmaRepository, long allPageCount) {
        HashMap<Integer, Lemma> tempLemmas = new HashMap<>();
        lemmaRepository.findAll().forEach(lemma -> {
            if (lemma.getSiteId() == siteId && !lemmaFrequencyIsOften(lemma, allPageCount)) {
                tempLemmas.put(lemma.getId(), lemma);
            }
        });
        return tempLemmas;
    }

    public static HashMap<Integer, Lemma> loadLemmasFromDBWithFreqAndIndexedSites(SiteRepository siteRepository, LemmaRepository lemmaRepository, long allPageCount) {
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

    private static boolean lemmaFrequencyIsOften(Lemma lemma, long allPageCount){
        return lemma.getFrequency() >= (allPageCount) - (allPageCount / 100) * 60;
    }

}
