package com.shoma.controller;

import com.shoma.dto.*;
import com.shoma.enity.BankAccount;
import com.shoma.service.BankAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/account")
public class BankController {
    private final BankAccountService bankService;

    public BankController(BankAccountService bankService) {
        this.bankService = bankService;
    }

    @PostMapping("/create")
    public ResponseEntity<BankAccountResponse> createCard(@RequestBody CreateAccountRequest createAccountRequest) {
        BankAccount newAccount = bankService.saveAccount(createAccountRequest.getPassword(), createAccountRequest.getOwnerName());

        BankAccountResponse response = new BankAccountResponse();
        response.setId(newAccount.getId());
        response.setCardNumber(newAccount.getCardNumber());
        response.setOwnerName(newAccount.getOwnerName());
        response.setBalance(newAccount.getBalance());
        response.setBlocked(newAccount.isBlocked());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/balance")
    public ResponseEntity<BalanceResponse> getBalance(@RequestParam String cardNumber, @RequestParam String password) {
        BankAccount account = bankService.getAccountWithVerification(cardNumber, password);

        BalanceResponse response = new BalanceResponse();
        response.setOwnerName(account.getOwnerName());
        response.setBalance(account.getBalance());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/deposit")
    public ResponseEntity<String> deposit(@RequestBody TransactionRequest request) {
        bankService.deposit(request.getCardNumber(), request.getPassword(), request.getAmount());
        return ResponseEntity.ok("Баланс успешно пополнен");
    }

    @PutMapping("/withdraw")
    public ResponseEntity<String> withdraw(@RequestBody TransactionRequest request) {
        bankService.withdraw(request.getCardNumber(), request.getPassword(), request.getAmount());
        return ResponseEntity.ok("Деньги успешно сняты");
    }

    @PutMapping("/transfer")
    public ResponseEntity<String> transfer(@RequestBody TransferRequest request) {
        bankService.transfer(request.getCardNumberFrom(), request.getPassword(), request.getCardNumberTo(), request.getAmount());
        return ResponseEntity.ok("Перевод успешно выполнен!");
    }



}