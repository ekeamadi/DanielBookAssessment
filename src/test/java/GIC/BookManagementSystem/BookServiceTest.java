package GIC.BookManagementSystem;

import BookEntities.Book;
import BookEntities.IdempotencyKey;
import BookReposities.BookRepository;
import BookReposities.IdempotencyKeyRepository;
import BookServices.BookService;
import BookServices.IsbnCounterService;
import DTOs.BookRequest;
import ManageBookController.BookController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultMatcher;

import javax.script.Invocable;
import javax.script.ScriptException;
import java.time.LocalDateTime;
import java.util.Optional;

import static java.lang.reflect.Array.get;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.mock.http.server.reactive.MockServerHttpRequest.post;
import static org.springframework.mock.http.server.reactive.MockServerHttpRequest.put;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private IsbnCounterService isbnCounterService;

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @InjectMocks
    private BookService bookService;

    @Test
    void createBook_NewRequest_ShouldCreateNewBook() {
        // Arrange
        BookRequest request = new BookRequest("Effective Java", "Joshua Bloch");
        String idempotencyKey = "test-key-123";
        String expectedIsbn = "9780000001236";

        when(idempotencyKeyRepository.findById(idempotencyKey)).thenReturn(Optional.empty());
        when(isbnCounterService.getNextValue()).thenReturn(123L);
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book book = invocation.getArgument(0);
            book.setId(1L);
            return book;
        });

        // Act
        Book result = bookService.createBook(request, idempotencyKey);

        // Assert
        assertNotNull(result.getId());
        assertEquals("Effective Java", result.getTitle());
        assertEquals("Joshua Bloch", result.getAuthor());
        assertEquals(expectedIsbn, result.getIsbn());

        verify(idempotencyKeyRepository).save(any(IdempotencyKey.class));
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void createBook_DuplicateIdempotencyKey_ShouldReturnExistingBook() {
        // Arrange
        String idempotencyKey = "existing-key-456";
        Book existingBook = new Book(1L, "Existing Book", "Existing Author", "9781234567890");

        when(idempotencyKeyRepository.findById(idempotencyKey))
                .thenReturn(Optional.of(new IdempotencyKey(idempotencyKey, "1", LocalDateTime.now())));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existingBook));

        // Act
        Book result = bookService.createBook(new BookRequest(), idempotencyKey);

        // Assert
        assertEquals(existingBook, result);
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void generateISBN_ShouldCreateValidISBN13() {
        // Arrange
        when(isbnCounterService.getNextValue()).thenReturn(1L);

        // Act
        String isbn = bookService.generateISBN();

        // Assert
        assertEquals(13, isbn.length());
        assertEquals("9780000000010", isbn); // 1L formatted to 9 digits: 000000001
    }
}




@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

   // @MockBean
    private BookService bookService;

    @Test
    void createBook_ValidRequest_ShouldReturnCreated() throws Exception {
        BookResponse response = new BookResponse(1L, "Effective Java", "Joshua Bloch", "9780000000010");

        when(bookService.createBook(any(BookRequest.class), anyString()))
                .getMock();

        mockMvc.perform((RequestBuilder) post("/api/books")
                        .header("Idempotency-Key", "test-key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.valueOf("{\"title\":\"Effective Java\",\"author\":\"Joshua Bloch\"}")))
                .andExpect(status().isCreated())
                .andExpect((ResultMatcher) jsonPath("$.id").value(1))
                .andExpect((ResultMatcher) jsonPath("$.isbn").value("9780000000010"));
    }

    @Test
    void createBook_MissingIdempotencyKey_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform((RequestBuilder) post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.valueOf("{\"title\":\"Effective Java\",\"author\":\"Joshua Bloch\"}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    <BookResponse>
    void getBookById_ExistingId_ShouldReturnBook() throws Exception {
        final BookResponse response = null;
        when(bookService.getBookById(1L)).thenReturn(response);
        mockMvc.perform((RequestBuilder) get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect((ResultMatcher) jsonPath("$.title").value("Effective Java"));
    }

    private Object get(String path) {
        return null;
    }

    @Test
    void updateBook_ValidRequest_ShouldReturnUpdatedBook() throws Exception {
        BookResponse response = new BookResponse(1L, "Updated Title", "Updated Author", "9780000000010");

        when(bookService.updateBook(eq(1L), any(BookRequest.class))).thenReturn(response);

        mockMvc.perform((RequestBuilder) put("/api/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.valueOf("{\"title\":\"Updated Title\",\"author\":\"Updated Author\"}")))
                .andExpect(status().isOk())
                .andExpect((ResultMatcher) jsonPath("$.title").value("Updated Title"));
    }

    @Test
    void deleteBook_ExistingId_ShouldReturnNoContent() throws Exception {
        doNothing().when(bookService).deleteBook(1L);

        mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void createBook_InvalidRequest_ShouldReturnValidationErrors() throws Exception {
        mockMvc.perform((RequestBuilder) post("/api/books")
                        .header("Idempotency-Key", "test-key-123")
                        .contentType(MediaType.APPLICATION_JSON).contentLength(Long.parseLong("{\"title\":\"\",\"author\":\"\"}")))
                .andExpect(status().isBadRequest())
                .andExpect((ResultMatcher) jsonPath("$.title").value("must not be blank"))
                .andExpect((ResultMatcher) jsonPath("$.author").value("must not be blank"));
    }
}




class IsbnGenerationTest {

    private final BookService bookService;

    IsbnGenerationTest(BookService bookService) {
        this.bookService = bookService;
    }

    @Test
    void calculateCheckDigit_ShouldReturnCorrectDigit() {
        // Test case from requirements example
        String base = "978030640615";
        int checkDigit = bookService.calculateCheckDigit(base);
        assertEquals(7, checkDigit);
    }

    @Test
    void generateISBN_ShouldCreateValidFormat() throws ScriptException, NoSuchMethodException {
        // Using reflection to test private method
        Invocable Whitebox = null;
        String isbn = Whitebox.invokeMethod(bookService, "generateISBN").toString();
        assertTrue(isbn.matches("^978\\d{10}$"));
    }
}