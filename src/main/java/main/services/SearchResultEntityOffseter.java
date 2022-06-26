package main.services;

import lombok.NoArgsConstructor;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@NoArgsConstructor
public class SearchResultEntityOffseter {

    public ResponseEntity<JSONObject> loadEntityWithOffset(int limit, int offset, ResponseEntity<JSONObject> result){
        int foundPagesSize = Integer.parseInt(Objects.requireNonNull(result.getBody()).get("count").toString());
        int pageLimit = (limit == 0 ? 20 + offset : limit + offset);
        if(pageLimit > foundPagesSize){
            pageLimit = foundPagesSize;
        }
        if(offset > foundPagesSize){
            offset = 0;
        }
        StringBuilder offsetResult = new StringBuilder();
        offsetResult.append("{\n\"result\": true,\n \"count\" : ");
        offsetResult.append(result.getBody().get("count").toString());
        offsetResult.append(",\n \"data\": [\n");
        String[] tempData = result.getBody().get("data").toString().replaceAll("]", "").replaceAll("\\[", "").split("},\\{");
        for (; offset < pageLimit; offset++){
            offsetResult.append("{\n ");
            offsetResult.append(tempData[offset].replaceAll("\\{", "").replaceAll("}", ""));
            offsetResult.append("\n}");
            if(offset != pageLimit - 1){
                offsetResult.append(",");
            }
        }
        offsetResult.append("\n]\n}");
        JSONParser parser = new JSONParser();
        JSONObject resultJson = new JSONObject();
        try {
            resultJson = (JSONObject) parser.parse(offsetResult.toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new ResponseEntity<>(resultJson, HttpStatus.OK);
    }

}
