package main.data.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
@Setter

@Entity
@Table(name = "site")
public class Site  implements Serializable, Comparable<Site> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')", nullable = false)
    private Status status;

    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column( columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;

    @Column( columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;

    public Site(){
        status = Status.INDEXING;
        statusTime = LocalDateTime.now();
        lastError = null;
    }

    public Site(String lastError) {
        this.lastError = lastError;
    }

    @Override
    public int compareTo(Site o) {
        int compareSiteId = Integer.compare(this.id, o.id);
        if(compareSiteId != 0){
            return compareSiteId;
        }
        return this.url.compareTo(o.url);
    }
}

