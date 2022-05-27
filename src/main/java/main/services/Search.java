package main.services;

import main.data.model.Index;
import main.data.model.Lemma;
import main.services.LemmaSortByFreqAndName;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

public class Search {

    private String searchPhrase;
    private List<Lemma> lemmasFromDB;
    private List<Index> indexFromDB;
    private List<Lemma> searchLemmas;
    private TreeMap<Integer, List<Index>> foundPages;


    public Search(String searchPhrase, Collection<Lemma> lemmasFromDB, ArrayList<Index> indexFromDB) {
        this.searchPhrase = searchPhrase;
        this.lemmasFromDB = new ArrayList<>();
        this.lemmasFromDB.addAll(lemmasFromDB);
        this.indexFromDB = indexFromDB;

        this.searchLemmas = separateLemmas();
        this.foundPages = searchRelevantPages(searchLemmas);
    }

    public TreeMap<Integer, List<Index>> getFoundPages() {
        return foundPages;
    }

    private List<Lemma> separateLemmas(){
        String[] phraseWords = cleanSearchPhrase(searchPhrase).split(" ");
        List<String> tempLemms = new ArrayList<>();
        try {
            LuceneMorphology luceneMorph = new RussianLuceneMorphology();
            for (String textPart : phraseWords) {
                if(textPart.length() == 0){
                    continue;
                }
               for(String lemma : luceneMorph.getMorphInfo(textPart)){
                   if (!servicePartExist(lemma)){
                       tempLemms.add(getLemmWord(lemma));
                   }
               }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        List<Lemma> lemmaForSearch = new ArrayList<>();

        tempLemms.forEach(tempLemma ->
            lemmasFromDB.forEach(lemma -> {
                if(lemma.getLemma().compareTo(tempLemma) == 0){
                    lemmaForSearch.add(lemma);
                }})
        );
        lemmaForSearch.sort(new LemmaSortByFreqAndName());
        return lemmaForSearch;
    }


    private String cleanSearchPhrase(String searchPhrase){
        return searchPhrase.replaceAll("\n", "").replaceAll("[^А-я\\s]","").replaceAll("\\s{2,}", " ").strip().toLowerCase();
    }

    private boolean servicePartExist(String lemm){
        return lemm.matches(".*(СОЮЗ|МЕЖД|ПРЕДЛ|ЧАСТ).*");
    }

    private String getLemmWord(String lemm){
        return lemm.substring(0, lemm.indexOf("|"));
    }

    private TreeMap<Integer, List<Index>> searchRelevantPages(List<Lemma> searchLemmas){
        ArrayList<Index> foundIndexes = new ArrayList<>();
        ArrayList<Index> tempIndexes = new ArrayList<>();
        TreeMap<Integer, List<Index>> tempRelevantPages = new TreeMap<>();
        int countSearchLemmas = searchLemmas.size();

        if(searchLemmas.isEmpty()){
            return new TreeMap<>();
        }

        for(Index index : indexFromDB) {
            if (index.getLemmaId() != searchLemmas.get(0).getId()) {
                continue;
            }
            for(Index tempPageIndex : indexFromDB){
                if(tempPageIndex.getPageId() == index.getPageId()){
                    tempIndexes.add(tempPageIndex);
                }
            }
        }

        if(countSearchLemmas == 1){
            foundIndexes.addAll(tempIndexes);
        }

        for(int counter = 1; counter < countSearchLemmas; counter++ ){
            if(tempIndexes.size() == 0){
                foundIndexes.clear();
                break;
            }

            for(Index tempIndex : tempIndexes){
                if(tempIndex.getLemmaId() != searchLemmas.get(counter).getId()){
                    continue;
                }
                for(Index tempPageIndex : tempIndexes){
                    if(tempPageIndex.getPageId() == tempIndex.getPageId()){
                        foundIndexes.add(tempPageIndex);
                    }
                }
            }

            if(counter != countSearchLemmas - 1) {
                tempIndexes.clear();
                tempIndexes.addAll(foundIndexes);
                foundIndexes.clear();
            }
        }
        tempIndexes.clear();

        if(foundIndexes.isEmpty()){
            return new TreeMap<>();
        }

        for(Index foundIndex : foundIndexes){
                for(Lemma searchLemma : searchLemmas) {
                    if(foundIndex.getLemmaId() != searchLemma.getId()){
                        continue;
                    }
                    List<Index> tempIndexList = new ArrayList<>();
                    tempIndexList.addAll(tempRelevantPages.getOrDefault(foundIndex.getPageId(), new ArrayList<>()));
                    tempIndexList.add(foundIndex);
                    tempRelevantPages.put(foundIndex.getPageId(), tempIndexList);
                }
        }
        return tempRelevantPages;
    }

}
