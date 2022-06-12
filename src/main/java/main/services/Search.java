package main.services;

import main.data.model.Index;
import main.data.model.Lemma;
import main.services.LemmaSortByFreqAndName;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.*;

public class Search {

    private String searchPhrase;
    private HashMap<String, Lemma> lemmasFromDB;
    private List<Index> indexFromDB;
    private List<Lemma> searchLemmas;
    private HashMap<Integer, List<Index>> foundPages;


    public Search(String searchPhrase, Collection<Lemma> lemmasFromDB, ArrayList<Index> indexFromDB) {
        this.searchPhrase = searchPhrase;
        this.lemmasFromDB = new HashMap<>();
        lemmasFromDB.forEach(lemma -> this.lemmasFromDB.put(lemma.getLemma(), lemma));
        this.indexFromDB = indexFromDB;

        this.searchLemmas = separateLemmas();
        this.foundPages = searchRelevantPages(searchLemmas);
    }

    public HashMap<Integer, List<Index>> getFoundPages() {
        return foundPages;
    }

    public List<Lemma> getSearchLemmas(){
        return searchLemmas;
    }

    private List<Lemma> separateLemmas(){
        String[] phraseWords = cleanSearchPhrase(searchPhrase).split(" ");

        List<Lemma> lemmaForSearch = new ArrayList<>();

        new LemmFactory(phraseWords).getLemms().forEach(tempLemma ->
                lemmaForSearch.add(lemmasFromDB.get(tempLemma)));

        lemmaForSearch.sort(new LemmaSortByFreqAndName());
        return lemmaForSearch;
    }

    private String cleanSearchPhrase(String searchPhrase){
        return searchPhrase.replaceAll("\n", "").replaceAll("[^А-я\\s]","").replaceAll("\\s{2,}", " ").strip().toLowerCase();
    }

    private HashMap<Integer, List<Index>> searchRelevantPages(List<Lemma> searchLemmas){
        if(searchLemmas.isEmpty()){
            return new HashMap<>();
        }
        ArrayList<Index> foundIndexes = new ArrayList<>();
        HashMap<Integer, List<Index>> tempRelevantPages = new HashMap<>();

        foundIndexes.addAll(IndexLoader.loadIndexFromListByLemmas(indexFromDB, searchLemmas));

        if(foundIndexes.isEmpty()){
            return new HashMap<>();
        }
        for(Index foundIndex : foundIndexes) {
        if(tempRelevantPages.containsKey(foundIndex.getPageId())){
            tempRelevantPages.get(foundIndex.getPageId()).add(foundIndex);
            continue;
        }
        ArrayList<Index> tempIndex = new ArrayList<>();
        tempIndex.add(foundIndex);
        tempRelevantPages.put(foundIndex.getPageId(),tempIndex);
        }
        HashMap<Integer, List<Index>> resultPages = new HashMap<>();
        tempRelevantPages.forEach((page, indexes) ->{
            if(indexes.size() == searchLemmas.size()){
                resultPages.put(page, indexes);
            }
        });
        return resultPages;
    }

}
