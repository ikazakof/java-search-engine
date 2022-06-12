package main.services;

import main.data.dto.FoundPage;
import main.data.model.Site;
import main.data.repository.SiteRepository;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;


public class SearchResultEntityLoader {

    HashMap<Integer, Site> sites;

    public SearchResultEntityLoader(SiteRepository siteRepository) {
        this.sites = getSitesFromDB(siteRepository);
    }

    private HashMap<Integer, Site> getSitesFromDB(SiteRepository siteRepository){
        HashMap<Integer, Site> resultSites = new HashMap<>();
        siteRepository.findAll().forEach(site -> resultSites.put(site.getId(), site));
        return resultSites;
    }

    public ResponseEntity<JSONObject> getSearchResultJson(int limit, int offset, List<FoundPage> foundPages){
        int pageLimit = (limit == 0 ? 20 + offset : limit + offset);
        if(pageLimit > foundPages.size()){
            pageLimit = foundPages.size();
        }
        if(offset > foundPages.size()){
            offset = 0;
        }
        StringBuilder result = new StringBuilder();
        result.append("{\n\"result\": true,\n \"count\": ").append(foundPages.size()).append(",\n \"data\": [\n");
        for (; offset < pageLimit; offset++){
            result.append("{\n \"site\": \"").append(this.sites.get(foundPages.get(offset).getSiteId()).getUrl()).append("\",\n");
            result.append("\"siteName\": \"").append(this.sites.get(foundPages.get(offset).getSiteId()).getName()).append("\",\n");
            result.append("\"uri\": \"").append(foundPages.get(offset).getUri()).append("\",\n");
            result.append("\"title\": \"").append(foundPages.get(offset).getTitle()).append("\",\n");
            result.append("\"snippet\": \"").append(foundPages.get(offset).getSnippet()).append("\",\n");
            result.append("\"relevance\": \"").append(foundPages.get(offset).getRelevance()).append("\"\n}");
            if(offset != pageLimit - 1){
                result.append(",\n");
            }
        }
        result.append("\n]\n}");

        JSONParser parser = new JSONParser();
        JSONObject resultJson = new JSONObject();

        try {
            resultJson = (JSONObject) parser.parse(result.toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new ResponseEntity<>(resultJson,HttpStatus.OK);
    }
}
