package com.db.awmd.challenge.web;

import com.db.awmd.challenge.domain.TransferRequest;
import com.db.awmd.challenge.exception.AccountNotFoundException;
import com.db.awmd.challenge.exception.InsufficientBalanceException;
import com.db.awmd.challenge.service.FundsTransferService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/transfers")
@Slf4j
@RequiredArgsConstructor
public class FundsTransferController {

    private static final String TRANSFER_REQUEST_SUCCESSFULLY_COMPLETED = "Transfer request successfully completed";
    private static final String TRANSFER_REQUEST_COMPLETED_WITH_ERRORS = "Transfer request completed with errors";
    private static final String TRANSFER_REQUEST_RECEIVED = "Transfer request received";
    private final FundsTransferService fundsTransferService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> transfer(@RequestBody @Valid TransferRequest transferRequest) {

        log.info(TRANSFER_REQUEST_RECEIVED);
        try {
            fundsTransferService.transferFounds(transferRequest);
            log.info(TRANSFER_REQUEST_SUCCESSFULLY_COMPLETED);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (AccountNotFoundException e) {
            log.error(e.getMessage());
            log.warn(TRANSFER_REQUEST_COMPLETED_WITH_ERRORS);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (InsufficientBalanceException e) {
            log.error(e.getMessage());
            log.warn(TRANSFER_REQUEST_COMPLETED_WITH_ERRORS);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

}
