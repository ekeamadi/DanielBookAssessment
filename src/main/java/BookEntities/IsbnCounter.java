package BookEntities;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
    public class IsbnCounter {
        @Id
        private Long id = 1L;

        private Long currentValue = 0L;

        @Version
        private Long version;


    }

