package com.banka1.order.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * DTO representing a bank-owned internal account reference resolved through employee/account APIs.
 *
 * <p>{@code accountId} is the database primary key of the account. It is intentionally NOT aliased
 * to {@code accountNumber} or {@code brojRacuna}: those fields hold the 16-digit account number as
 * a string and parsing them as {@code Long} produced PK values that did not exist in the database,
 * causing downstream {@code accountClient.getAccountDetails(accountId)} lookups to 404 on the
 * subsequent {@code /internal/accounts/id/{accountId}/details} call.
 */
@Data
public class BankAccountDto {

    /** Database primary key of the bank account in account-service. */
    @JsonAlias({"id", "accountId"})
    private Long accountId;
}
