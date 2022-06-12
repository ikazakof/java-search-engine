package main.services;

import main.data.dto.FoundPage;
import main.data.model.Index;
import main.data.model.Lemma;
import main.data.model.Page;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;

public class RelevantPageLoader {

    private List<Page> pagesFromDB;
    private List<Lemma> lemmasFromDB;
    private HashMap<Integer, List<Index>> foundedPages;
    private List<FoundPage> relevantPages;

    public RelevantPageLoader(List<Page> pagesFromDB, List<Lemma> lemmasFromDB, HashMap<Integer, List<Index>> foundedPages) {
        this.pagesFromDB = pagesFromDB;

        lemmasFromDB.sort(new LemmaSortByFreqAndName());
        this.lemmasFromDB = lemmasFromDB;
        this.foundedPages = foundedPages;

        this.relevantPages = getRelevantPagesList(getRelevant());
    }

    private float[][] getRelevant(){
        float[][] pagesRelevant = new float[pagesFromDB.size()][lemmasFromDB.size() + 3];
        float maxRelevant = 0;
        for(int row = 0; row < pagesFromDB.size(); row++){
            float absoluteRelevant = 0;
            pagesRelevant[row][0] = pagesFromDB.get(row).getId();
            for(int column = 1; column < lemmasFromDB.size() + 1; column++){
                pagesRelevant[row][column] = foundedPages.get(pagesFromDB.get(row).getId()).get(column - 1).getRank();
                absoluteRelevant += foundedPages.get(pagesFromDB.get(row).getId()).get(column - 1).getRank();
            }
            pagesRelevant[row][lemmasFromDB.size() + 1] = absoluteRelevant;
            maxRelevant = Math.max(maxRelevant, absoluteRelevant);
        }

        for(int row = 0; row < pagesFromDB.size(); row++){
            pagesRelevant[row][lemmasFromDB.size() + 2] = pagesRelevant[row][lemmasFromDB.size() + 1] / maxRelevant;
        }
        return pagesRelevant;
    }

    private ArrayList<FoundPage> getRelevantPagesList(float[][] pagesRelevant){
        ArrayList<FoundPage> relevantPages = new ArrayList<>();
        for(int row = 0; row < pagesFromDB.size(); row++){
            Page pageFromDB = pagesFromDB.get(row);
            FoundPage foundPage = new FoundPage();
            foundPage.setSiteId(pageFromDB.getSiteId());
            foundPage.setUri(pageFromDB.getPath());
            foundPage.setRelevance(pagesRelevant[row][pagesRelevant[0].length - 1]);
            foundPage.setTitle(getPageTitle(pageFromDB.getPageContent()));
            foundPage.setSnippet(getOccurSnippet(pageFromDB.getPageContent()));
            relevantPages.add(foundPage);
        }
        relevantPages.sort(new FoundPageSortByRelevanceUriAndSiteId());
        return relevantPages;

    }

    private String getPageTitle(String pageContent){
        Document doc = Jsoup.parse(pageContent);
        return doc.select("title").text().replaceAll("\"", "'");
    }

    private String getOccurSnippet(String pageContent){
        StringBuilder result = new StringBuilder();
        for (String textPart : cleanElement(pageContent).split("\n")){
            if(!textPart.matches(".*[А-я]+.*")){
                continue;
                }
                String tempTextPart = textPart;
                String[] words = textPart.split(" ");
                for(String part : words){
                    if (part.isEmpty() || !part.matches("[А-я]+")){
                        continue;
                    }
                    HashSet<String> normalForm = LemmFactory.getLemmsToRelevantPageLoader(part);
                    for(Lemma lemma : lemmasFromDB){
                        if(normalForm.contains(lemma.getLemma())){
                        tempTextPart = tempTextPart.replaceAll(part, "<b>" + part + "</b>").replaceAll("\"", "'");
                        break;
                        }
                    }
                }
                result.append((textPart.equals(tempTextPart)) ? "" : tempTextPart);
            }
        return result.toString();
    }

    private String cleanElement(String string){
        return string.replaceAll("<.*?>", "").replaceAll("\\s{2,}", "\n");

    }

    public List<FoundPage> getRelevantPages() {
        return relevantPages;
    }
}
