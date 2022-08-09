package main.services;

import lombok.AllArgsConstructor;
import main.data.model.Field;
import main.data.model.Lemma;
import main.data.model.Page;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.util.*;
import java.util.concurrent.RecursiveTask;

@AllArgsConstructor
public class Lemmatizer extends RecursiveTask<TreeMap<Integer, TreeMap<Lemma, Float>>>{

        private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();

        private List<Page> pagesToLemmatize;

        private Iterable<Field> fieldsToLemmatize;

        private int threshold;

        private int siteId;

        public Lemmatizer(List<Page> pagesToLemmatize,Iterable<Field> fieldsToLemmatize, int siteId){
            this.pagesToLemmatize = pagesToLemmatize;
            this.fieldsToLemmatize = fieldsToLemmatize;
            this.siteId = siteId;
        }


        public TreeMap<Integer, TreeMap<Lemma, Float>> compute(){
            TreeMap<Integer, TreeMap<Lemma, Float>> result = new TreeMap<>();
            List<Lemmatizer> tasks = new ArrayList<>();
            threshold = 1;
            if(pagesToLemmatize.size() > AVAILABLE_PROCESSORS){
                threshold = pagesToLemmatize.size() / AVAILABLE_PROCESSORS;
            }

            if(pagesToLemmatize.size() <= threshold){
                pagesToLemmatize.forEach((page) -> result.put(page.getId(), getLemmasAndRank(page)));
            } else {
                int limit = 0;
                if (pagesToLemmatize.size() % threshold == 0) {
                    limit = pagesToLemmatize.size();
                }
                for (int partCounter = 0; partCounter != limit; partCounter += threshold) {
                    Lemmatizer task = new Lemmatizer(pagesToLemmatize.subList(partCounter, partCounter + threshold), fieldsToLemmatize, threshold, siteId);
                    task.fork();
                    tasks.add(task);
                }
                if(limit != 0){
                    addResultFromTasks(result, tasks);
                    return result;
                }
                for (int partCounter = 0; partCounter != pagesToLemmatize.size() - pagesToLemmatize.size() % threshold; partCounter += threshold) {
                    Lemmatizer task = new Lemmatizer(pagesToLemmatize.subList(partCounter, partCounter + threshold), fieldsToLemmatize, threshold, siteId);
                    task.fork();
                    tasks.add(task);
                }
                Lemmatizer task = new Lemmatizer(pagesToLemmatize.subList(pagesToLemmatize.size() - pagesToLemmatize.size() % threshold, pagesToLemmatize.size()), fieldsToLemmatize, threshold, siteId);
                task.fork();
                tasks.add(task);
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

                            LemmFactory lemmFactory = new LemmFactory(words);
                            for(String lemm : lemmFactory.getLemms()){
                                if (!result.containsKey(new Lemma(lemm, siteId))) {
                                    result.put(new Lemma(lemm, 1, siteId), fieldWeight);
                                    continue;
                                }
                                result.forEach((lemma, rank) -> {
                                    if(lemma.compareTo(new Lemma(lemm, siteId)) == 0){
                                    result.put(lemma, rank + fieldWeight);
                                    }
                                });
                            }
                });
           return result;
        }

        private String getFieldFromPage(Page page, Field field){
            String result = "";
            for(Element element : Jsoup.parse(page.getPageContent()).select(field.getSelector())){
              result = cleanElement(element);
            }
            return result;
        }

        private String cleanElement(Element element){
            return element.toString().replaceAll("\n", "").replaceAll("\r", " ").replaceAll("[^А-я\\s]"," ").replaceAll("\\s{2,}", " ").toLowerCase().strip();
        }

        private void addResultFromTasks(TreeMap<Integer, TreeMap<Lemma, Float>> result, List<Lemmatizer> tasks){
            tasks.forEach(task -> result.putAll(task.join()));
        }

    }



