package main.services;

import main.data.model.Lemma;
import java.util.Comparator;

public class LemmaSortByFreqAndName implements Comparator<Lemma> {


    @Override
    public int compare(Lemma o1, Lemma o2) {
        int compareName = o1.getFrequency() - o2.getFrequency();
        if(compareName != 0){
            return compareName;
        }
        return o1.getLemma().compareTo(o2.getLemma()) ;
    }
}
