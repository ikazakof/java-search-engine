package main.services;

import main.data.model.Index;
import main.data.model.Lemma;
import main.data.model.Site;
import main.data.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Indexer {

    public static ArrayList<Index> getIndexes(TreeMap<Integer, TreeMap<Lemma, Float>> lemmasResult, HashMap<String, Lemma> lemmasResultToDB, SiteRepository siteRepository){
        ArrayList<Index> result = new ArrayList<>();
        Site targetSite;
        for(Map.Entry<Integer, TreeMap<Lemma, Float>> page : lemmasResult.entrySet()){
            for (Map.Entry<Lemma, Float> entry : page.getValue().entrySet()){
                result.add(new Index(page.getKey(), lemmasResultToDB.get(entry.getKey().getLemma()).getId(), entry.getValue()));
                targetSite = siteRepository.findById(entry.getKey().getSiteId()).get();
                targetSite.setStatusTime(LocalDateTime.now());
                siteRepository.save(targetSite);
            }
        }
        return result;
    }

}
