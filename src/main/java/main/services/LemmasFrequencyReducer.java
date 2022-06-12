package main.services;

import main.data.model.Index;
import main.data.model.Lemma;
import main.data.repository.LemmaRepository;


import java.util.HashMap;


public class LemmasFrequencyReducer {
    public static void reduceLemmasFrequency(HashMap<String, Lemma> existingLemmas, LemmaRepository lemmaRepository){
        existingLemmas.forEach((lemmaName, lemma) -> {
                lemma.decreaseFrequency();
                lemmaRepository.save(lemma);
                if(lemma.getFrequency() <= 0){
                    lemmaRepository.deleteById(lemma.getId());
                }
        });
    }
}
