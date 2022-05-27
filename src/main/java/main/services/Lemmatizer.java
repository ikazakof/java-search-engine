package main.services;

import main.data.model.Field;
import main.data.model.Lemma;
import main.data.model.Page;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveTask;

public class Lemmatizer extends RecursiveTask<TreeMap<Integer, TreeMap<Lemma, Float>>>{

        private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();

        private List<Page> pagesToLemmatize;

        private List<Field> fieldsToLemmatize;

        private int threshold;

        private int siteId;

        public Lemmatizer(List<Page> pagesToLemmatize,List<Field> fieldsToLemmatize, int siteId){
            this.pagesToLemmatize = pagesToLemmatize;
            this.fieldsToLemmatize = fieldsToLemmatize;
            this.siteId = siteId;
        }

        public Lemmatizer(List<Page> pagesToLemmatize, List<Field> fieldsToLemmatize, int threshold, int siteId ){
            this.pagesToLemmatize = pagesToLemmatize;
            this.fieldsToLemmatize = fieldsToLemmatize;
            this.threshold = threshold;
            this.siteId = siteId;
        }

        public TreeMap<Integer, TreeMap<Lemma, Float>> compute(){
            TreeMap<Integer, TreeMap<Lemma, Float>> result = new TreeMap<>();
            List<Lemmatizer> tasks = new ArrayList<>();

            if(threshold == 0 && pagesToLemmatize.size() < THREAD_COUNT){
               threshold = 1;
            } else if(threshold == 0 && pagesToLemmatize.size() > THREAD_COUNT){
                threshold = pagesToLemmatize.size() / THREAD_COUNT;
            }

            if(pagesToLemmatize.size() <= threshold){
                pagesToLemmatize.forEach((page) ->
                        result.put(page.getId(), getLemmasAndRank(page)));
            } else {
                if (pagesToLemmatize.size() % threshold == 0) {
                    for (int partCounter = 0; partCounter != pagesToLemmatize.size(); partCounter += threshold) {
                        Lemmatizer task = new Lemmatizer(pagesToLemmatize.subList(partCounter, partCounter + threshold), fieldsToLemmatize, threshold, siteId);
                        task.fork();
                        tasks.add(task);
                    }
                } else {
                    for (int partCounter = 0; partCounter != pagesToLemmatize.size() - pagesToLemmatize.size() % threshold; partCounter += threshold) {
                        Lemmatizer task = new Lemmatizer(pagesToLemmatize.subList(partCounter, partCounter + threshold), fieldsToLemmatize, threshold, siteId);
                        task.fork();
                        tasks.add(task);
                    }
                    Lemmatizer task = new Lemmatizer(pagesToLemmatize.subList(pagesToLemmatize.size() - pagesToLemmatize.size() % threshold, pagesToLemmatize.size()), fieldsToLemmatize, threshold, siteId);
                    task.fork();
                    tasks.add(task);
                }

            }
            addResultFromTasks(result, tasks);
            return result;
        }



        private TreeMap<Lemma, Float> getLemmasAndRank(Page page){
            TreeMap<Lemma, Float> result = new TreeMap<>();
                fieldsToLemmatize.forEach(field ->{
                    float fieldWeight = field.getWeight();
                    String fieldString = getFieldFromPage(page, field);
                    String[] words = fieldString.split(" ");

                    List<String> tempLemms = new ArrayList<>();
                    try {
                        LuceneMorphology luceneMorph = new RussianLuceneMorphology();
                        for (String textPart : words) {
                            if (textPart.length() == 0){
                                continue;
                            }
                                tempLemms.addAll(luceneMorph.getMorphInfo(textPart));
                            for (String lemm : tempLemms) {
                                if (servicePartExist(lemm)) {
                                    continue;
                                }
                                if (!result.containsKey(new Lemma(getLemmWord(lemm), siteId))) {
                                    result.put(new Lemma(getLemmWord(lemm), 1, siteId), fieldWeight);
                                    continue;
                                }
                                result.forEach((lemma, rank) -> {
                                    if(lemma.compareTo(new Lemma(getLemmWord(lemm), siteId)) == 0){
                                        result.put(lemma, rank + fieldWeight);
                                    }
                                });
                            }
                            tempLemms.clear();
                        }
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                });
           return result;
        }

        private boolean servicePartExist(String lemm){
            return lemm.matches(".*(СОЮЗ|МЕЖД|ПРЕДЛ|ЧАСТ).*");
        }

        private String getLemmWord(String lemm){
            return lemm.substring(0, lemm.indexOf("|"));
        }


        private String getFieldFromPage(Page page, Field field){
            String result = "";
            for(Element element : Jsoup.parse(page.getPageContent()).select(field.getSelector())){
              result = cleanElement(element);
            }
            return result;
        }

        private String cleanElement(Element element){
            return element.toString().replaceAll("\n", "").replaceAll("[^А-я\\s]"," ").replaceAll("\\s{2,}", " ").strip().toLowerCase();
        }

        private void addResultFromTasks(TreeMap<Integer, TreeMap<Lemma, Float>> result, List<Lemmatizer> tasks){
            tasks.forEach(task -> result.putAll(task.join()));
        }

    }



