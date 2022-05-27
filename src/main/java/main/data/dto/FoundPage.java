package main.data.dto;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class FoundPage {

    private Integer siteId;
    private String uri;
    private String title;
    private String snippet;
    private Float relevance;

}
