package main.model;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "lemma")
public class Lemma implements Serializable, Comparable<Lemma> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;
    @Column(nullable = false)
    private int frequency;
    @Column(name = "site_id", nullable = false)
    private int siteId;

    public Lemma() {
    }
    public Lemma(String lemma, int siteId) {
        this.lemma = lemma;
        this.siteId = siteId;
    }

    public Lemma(String lemma, int frequency, int siteId) {
        this.lemma = lemma;
        this.frequency = frequency;
        this.siteId = siteId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public int getFrequency() {
        return frequency;
    }

    public void increaseFrequency() {this.frequency += 1;}

    public void decreaseFrequency() {this.frequency -= 1;}

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public int getSiteId() {
        return siteId;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

    @Override
    public int compareTo(Lemma o) {
        int compareLemma = this.lemma.compareTo(o.lemma);
        if(compareLemma != 0){
            return compareLemma;
        }
        return Integer.compare(this.siteId, o.siteId);

    }
}
