package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.TransferRequest;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.InsufficientBalanceException;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FundsTransferService {

    private final AccountsService accountsService;
    private final NotificationService notificationService;

    public void transferFounds(TransferRequest request) {
        Account from = getAccountOrElseThrowException(request.getSenderAccountId());
        Account to = getAccountOrElseThrowException(request.getRecipientAccountId());
        doTransferFounds(from, to, request.getAmount());
        notificationService.notifyAboutTransfer(from, String.format(
            "Founds transferred successfully to account: %s. Amount: %s", to.getAccountId(), request.getAmount()));
        notificationService.notifyAboutTransfer(to, String.format(
            "Founds transferred successfully from account: %s. Amount: %s", from.getAccountId(), request.getAmount()));
    }

    private void doTransferFounds(Account from, Account to, BigDecimal amount) {
        Account[] syncMonitors = getSortedSyncMonitors(from, to);
        synchronized (syncMonitors[0]) {
            synchronized (syncMonitors[1]) {
                withdraw(from, amount);
                deposit(to, amount);
            }
        }
    }

    private Account getAccountOrElseThrowException(String accountId) {
        return Optional.ofNullable(accountsService.getAccount(accountId))
            .orElseThrow(() -> new AccountNotFoundException("Account not found"));
    }

    private Account[] getSortedSyncMonitors(Account from, Account to) {
        return from.getAccountId().compareTo(to.getAccountId()) < 0
            ? new Account[]{from, to}
            : new Account[]{to, from};
    }

    private void withdraw(Account account, BigDecimal amount) {
        BigDecimal balance = account.getBalance();
        if (balance.compareTo(amount) >= 0) {
            account.setBalance(balance.subtract(amount));
        } else {
            throw new InsufficientBalanceException("Insufficient balance");
        }
    }

    private void deposit(Account account, BigDecimal amount) {
        account.setBalance(account.getBalance().add(amount));
    }

}
