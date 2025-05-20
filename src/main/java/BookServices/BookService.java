package BookServices;


import BookEntities.Book;
import BookEntities.IdempotencyKey;
import BookReposities.BookRepository;
import BookReposities.IdempotencyKeyRepository;
import DTOs.BookRequest;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    private final BookRepository bookRepository;


    private final IsbnCounterService isbnCounterService;

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public BookService(BookRepository bookRepository, IsbnCounterService isbnCounterService, IdempotencyKeyRepository idempotencyKeyRepository) {
        this.bookRepository = bookRepository;
        this.isbnCounterService = isbnCounterService;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    @Transactional
    public Book createBook(BookRequest bookRequest, String idempotencyKey) {
        // Check idempotency key
        Optional<IdempotencyKey> existingKey = idempotencyKeyRepository.findById(idempotencyKey);
        if (existingKey.isPresent()) {
            return bookRepository.findById(Long.parseLong(existingKey.get().getResponse()))
                    .orElseThrow(() -> new RuntimeException("Book not found"));
        }

        String isbn = generateISBN();
        Book book = new Book();
        book.setTitle(bookRequest.getTitle());
        book.setAuthor(bookRequest.getAuthor());
        book.setIsbn(isbn);
        book = bookRepository.save(book);

        // Save idempotency key
        IdempotencyKey newKey = new IdempotencyKey();
        newKey.setKey(idempotencyKey);
        newKey.setResponse(book.getId().toString());
        newKey.setCreatedAt(LocalDateTime.now());
        idempotencyKeyRepository.save(newKey);

        return book;
    }

    public String generateISBN() {
        Long nextValue = isbnCounterService.getNextValue();
        String base = "978" + String.format("%09d", nextValue);
        int checkDigit = calculateCheckDigit(base);
        return base + checkDigit;
    }

    public int calculateCheckDigit(String base) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(base.charAt(i));
            sum += digit * (i % 2 == 0 ? 1 : 3);
        }
        return (10 - (sum % 10)) % 10;
    }


    public Object getBookById(long l) {
        return bookRepository.findById(l);
    }

    public void deleteBook(long id) {
        bookRepository.findById(id).ifPresentOrElse(book -> bookRepository.deleteById(id),
                ()->{
            throw new EntityNotFoundException("Book not found with id: " +id);
        }
        );
    }

    public Object updateBook(long id, BookRequest bookRequest) {
        Book existingBook = bookRepository.findById(id).orElseThrow(()-> new EntityNotFoundException("" + id));
        existingBook.setTitle(bookRequest.getTitle());
        existingBook.setAuthor(bookRequest.getAuthor());
        return bookRepository.save(existingBook);
    }

    public List<Book> getAllBooks() {
        return bookRepository.findById();
    }
}
