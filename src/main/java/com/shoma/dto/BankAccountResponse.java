package com.shoma.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BankAccountResponse {
    long id;
    String cardNumber;
    String ownerName;
    BigDecimal balance;
    boolean isBlocked;

}
