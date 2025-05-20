package ManageBookController;


import BookEntities.Book;
import BookServices.BookService;
import DTOs.BookRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {
    @Autowired
    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    public ResponseEntity<Book> createBook
            (@Valid @RequestBody DTOs.BookRequest bookRequest, @RequestHeader
                    ("Idempotency-Key") String idempotencyKey) {
        Book book = bookService.createBook((BookRequest) bookRequest, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(book);
    }

    @GetMapping
    public ResponseEntity<List<Book>> getAllBooks() {
        return ResponseEntity.ok(bookService.getAllBooks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok((Book) bookService.getBookById(id));
    }

    @PutMapping("/{id}")
    public <BookRequest> ResponseEntity<Book> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody BookRequest bookRequest) {
        return ResponseEntity.ok((Book) bookService.updateBook((Long) id, (DTOs.BookRequest) bookRequest));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
