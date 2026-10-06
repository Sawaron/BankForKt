package com.shoma;

import com.shoma.enity.BankAccount;
import com.shoma.exceptions.BankTerminalException;
import com.shoma.repository.BankAccountRepository;
import com.shoma.service.BankAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * ВОСХОДЯЩИЕ тесты: реальный сервис + реальный репозиторий + H2. Контроллера нет,
 * тест сам вызывает сервис (драйвер). Заглушка: только Redis (внешняя система).
 */
@DataJpaTest
@Import(BankAccountService.class)
class BankBottomUpTest {

    @Autowired BankAccountService service;
    @Autowired BankAccountRepository repository;
    @Autowired TestEntityManager em;

    @MockBean StringRedisTemplate redisTemplate;

    private ValueOperations<String, String> valueOps;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    private BankAccount persist(String card, String pin, String balance, boolean blocked) {
        BankAccount a = new BankAccount();
        a.setCardNumber(card);
        a.setPassword(pin);
        a.setOwnerName("Owner-" + card);
        a.setBalance(new BigDecimal(balance));
        a.setBlocked(blocked);
        return em.persistAndFlush(a);
    }

    /** Читаем из БД заново, а не из кэша Hibernate. */
    private BankAccount reload(String card) {
        em.flush();
        em.clear();
        return repository.findByCardNumber(card).orElseThrow();
    }

    // ---------- позитивные ----------

    @Test
    void saveAccount_persistsWithZeroBalance() {
        BankAccount created = service.saveAccount("1234", "Ivan");

        BankAccount fromDb = reload(created.getCardNumber());
        assertThat(fromDb.getId()).isPositive();
        assertThat(fromDb.getOwnerName()).isEqualTo("Ivan");
        assertThat(fromDb.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(fromDb.getCardNumber()).matches("\\d{2}-\\d{2}-\\d{4}-\\d{4}-\\d{4}");
    }

    @Test
    void deposit_updatesBalanceInDatabase() {
        persist("C1", "1234", "100", false);

        service.deposit("C1", "1234", new BigDecimal("50"));

        assertThat(reload("C1").getBalance()).isEqualByComparingTo("150");
    }

    @Test
    void withdraw_reducesBalanceInDatabase() {
        persist("C1", "1234", "100", false);

        service.withdraw("C1", "1234", new BigDecimal("30"));

        assertThat(reload("C1").getBalance()).isEqualByComparingTo("70");
    }

    @Test
    void transfer_movesMoneyBetweenAccounts() {
        persist("A", "1234", "1000", false);
        persist("B", "9999", "0", false);

        service.transfer("A", "1234", "B", new BigDecimal("300"));

        assertThat(reload("A").getBalance()).isEqualByComparingTo("700");
        assertThat(reload("B").getBalance()).isEqualByComparingTo("300");
    }

    // ---------- негативные ----------

    @Test
    void withdraw_insufficientFunds_throws_andBalanceUnchanged() {
        persist("C1", "1234", "10", false);

        assertThatThrownBy(() -> service.withdraw("C1", "1234", new BigDecimal("50")))
                .isInstanceOf(BankTerminalException.class)
                .hasMessageContaining("Недостаточно");

        assertThat(reload("C1").getBalance()).isEqualByComparingTo("10");
    }

    @Test
    void deposit_unknownCard_throws() {
        assertThatThrownBy(() -> service.deposit("NOPE", "1234", BigDecimal.TEN))
                .isInstanceOf(BankTerminalException.class)
                .hasMessageContaining("не найдена");
    }

    @Test
    void transfer_toBlockedCard_throws_andSenderBalanceUnchanged() {
        persist("A", "1234", "1000", false);
        persist("B", "9999", "0", true);

        assertThatThrownBy(() -> service.transfer("A", "1234", "B", new BigDecimal("100")))
                .isInstanceOf(BankTerminalException.class);

        assertThat(reload("A").getBalance()).isEqualByComparingTo("1000");
        assertThat(reload("B").getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void deposit_thirdWrongPin_locksCardInRedis_andBalanceUnchanged() {
        persist("C1", "1234", "100", false);
        when(valueOps.increment("failed_attempts:C1")).thenReturn(3L);

        assertThatThrownBy(() -> service.deposit("C1", "0000", BigDecimal.TEN))
                .isInstanceOf(BankTerminalException.class)
                .hasMessageContaining("10 минут");

        verify(valueOps).set("lock:C1", "blocked", 10L, TimeUnit.MINUTES);
        assertThat(reload("C1").getBalance()).isEqualByComparingTo("100");
    }

    @Test
    void deposit_negativeAmount_throws_andBalanceUnchanged() {
        persist("C1", "1234", "100", false);

        assertThatThrownBy(() -> service.deposit("C1", "1234", new BigDecimal("-50")))
                .isInstanceOf(BankTerminalException.class);

        assertThat(reload("C1").getBalance()).isEqualByComparingTo("100");
    }
}
