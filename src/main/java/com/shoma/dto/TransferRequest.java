package com.shoma.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {
    String cardNumberFrom;
    String password;
    BigDecimal amount;
    String cardNumberTo;
}
