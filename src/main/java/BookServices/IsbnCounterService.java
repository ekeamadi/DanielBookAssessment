package BookServices;


import BookEntities.IsbnCounter;
import BookReposities.IsbnCounterRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;


@Service
public class IsbnCounterService {

    private final IsbnCounterRepository isbnCounterRepository;

    public IsbnCounterService(IsbnCounterRepository isbnCounterRepository) {
        this.isbnCounterRepository = isbnCounterRepository;
    }

    @PostConstruct
    public void init() {
        if (!isbnCounterRepository.existsById(1L)) {
            IsbnCounter counter = new IsbnCounter();
            counter.setId(1L);
            counter.setCurrentValue(0L);
            isbnCounterRepository.save(counter);
        }
    }

    @Transactional
    public Long getNextValue() {
        IsbnCounter counter = isbnCounterRepository.findById(1L).orElseThrow();
        Long nextValue = counter.getCurrentValue() + 1;
        counter.setCurrentValue(nextValue);
        isbnCounterRepository.save(counter);
        return nextValue;
    }
}