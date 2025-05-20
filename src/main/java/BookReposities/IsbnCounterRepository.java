package BookReposities;


import BookEntities.IsbnCounter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IsbnCounterRepository extends JpaRepository<IsbnCounter, Long> {}