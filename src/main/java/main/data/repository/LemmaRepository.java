package main.data.repository;

import main.data.model.Lemma;
import org.springframework.data.repository.CrudRepository;

public interface LemmaRepository extends CrudRepository<Lemma, Integer> {
}
