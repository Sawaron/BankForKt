package com.shoma.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BalanceResponse {
    String ownerName;
    BigDecimal balance;
}
