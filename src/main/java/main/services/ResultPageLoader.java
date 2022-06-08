package main.services;

import main.data.model.Page;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ResultPageLoader {

    public static List<Page> getCorrectlyResponsivePages(Collection<Page> resultPages){
        List<Page> resultList = new ArrayList<>();
        resultPages.forEach(page -> {
            if(!(page.getAnswerCode() >= 400 && page.getAnswerCode() <= 417) && !(page.getAnswerCode() >= 500 && page.getAnswerCode() <= 505)){
                resultList.add(page);
            }
        });
        return resultList;
    }

    public static Page getCorrectlyResponsivePage(Page page){
        if(!(page.getAnswerCode() >= 400 && page.getAnswerCode() <= 417) && !(page.getAnswerCode() >= 500 && page.getAnswerCode() <= 505)){
            return page;
        }
        return new Page();
    }
}
