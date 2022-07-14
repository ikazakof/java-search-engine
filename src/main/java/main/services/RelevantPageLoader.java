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
    private HashMap<String, Lemma> lemmasHashFromDB;

    private int searchedSites;
    private HashMap<Integer, List<Index>> foundedPages;
    private List<FoundPage> relevantPages;

    public RelevantPageLoader(List<Page> pagesFromDB, List<Lemma> lemmasFromDB, HashMap<Integer, List<Index>> foundedPages) {
        this.pagesFromDB = pagesFromDB;

        lemmasFromDB.sort(new LemmaSortByFreqAndName());
        lemmasHashFromDB = new HashMap<>();
        lemmasFromDB.forEach(lemma -> this.lemmasHashFromDB.put(lemma.getLemma(), lemma));
        this.foundedPages = foundedPages;
        HashSet<Integer> searchSites = new HashSet<>();
        lemmasFromDB.forEach(lemma -> searchSites.add(lemma.getSiteId()));
        this.searchedSites = searchSites.size();
        this.relevantPages = getRelevantPagesList(getRelevant());
    }

    private float[][] getRelevant(){
        int lemmOnPageCounter =  lemmasHashFromDB.size();
        float[][] pagesRelevant = new float[pagesFromDB.size()][lemmOnPageCounter + 3];
        float maxRelevant = 0;
        for(int row = 0; row < pagesFromDB.size(); row++){
            float absoluteRelevant = 0;
            pagesRelevant[row][0] = pagesFromDB.get(row).getId();
            for(int column = 1; column < lemmOnPageCounter + 1; column++){
                pagesRelevant[row][column] = foundedPages.get(pagesFromDB.get(row).getId()).get(column - 1).getRank();
                absoluteRelevant += foundedPages.get(pagesFromDB.get(row).getId()).get(column - 1).getRank();
            }
            pagesRelevant[row][lemmOnPageCounter + 1] = absoluteRelevant;
            maxRelevant = Math.max(maxRelevant, absoluteRelevant);
        }

        for(int row = 0; row < pagesFromDB.size(); row++){
            pagesRelevant[row][lemmOnPageCounter + 2] = pagesRelevant[row][lemmOnPageCounter + 1] / maxRelevant;
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
        ArrayList<String> cleanSplittedContent = cleanElements(pageContent.split("\n"));

        List<String> wordsList = new ArrayList<>();
        for(String textPart : cleanSplittedContent){
            for(String part : textPart.split(" ")){
                if (part.isEmpty() || !part.matches("[А-я]+")){
                    continue;
                }
                wordsList.add(part);
            }
        }
        HashMap<String, String> normalForm;
        LemmFactory lemmFactory = new LemmFactory(wordsList.toArray(String[]::new));
        normalForm = lemmFactory.getLemmsToRelevantPageLoader();

        for (String textPart : cleanSplittedContent){
            String tempTextPart = textPart;
            for(Map.Entry<String, String> wordFromCleanPage : normalForm.entrySet()){
                if (lemmasHashFromDB.containsKey(wordFromCleanPage.getValue()) && textPart.contains(wordFromCleanPage.getKey())){
                    tempTextPart = tempTextPart.replaceAll( "\\b" + wordFromCleanPage.getKey() + "\\b", "<b>" + wordFromCleanPage.getKey() + "</b>").replaceAll("\"", "'") + " ";
                }
            }
            result.append((textPart.equals(tempTextPart)) ? "" : tempTextPart);
            }
        return result.toString();
    }

    private ArrayList<String> cleanElements(String[] elements){
        ArrayList<String> cleanElementsList = new ArrayList<>();
        for(String element : elements){
            String tempElement = element.replaceAll("&nbsp;", " ").replaceAll("(<.*?>|<!--.*?-->|/\\*.*?\\*/|[^А-я\\sA-z0-9.,!:;])", "").replaceAll("\\s{2,}", " ").strip();
            if(tempElement.matches(".*[А-я]+.*")){
                cleanElementsList.add(tempElement);
            }
        }
        return cleanElementsList;
    }

    public List<FoundPage> getRelevantPages() {
        return relevantPages;
    }
}
