package com.shoma.dto;


import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequest {
    String cardNumber;
    String password;
    BigDecimal amount;
}
