package main.services;

import lombok.Getter;
import main.data.model.Index;
import main.data.repository.IndexRepository;

import java.util.TreeMap;

public class IndexLoader {
    @Getter
    TreeMap<Integer, Index> existingIndexes;

    public static TreeMap<Integer, Index> loadIndexFromDB(int pageId, IndexRepository indexRepository){
        TreeMap<Integer, Index> existingIndexes = new TreeMap<>();
        for (Index indexFromDB : indexRepository.findAll()){
            if(indexFromDB.getPageId() == pageId){
                existingIndexes.put(indexFromDB.getLemmaId(), indexFromDB);
            }
        }
        return existingIndexes;
    }
}
