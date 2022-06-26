package main.services;

import lombok.NoArgsConstructor;
import main.data.model.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
@NoArgsConstructor
public class ResultPageLoader {

    public List<Page> getCorrectlyResponsivePages(Collection<Page> resultPages){
        List<Page> resultList = new ArrayList<>();
        resultPages.forEach(page -> {
            if(!(page.getAnswerCode() >= 400 && page.getAnswerCode() <= 417) && !(page.getAnswerCode() >= 500 && page.getAnswerCode() <= 505)){
                resultList.add(page);
            }
        });
        return resultList;
    }

    public Page getCorrectlyResponsivePage(Page page){
        if(!(page.getAnswerCode() >= 400 && page.getAnswerCode() <= 417) && !(page.getAnswerCode() >= 500 && page.getAnswerCode() <= 505)){
            return page;
        }
        return new Page();
    }
}
