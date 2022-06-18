package main.services;

import main.data.model.Index;
import main.data.model.Lemma;
import main.data.repository.IndexRepository;

import java.util.*;


public class IndexLoader {

    public static HashMap<Integer, Index> loadIndexFromDB(int pageId, IndexRepository indexRepository){
        HashMap<Integer, Index> existingIndexes = new HashMap<>();
        for (Index indexFromDB : indexRepository.findAll()){
            if(indexFromDB.getPageId() == pageId){
                existingIndexes.put(indexFromDB.getLemmaId(), indexFromDB);
            }
        }
        return existingIndexes;
    }

    public static ArrayList<Index> loadIndexFromDBByPageIdAndLemmas(Collection<Integer> pagesId, IndexRepository indexRepository, Set<Integer> lemmasId){
        ArrayList<Index> existingIndexes = new ArrayList<>();
        for (Index indexFromDB : indexRepository.findAll()){
            if (pagesId.contains(indexFromDB.getPageId()) && lemmasId.contains(indexFromDB.getLemmaId())){
                existingIndexes.add(indexFromDB);
            }
        }
        return existingIndexes;
    }

    public static ArrayList<Index> loadIndexFromDBByLemmas(IndexRepository indexRepository, Set<Integer> lemmasId){
        ArrayList<Index> existingIndexes = new ArrayList<>();
        for (Index indexFromDB : indexRepository.findAll()){
            if(lemmasId.contains(indexFromDB.getLemmaId())){
                existingIndexes.add(indexFromDB);
            }
        }
        return existingIndexes;
    }

    public static ArrayList<Index> loadIndexFromListByLemmas(List<Index> indexList, List<Lemma> searchLemmas){
        ArrayList<Index> existingIndexes = new ArrayList<>();
        HashSet<Integer> searchLemmasId = new HashSet<>();
        searchLemmas.forEach(lemma -> searchLemmasId.add(lemma.getId()));

        for (Index indexFromDB : indexList){
            if(searchLemmasId.contains(indexFromDB.getLemmaId())){
                existingIndexes.add(indexFromDB);
            }
        }
        return existingIndexes;
    }

}
