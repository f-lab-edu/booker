// package com.bookerapp.core.domain.service;

// import com.bookerapp.core.domain.model.dto.BookLoanDto;
// import com.bookerapp.core.domain.model.entity.Book;
// import com.bookerapp.core.domain.model.entity.LoanStatus;
// import com.bookerapp.core.domain.repository.BookLoanRepository;
// import com.bookerapp.core.domain.repository.BookRepository;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
// import org.springframework.context.annotation.Import;
// import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.Objects;
// import java.util.UUID;
// import java.util.concurrent.*;

// import static org.assertj.core.api.Assertions.assertThat;

// @DataJpaTest
// @Import(BookLoanService.class)
// @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// class BookLoanServiceIsolationTest {
//     @Autowired
//     private BookLoanRepository bookLoanRepository;

//     @Autowired
//     private BookRepository bookRepository;

//     @Autowired
//     private BookLoanService bookLoanService;

//     @Test
//     void createLoan_ShouldBeAtomicWithReadCommittedIsolation() throws Exception {
//         // given
//         Book book = bookRepository.save(Book.builder()
//                 .title("동시성 테스트")
//                 .author("테스터")
//                 .isbn(UUID.randomUUID().toString())
//                 .build());

//         int threadCount = 10;
//         ExecutorService executor = Executors.newFixedThreadPool(threadCount);
//         CountDownLatch readyLatch = new CountDownLatch(threadCount);
//         CountDownLatch startLatch = new CountDownLatch(1);

//         List<Future<BookLoanDto.Response>> futures = new ArrayList<>();

//         for (int i = 0; i < threadCount; i++) {
//             final String memberId = "member" + i;
//             futures.add(executor.submit(() -> {
//                 readyLatch.countDown();
//                 startLatch.await();
//                 return bookLoanService.createLoan(memberId, new BookLoanDto.Request(book.getId()));
//             }));
//         }

//         // 모든 스레드 준비될 때까지 대기
//         readyLatch.await();
//         // 모든 스레드 동시에 시작
//         startLatch.countDown();

//         List<BookLoanDto.Response> results = new ArrayList<>();
//         for (Future<BookLoanDto.Response> future : futures) {
//             try {
//                 results.add(future.get(3, TimeUnit.SECONDS));
//             } catch (TimeoutException e) {
//                 // 무한 대기 방지
//                 results.add(null);
//             }
//         }

//         // then
//         long activeCount = results.stream()
//                 .filter(Objects::nonNull)
//                 .filter(r -> r.getStatus() == LoanStatus.ACTIVE)
//                 .count();
//         long waitingCount = results.stream()
//                 .filter(Objects::nonNull)
//                 .filter(r -> r.getStatus() == LoanStatus.WAITING)
//                 .count();

//         assertThat(activeCount).isEqualTo(1);
//         assertThat(waitingCount).isEqualTo(threadCount - 1);
//     }
// }
