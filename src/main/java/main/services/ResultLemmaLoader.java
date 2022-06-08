package main.services;

import lombok.Getter;
import main.data.model.Lemma;

import java.util.*;

public class ResultLemmaLoader {

    @Getter
    HashMap<String, Lemma> lemmaResultToDB;

    public ResultLemmaLoader(Collection<TreeMap<Lemma, Float>> lemmasResult) {
        this.lemmaResultToDB = setResultToDB(lemmasResult);
    }


    private HashMap<String, Lemma> setResultToDB(Collection<TreeMap<Lemma, Float>> tempTreeMap){
        HashMap<String, Lemma> tempLemmasHash = new HashMap<>();
        tempTreeMap.forEach(collect -> collect.forEach((lemma, rank) -> {
            if(tempLemmasHash.containsKey(lemma.getLemma())){
                tempLemmasHash.get(lemma.getLemma()).increaseFrequency();
            } else {
                tempLemmasHash.put(lemma.getLemma(), lemma);
            }
        }));
        return tempLemmasHash;
    }


}
