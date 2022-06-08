package main.services;

import main.data.model.Index;
import main.data.model.Lemma;
import main.data.repository.LemmaRepository;

import java.util.ArrayList;
import java.util.TreeMap;

public class LemmasLoader {

public static TreeMap<String, Lemma> loadLemmasFromDB(int siteId, LemmaRepository lemmaRepository){
    TreeMap<String, Lemma> tempLemmas = new TreeMap<>();
    lemmaRepository.findAll().forEach(lemma -> {
        if(lemma.getSiteId() == siteId) {
            tempLemmas.put(lemma.getLemma(), lemma);
        }
    });
    return tempLemmas;
}

public static TreeMap<String, Lemma> loadLemmasFromDBWithIndex(TreeMap<Integer, Index> existingIndexes, LemmaRepository lemmaRepository){
    TreeMap<String, Lemma> tempLemmas = new TreeMap<>();
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

}
