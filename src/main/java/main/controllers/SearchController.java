package main.controllers;

import main.services.*;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
public class SearchController {

    final
    ResponseEntityLoader responseEntityLoader;
    final
    SearchServices searchServices;
    final
    SearchResultEntityOffseter searchResultEntityOffseter;
    @Autowired
    public SearchController(ResponseEntityLoader responseEntityLoader, SearchServices searchServices, SearchResultEntityOffseter searchResultEntityOffseter) {
        this.responseEntityLoader = responseEntityLoader;
        this.searchServices = searchServices;
        this.searchResultEntityOffseter = searchResultEntityOffseter;
    }

    @GetMapping("/search")
    public ResponseEntity<JSONObject> search(@RequestParam String query, @RequestParam(required = false)  String site, @RequestParam int offset, @RequestParam int limit) {
        if (query == null || query.isEmpty()){
            return responseEntityLoader.getEmptySearchQueryResponse();
        }

        if(site != null && !site.isEmpty()){
            ResponseEntity<JSONObject> siteResultJson = searchServices.getMatchesInSite(site, query);
            if(!Objects.requireNonNull(siteResultJson.getBody()).containsKey("data")){
                return siteResultJson;
            } else {
                return searchResultEntityOffseter.loadEntityWithOffset(limit, offset, siteResultJson);
            }
        }
        ResponseEntity<JSONObject> sitesResultJson = searchServices.getMatchesInSites(query);
        if(!Objects.requireNonNull(sitesResultJson.getBody()).containsKey("data")){
            return sitesResultJson;
        } else {
            return searchResultEntityOffseter.loadEntityWithOffset(limit, offset, sitesResultJson);
        }
    }
}
