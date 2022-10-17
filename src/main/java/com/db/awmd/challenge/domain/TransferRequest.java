package com.db.awmd.challenge.domain;

import java.math.BigDecimal;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

    @NotBlank
    private String senderAccountId;

    @NotBlank
    private String recipientAccountId;

    @NotNull
    @Min(value = 0, message = "Amount to transfer must be positive.")
    private BigDecimal amount;

}
