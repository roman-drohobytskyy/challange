package com.db.awmd.challenge.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.TransferRequest;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.InsufficientBalanceException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}