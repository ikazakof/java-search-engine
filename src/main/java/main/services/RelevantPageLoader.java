package main.services;

import main.data.dto.FoundPage;
import main.data.model.Index;
import main.data.model.Lemma;
import main.data.model.Page;
import main.services.FoundPageSortByRelevanceAndUri;
import main.services.LemmaSortByFreqAndName;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class RelevantPageLoader {

    private List<Page> pagesFromDB;
    private List<Lemma> lemmasFromDB;
    private TreeMap<Integer, List<Index>> foundedPages;
    private List<FoundPage> relevantPages;

    public RelevantPageLoader(List<Page> pagesFromDB, List<Lemma> lemmasFromDB, TreeMap<Integer, List<Index>> foundedPages) {
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
        relevantPages.sort(new FoundPageSortByRelevanceAndUri());
        return relevantPages;

    }

    private String getPageTitle(String pageContent){
        Document doc = Jsoup.parse(pageContent);
        return doc.select("title").text().replaceAll("\"", "'");
    }

    private String getOccurSnippet(String pageContent){
        StringBuilder result = new StringBuilder();
        int searchWordEntry = 0;
        try {
            LuceneMorphology luceneMorphology = new RussianLuceneMorphology();
            for (String textPart : pageContent.split("\n")){
                String tempTextPart = textPart.replaceAll("<.*?>", "");
                String[] words = cleanElement(textPart).split(" ");
                for(String part : words){
                    if (part.isEmpty() || !part.matches("[А-я]*")){
                        continue;
                    }
                    List<String> getNormalForm = luceneMorphology.getNormalForms(part.toLowerCase());
                    for(Lemma lemma : lemmasFromDB){
                        if(getNormalForm.contains(lemma.getLemma())){
                           tempTextPart = tempTextPart.replaceAll(part, "<b>" + part + "</b>").replaceAll("\"", "'");
                           searchWordEntry++;
                        }
                    }
                }

                result.append((textPart.replaceAll("<.*?>", "").equals(tempTextPart)) ? "" : tempTextPart.replaceAll("(<b>){2,}", "<b>").replaceAll("(</b>){2,}", "</b>"));

                if (searchWordEntry == lemmasFromDB.size() * 3){
                    break;
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return result.toString();
    }

    private String cleanElement(String string){
        return string.replaceAll("<.*?>" ,"").replaceAll("[^А-я\\sA-z]","").replaceAll("\\s{2,}", " ").strip();
    }

    public List<FoundPage> getRelevantPages() {
        return relevantPages;
    }
}
