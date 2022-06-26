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

    public ResponseEntity<JSONObject> getSearchResultJson(List<FoundPage> foundPages){
        StringBuilder result = new StringBuilder();
        result.append("{\n\"result\": true,\n \"count\": ").append(foundPages.size()).append(",\n \"data\": [\n");
        for (int counter = 0; counter < foundPages.size(); counter++){
            result.append("{\n \"site\": \"").append(this.sites.get(foundPages.get(counter).getSiteId()).getUrl()).append("\",\n");
            result.append("\"siteName\": \"").append(this.sites.get(foundPages.get(counter).getSiteId()).getName()).append("\",\n");
            result.append("\"uri\": \"").append(foundPages.get(counter).getUri()).append("\",\n");
            result.append("\"title\": \"").append(foundPages.get(counter).getTitle()).append("\",\n");
            result.append("\"snippet\": \"").append(foundPages.get(counter).getSnippet()).append("\",\n");
            result.append("\"relevance\": \"").append(foundPages.get(counter).getRelevance()).append("\"\n}");
            if(counter != foundPages.size() - 1){
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
