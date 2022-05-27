package main.services;

import main.data.dto.FoundPage;

import java.util.Comparator;

public class FoundPageSortByRelevanceAndUri implements Comparator<FoundPage> {

    @Override
    public int compare(FoundPage f1, FoundPage f2){
        int compareRel = Float.compare(f2.getRelevance(), f1.getRelevance());
        if(compareRel != 0){
            return compareRel;
        }
        return  f1.getUri().compareTo(f2.getUri());
    }

}
