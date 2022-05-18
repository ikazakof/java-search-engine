package main.model;

import org.springframework.lang.NonNull;

import javax.persistence.*;
import java.io.Serializable;

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

    public Page(){
    }

    public Page(String path) {
        this.path = path;
    }

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


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getAnswerCode() {
        return answerCode;
    }

    public void setAnswerCode(int answerCode) {
        this.answerCode = answerCode;
    }

    public String getPageContent() {
        return pageContent;
    }

    public void setPageContent(String pageContent) {
        this.pageContent = pageContent;
    }

    public int getSiteId() {
        return siteId;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

    @Override
    public int compareTo(Page o) {
        int compareSiteId = Integer.compare(this.siteId, o.siteId);
        if(compareSiteId != 0){
            return compareSiteId;
        }
        int compareId = Integer.compare(this.id, o.id);
        if (compareId != 0){
            return compareId;
        }
        return this.getPath().compareTo(o.getPath());
    }
}

