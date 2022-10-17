package com.db.awmd.challenge.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.TransferRequest;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.InsufficientBalanceException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Slf4j
@ExtendWith(MockitoExtension.class)
class FundsTransferServiceTest {

    private static final String FROM_ACCOUNT_ID = "1111";
    private static final String TO_ACCOUNT_ID = "3333";

    @InjectMocks
    private FundsTransferService fundsTransferService;

    @Mock
    private AccountsService accountsService;
    @Mock
    private NotificationService notificationService;

    @Test
    void transferFounds_whenAccountNotFound_thenAccountNotFoundExceptionIsThrown() {
        TransferRequest transferRequest = new TransferRequest(FROM_ACCOUNT_ID, TO_ACCOUNT_ID, BigDecimal.TEN);
        when(accountsService.getAccount(FROM_ACCOUNT_ID))
            .thenReturn(null);

        assertThrows(AccountNotFoundException.class,
            () -> fundsTransferService.transferFounds(transferRequest));

        verify(accountsService).getAccount(FROM_ACCOUNT_ID);
        verifyNoMoreInteractions(accountsService);
        verifyNoInteractions(notificationService);
    }

    @Test
    void transferFounds_whenInsufficientBalance_thenInsufficientBalanceExceptionIsThrown() {
        Account fromAccount = new Account(FROM_ACCOUNT_ID, BigDecimal.ONE);
        Account toAccount = new Account(TO_ACCOUNT_ID, BigDecimal.TEN);
        TransferRequest transferRequest = new TransferRequest(FROM_ACCOUNT_ID, TO_ACCOUNT_ID, BigDecimal.TEN);
        when(accountsService.getAccount(FROM_ACCOUNT_ID))
            .thenReturn(fromAccount);
        when(accountsService.getAccount(TO_ACCOUNT_ID))
            .thenReturn(toAccount);

        assertThrows(InsufficientBalanceException.class,
            () -> fundsTransferService.transferFounds(transferRequest));

        verify(accountsService).getAccount(FROM_ACCOUNT_ID);
        verify(accountsService).getAccount(TO_ACCOUNT_ID);
        verifyNoMoreInteractions(accountsService);
        verifyNoInteractions(notificationService);
    }

    @Test
    void transferFounds_whenNoIssues_thenFoundsTransferred() {
        Account fromAccount = new Account(FROM_ACCOUNT_ID, BigDecimal.ONE);
        Account toAccount = new Account(TO_ACCOUNT_ID, BigDecimal.TEN);
        TransferRequest transferRequest = new TransferRequest(FROM_ACCOUNT_ID, TO_ACCOUNT_ID, BigDecimal.ONE);
        when(accountsService.getAccount(FROM_ACCOUNT_ID))
            .thenReturn(fromAccount);
        when(accountsService.getAccount(TO_ACCOUNT_ID))
            .thenReturn(toAccount);

        assertDoesNotThrow(() -> fundsTransferService.transferFounds(transferRequest));

        assertEquals(BigDecimal.ZERO, fromAccount.getBalance());
        assertEquals(BigDecimal.valueOf(11), toAccount.getBalance());
        verify(accountsService).getAccount(FROM_ACCOUNT_ID);
        verify(accountsService).getAccount(TO_ACCOUNT_ID);
        verifyNoMoreInteractions(accountsService);
        verify(notificationService).notifyAboutTransfer(eq(fromAccount), anyString());
        verify(notificationService).notifyAboutTransfer(eq(toAccount), anyString());
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void transferFounds_whenNoIssuesConcurrently_thenFoundsTransferred() throws InterruptedException {
        BigDecimal initialSum = BigDecimal.TEN;
        Account firstAccount = new Account(FROM_ACCOUNT_ID, initialSum);
        Account secondAccount = new Account(TO_ACCOUNT_ID, initialSum);
        when(accountsService.getAccount(FROM_ACCOUNT_ID))
            .thenReturn(firstAccount);
        when(accountsService.getAccount(TO_ACCOUNT_ID))
            .thenReturn(secondAccount);

        List<TransferRequest> transferRequests = generateTransferRequests();
        int transferRequestsAmount = transferRequests.size();
        CountDownLatch countDownLatch = new CountDownLatch(transferRequestsAmount);

        Stream.ofNullable(transferRequests)
            .flatMap(Collection::stream)
            .map(tr -> new Worker(() -> fundsTransferService.transferFounds(tr), countDownLatch))
            .map(Thread::new)
            .forEach(Thread::start);
        countDownLatch.await();

        assertEquals(initialSum, firstAccount.getBalance());
        assertEquals(initialSum, secondAccount.getBalance());
        verify(accountsService, times(transferRequestsAmount)).getAccount(FROM_ACCOUNT_ID);
        verify(accountsService, times(transferRequestsAmount)).getAccount(TO_ACCOUNT_ID);
        verifyNoMoreInteractions(accountsService);
        verify(notificationService, times(transferRequestsAmount)).notifyAboutTransfer(eq(firstAccount), anyString());
        verify(notificationService, times(transferRequestsAmount)).notifyAboutTransfer(eq(secondAccount), anyString());
        verifyNoMoreInteractions(notificationService);
    }

    private List<TransferRequest> generateTransferRequests() {
        return IntStream.range(0, 10)
            .mapToObj(i -> i % 2 == 0
                ? new TransferRequest(FROM_ACCOUNT_ID, TO_ACCOUNT_ID, BigDecimal.ONE)
                : new TransferRequest(TO_ACCOUNT_ID, FROM_ACCOUNT_ID, BigDecimal.ONE))
            .collect(Collectors.toList());
    }

    @RequiredArgsConstructor
    private static class Worker implements Runnable {

        private final Runnable runnable;
        private final CountDownLatch countDownLatch;

        @Override
        public void run() {
            runnable.run();
            log.info("Counted down. Thread: {}", Thread.currentThread().getName());
            countDownLatch.countDown();
        }
    }
}