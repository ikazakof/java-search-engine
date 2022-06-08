package main.services;

import main.data.model.Index;
import main.data.model.Lemma;
import main.data.repository.LemmaRepository;


import java.util.TreeMap;

public class LemmasFrequencyReducer {
    public static void reduceLemmasFrequency(TreeMap<String, Lemma> existingLemmas, LemmaRepository lemmaRepository){
        existingLemmas.forEach((lemmaName, lemma) -> {
                lemma.decreaseFrequency();
                lemmaRepository.save(lemma);
                if(lemma.getFrequency() <= 0){
                    lemmaRepository.deleteById(lemma.getId());
                }
        });
    }
}
