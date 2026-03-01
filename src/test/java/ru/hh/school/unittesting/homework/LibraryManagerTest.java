package ru.hh.school.unittesting.homework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {

  @Mock
  NotificationService notificationService;

  @Mock
  UserService userService;

  @InjectMocks
  LibraryManager libraryManager;

  // Тесты addBook

  @ParameterizedTest
  @CsvSource({
      "book1, 5, 5",
      "book1, 0, 0"
  })
  void testAddBookSuccess(String bookId,
                          int quantity,
                          int expectedCount) {
    libraryManager.addBook(bookId, quantity);
    int actualCount = libraryManager.getAvailableCopies(bookId);
    assertEquals(expectedCount, actualCount);
  }

  @Test
  void testAddBookWithNegativeQuantityMakeNegativeCopies() {
    libraryManager.addBook("book1", -5);
    assertEquals(-5, libraryManager.getAvailableCopies("book1"));
  }

  // Тесты borrowBook

  @Test
  void testBorrowBookWhenUserNotActive() {
    String bookId = "book1";
    String userId = "user1";

    libraryManager.addBook(bookId, 5);
    when(userService.isUserActive(userId)).thenReturn(false);

    boolean result = libraryManager.borrowBook(bookId, userId);

    assertFalse(result);
    assertEquals(5, libraryManager.getAvailableCopies(bookId));

    verify(notificationService, times(1))
        .notifyUser(userId, "Your account is not active.");

    verify(notificationService, never())
        .notifyUser(userId, "You have borrowed the book: " + bookId);
  }

  @Test
  void testBorrowBookWhenNoCopiesOfBook() {
    String bookId = "book1";
    String userId = "user1";

    when(userService.isUserActive(userId)).thenReturn(true);

    boolean result = libraryManager.borrowBook(bookId, userId);

    assertFalse(result);

    verify(notificationService, never())
        .notifyUser(anyString(), anyString());
  }

  @Test
  void testBorrowBookSuccess() {
    String bookId = "book1";
    String userId = "user1";

    libraryManager.addBook(bookId, 3);

    when(userService.isUserActive(userId)).thenReturn(true);

    boolean result = libraryManager.borrowBook(bookId, userId);

    assertTrue(result);
    assertEquals(2, libraryManager.getAvailableCopies(bookId));

    verify(notificationService, times(1))
        .notifyUser(userId, "You have borrowed the book: " + bookId);

    verify(notificationService, never())
        .notifyUser(userId, "Your account is not active.");
  }

  @ParameterizedTest
  @CsvSource({
      "book1, user1, 5, 4",
      "book1, user1, 1, 0"
  })
  void testBorrowBookDecrementsInventory(String bookId,
                                         String userId,
                                         int initialQuantity,
                                         int expectedQuantity) {
    libraryManager.addBook(bookId, initialQuantity);
    when(userService.isUserActive(userId)).thenReturn(true);

    boolean result = libraryManager.borrowBook(bookId, userId);

    assertTrue(result);
    assertEquals(expectedQuantity, libraryManager.getAvailableCopies(bookId));

    verify(notificationService, times(1))
        .notifyUser(userId, "You have borrowed the book: " + bookId);
  }

  // Тесты returnBook

  @Test
  void testReturnBookSuccess() {
    String bookId = "book1";
    String userId = "user1";

    libraryManager.addBook(bookId, 2);

    when(userService.isUserActive(userId)).thenReturn(true);
    libraryManager.borrowBook(bookId, userId);

    boolean result = libraryManager.returnBook(bookId, userId);

    assertTrue(result);

    assertEquals(2, libraryManager.getAvailableCopies(bookId));

    verify(notificationService, times(1))
        .notifyUser(userId, "You have returned the book: " + bookId);
  }

  @Test
  void testReturnBookWhenBookWasNotBorrowed() {
    String bookId = "book1";
    String userId = "user1";

    libraryManager.addBook(bookId, 5);

    boolean result = libraryManager.returnBook(bookId, userId);

    assertFalse(result);

    assertEquals(5, libraryManager.getAvailableCopies(bookId));

    verify(notificationService, never())
        .notifyUser(anyString(), anyString());
  }

  @Test
  void testReturnBookByAnotherUser() {
    String bookId = "book1";
    String originalBorrower = "user1";
    String anotherUser = "user2";

    libraryManager.addBook(bookId, 3);

    when(userService.isUserActive(originalBorrower)).thenReturn(true);
    libraryManager.borrowBook(bookId, originalBorrower);

    boolean result = libraryManager.returnBook(bookId, anotherUser);

    assertFalse(result);

    assertEquals(2, libraryManager.getAvailableCopies(bookId));

    verify(notificationService, never())
        .notifyUser(anotherUser, "You have returned the book: " + bookId);
  }

  @ParameterizedTest
  @CsvSource({
      "book1, user1, 10, 10",
      "book1, user1, 1, 1"
  })
  void testReturnBookMakesInventorySameCount(String bookId,
                                         String userId,
                                         int initialQuantity,
                                         int expectedQuantity) {
    libraryManager.addBook(bookId, initialQuantity);
    when(userService.isUserActive(userId)).thenReturn(true);
    libraryManager.borrowBook(bookId, userId);

    boolean result = libraryManager.returnBook(bookId, userId);

    assertTrue(result);
    assertEquals(expectedQuantity, libraryManager.getAvailableCopies(bookId));

    verify(notificationService, times(1))
        .notifyUser(userId, "You have returned the book: " + bookId);
  }

  @Test
  void testReturnBookReturningSameBookTwice() {
    String bookId = "book1";
    String userId = "user1";

    libraryManager.addBook(bookId, 1);

    when(userService.isUserActive(userId)).thenReturn(true);
    libraryManager.borrowBook(bookId, userId);

    boolean firstReturn = libraryManager.returnBook(bookId, userId);

    boolean secondReturn = libraryManager.returnBook(bookId, userId);

    assertTrue(firstReturn);
    assertFalse(secondReturn);

    assertEquals(1, libraryManager.getAvailableCopies(bookId));

    verify(notificationService, times(1))
        .notifyUser(userId, "You have returned the book: " + bookId);
  }


  // Тесты calculateDynamicFee

  @ParameterizedTest
  @CsvSource({
      "10, false, false, 5.0",
      "10, true,  false, 7.5",
      "10, false, true,  4.0",
      "10, true,  true,  6.0",
      "0,  false, false, 0.0"
  })
  void testCalculateDynamicLateFeeSomeCombinations(int overdueDays,
                                                   boolean isBestseller,
                                                   boolean isPremiumMember,
                                                   double expectedFee) {
    double actualFee = libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember);

    assertEquals(expectedFee, actualFee, 0.01);
  }

  @Test
  void testCalculateDynamicLateFeeWhenNegativeDaysThrowsException() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> libraryManager.calculateDynamicLateFee(-5, false, false)
    );

    assertEquals("Overdue days cannot be negative.", exception.getMessage());
  }

  @Test
  void testCalculateDynamicLateFeeRoundingToTwoDecimal() {
    double fee = libraryManager.calculateDynamicLateFee(7, true, true);

    assertEquals(4.2, fee, 0.01);
  }
}
