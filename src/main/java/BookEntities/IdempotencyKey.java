package BookEntities;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdempotencyKey {
    @Id
    private String key;

    private String response; // Stores book ID as String

    private LocalDateTime createdAt;


}