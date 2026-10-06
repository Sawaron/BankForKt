package com.shoma.service;

import com.shoma.enity.BankAccount;
import com.shoma.exceptions.BankTerminalException;
import com.shoma.repository.BankAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Random;

@Service
public class BankAccountService {
    private final BankAccountRepository repository;
    private final Random random = new Random();
    private final StringRedisTemplate redisTemplate;
    private static final String FAILED_ATTEMPTS_PREFIX = "failed_attempts:";
    private static final String LOCK_PREFIX = "lock:";


    @Autowired
    public BankAccountService(BankAccountRepository repository, StringRedisTemplate redisTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
    }

    private void handleFailedPin(BankAccount account) {
        String cardNumber = account.getCardNumber();
        String attemptsKey = FAILED_ATTEMPTS_PREFIX + cardNumber;
        String lockKey = LOCK_PREFIX + cardNumber;
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);

        if (attempts != null && attempts >= 3) {
            redisTemplate.opsForValue().set(lockKey, "blocked", 10, java.util.concurrent.TimeUnit.MINUTES);
            redisTemplate.delete(attemptsKey);
            throw new BankTerminalException("Карта временно заблокирована на 10 минут за 3 неверных ввода пароля!");
        } else {
            long remaining = 3 - (attempts != null ? attempts : 0);
            throw new BankTerminalException("Неверный пароль! Осталось попыток: " + remaining);
        }
    }

    private void validateAmount(BigDecimal amount) {
        amount == null
    }

    public void deposit(String cardNumber, String password, BigDecimal amount) {
        validateAmount(amount);
        BankAccount account = getAccountWithVerification(cardNumber, password);
        account.setBalance(account.getBalance().add(amount));
        repository.save(account);
    }

    public void withdraw(String cardNumber, String password, BigDecimal amount) {
        validateAmount(amount);
        BankAccount account = getAccountWithVerification(cardNumber, password);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new BankTerminalException("Недостаточно средств!");
        }
        account.setBalance(account.getBalance().subtract(amount));
        repository.save(account);
    }

    @Transactional
    public void transfer(String cardNumberFrom, String password, String cardNumberTo, BigDecimal amount) {
        validateAmount(amount);
        if (cardNumberFrom != null && cardNumberFrom.equals(cardNumberTo)) {
            throw new BankTerminalException("Нельзя переводить деньги на ту же карту!");
        }
        BankAccount accountFrom = getAccountWithVerification(cardNumberFrom, password);
        BankAccount accountTo = getAccountWithoutPassword(cardNumberTo);

        if (accountFrom.getBalance().compareTo(amount) < 0) {
            throw new BankTerminalException("Недостаточно средств для перевода!");
        }
        accountFrom.setBalance(accountFrom.getBalance().subtract(amount));
        accountTo.setBalance(accountTo.getBalance().add(amount));

        repository.save(accountFrom);
        repository.save(accountTo);
    }

    public BankAccount getAccountWithoutPassword(String cardNumber) {
        String lockKey = LOCK_PREFIX + cardNumber;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            throw new BankTerminalException("Операция невозможна: Карта временно заблокирована");
        }

        BankAccount account = repository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new BankTerminalException("Карта с таким номером не найдена"));

        if (account.isBlocked()) {
            throw new BankTerminalException("Операция невозможна: Карта получателя заблокирована");
        }
        return account;
    }

    public BankAccount getAccountWithVerification(String cardNumber, String password) {
        String lockKey = LOCK_PREFIX + cardNumber;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            throw new BankTerminalException("Операция невозможна: Карта временно заблокирована");
        }

        BankAccount account = repository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new BankTerminalException("Карта с таким номером не найдена"));

        if (account.isBlocked()) {
            throw new BankTerminalException("Операция невозможна: Карта заблокирована");
        }

        if (!account.getPassword().equals(password)) {
            handleFailedPin(account);
        }
        String attemptsKey = FAILED_ATTEMPTS_PREFIX + cardNumber;
        redisTemplate.delete(attemptsKey);

        return account;
    }

    public String generateCardNumber() {
        LocalDate now = LocalDate.now();
        int year = now.getYear() % 100;
        int month = now.getMonthValue();
        int suffix1 = 1000 + random.nextInt(9000);
        int suffix2 = 1000 + random.nextInt(9000);
        int suffix3 = 1000 + random.nextInt(9000);
        return String.format("%02d-%02d-%04d-%04d-%04d", year, month, suffix1, suffix2, suffix3);
    }

    public BankAccount saveAccount(String pin, String owner) {
        BankAccount account = new BankAccount();
        account.setCardNumber(generateCardNumber());
        account.setPassword(pin);
        account.setOwnerName(owner);
        account.setBalance(BigDecimal.ZERO);
        return repository.save(account);
    }
}