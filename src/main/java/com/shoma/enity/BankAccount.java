package com.shoma.enity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;


@Data
@Entity
@Table(name = "accounts")
public class BankAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    @Column(name = "card_number",nullable = false, unique = true, length = 25)
    String cardNumber;
    @Column(name = "password",nullable = false)
    String password;
    @Column(name = "balance")
    BigDecimal balance;
    @Column(name = "owner_name")
    String ownerName;
    @Column(name = "is_blocked")
    boolean blocked;
    @Column(name = "failed_attempts")
    int failedAttempts;

    public BankAccount() {

    }



}
