package main.data.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;


@NoArgsConstructor
@Getter

@Entity
@Table(name = "lemma")
public class Lemma implements Serializable, Comparable<Lemma> {

    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;

    @Column(name = "site_id", nullable = false)
    private int siteId;


    public Lemma(String lemma, int siteId) {
        this.lemma = lemma;
        this.siteId = siteId;
    }

    public Lemma(String lemma, int frequency, int siteId) {
        this.lemma = lemma;
        this.frequency = frequency;
        this.siteId = siteId;
    }

    public void increaseFrequency() {this.frequency += 1;}

    public void decreaseFrequency() {this.frequency -= 1;}

    public void increaseFrequencyByVal(int value) {this.frequency += value;}

    @Override
    public int compareTo(Lemma o) {
        int compareLemma = this.lemma.compareTo(o.lemma);
        if(compareLemma != 0){
            return compareLemma;
        }
        return Integer.compare(this.siteId, o.siteId);

    }
}
