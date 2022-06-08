package main.data.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter

@Entity
@Table(name = "page")
public class Page implements Serializable, Comparable<Page> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String path;
    @Column(name = "code", nullable = false )
    private int answerCode;
    @Column(name = "content", columnDefinition = "MEDIUMTEXT", nullable = false)
    private String pageContent;
    @Column(name = "site_id", nullable = false)
    private int siteId;


    public Page(String path, int siteId){
        this.path = path;
        this.siteId = siteId;
    }

    public Page(String path , int answerCode, String pageContent, int siteId) {
        this.path = path;
        this.answerCode = answerCode;
        this.pageContent = pageContent;
        this.siteId = siteId;
    }


    @Override
    public int compareTo(Page o) {
        int compareSiteId = Integer.compare(this.siteId, o.siteId);
        if(compareSiteId != 0){
            return compareSiteId;
        }
        return this.path.compareTo(o.path);
    }
}

