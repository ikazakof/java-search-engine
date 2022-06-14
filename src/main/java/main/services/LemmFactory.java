package main.services;

import lombok.AllArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@AllArgsConstructor
public class LemmFactory {
    String[] wordsToLemmatize;

    protected List<String> getLemms(){
        List<String> tempLemms = new ArrayList<>();
        List<String> resultLemms = new ArrayList<>();
        try {
            LuceneMorphology luceneMorph = new RussianLuceneMorphology();
            for (String textPart : wordsToLemmatize) {
                if (textPart.length() == 0) {
                    continue;
                }
                tempLemms.addAll(luceneMorph.getMorphInfo(textPart));
                for (String lemm : tempLemms) {
                    if (servicePartExist(lemm)) {
                        continue;
                    }
                    resultLemms.add(getLemmWord(lemm));
                }
                tempLemms.clear();
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return resultLemms;
    }

    private boolean servicePartExist(String lemm){
        return lemm.matches(".*(СОЮЗ|МЕЖД|ПРЕДЛ|ЧАСТ).*");
    }

    private String getLemmWord(String lemm){
        return lemm.substring(0, lemm.indexOf("|"));
    }

    protected static HashMap<String, String> getLemmsToRelevantPageLoader(List<String> words){
        HashMap<String, String> resultLemmsAndBaseWords = new HashMap<>();
            try {
                LuceneMorphology luceneMorph = new RussianLuceneMorphology();
                words.forEach(word ->{
                    luceneMorph.getNormalForms(word.toLowerCase()).forEach(normalForm -> resultLemmsAndBaseWords.put(word , normalForm));
                });
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        return resultLemmsAndBaseWords;
        }
}
