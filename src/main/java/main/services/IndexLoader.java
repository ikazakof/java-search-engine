package main.services;

import lombok.NoArgsConstructor;
import main.data.model.Index;
import main.data.model.Lemma;
import main.data.repository.IndexRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@NoArgsConstructor
public class IndexLoader {

    IndexRepository indexRepository;

    @Autowired
    public IndexLoader(IndexRepository indexRepository) {
        this.indexRepository = indexRepository;
    }

    public HashMap<Integer, Index> loadIndexFromDB(int pageId){
        HashMap<Integer, Index> existingIndexes = new HashMap<>();
        for (Index indexFromDB : indexRepository.findAll()){
            if(indexFromDB.getPageId() == pageId){
                existingIndexes.put(indexFromDB.getLemmaId(), indexFromDB);
            }
        }
        return existingIndexes;
    }

    public ArrayList<Index> loadIndexFromDBByPageIdAndLemmas(Collection<Integer> pagesId, Set<Integer> lemmasId){
        ArrayList<Index> existingIndexes = new ArrayList<>();
        for (Index indexFromDB : indexRepository.findAll()){
            if (pagesId.contains(indexFromDB.getPageId()) && lemmasId.contains(indexFromDB.getLemmaId())){
                existingIndexes.add(indexFromDB);
            }
        }
        return existingIndexes;
    }

    public ArrayList<Index> loadIndexFromDBByLemmas( Set<Integer> lemmasId){
        ArrayList<Index> existingIndexes = new ArrayList<>();
        for (Index indexFromDB : indexRepository.findAll()){
            if(lemmasId.contains(indexFromDB.getLemmaId())){
                existingIndexes.add(indexFromDB);
            }
        }
        return existingIndexes;
    }

    public ArrayList<Index> loadIndexFromListByLemmas(List<Index> indexList, List<Lemma> searchLemmas){
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
