package com.shoma;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoma.controller.BankController;
import com.shoma.enity.BankAccount;
import com.shoma.repository.BankAccountRepository;
import com.shoma.service.BankAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * НИСХОДЯЩИЕ тесты: реальный контроллер + реальный сервис.
 * Заглушки: BankAccountRepository и StringRedisTemplate (Redis). Базы данных нет.
 */
@WebMvcTest(BankController.class)
@Import(BankAccountService.class)
class BankTopDownTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean BankAccountRepository repository;
    @MockBean StringRedisTemplate redisTemplate;

    private ValueOperations<String, String> valueOps;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    private BankAccount account(String card, String pin, String balance, boolean blocked) {
        BankAccount a = new BankAccount();
        a.setCardNumber(card);
        a.setPassword(pin);
        a.setOwnerName("Owner-" + card);
        a.setBalance(new BigDecimal(balance));
        a.setBlocked(blocked);
        return a;
    }

    private ResultActions putJson(String url, Object body) throws Exception {
        return mvc.perform(put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    // ---------- позитивные ----------

    @Test
    void createAccount_returns201AndJson() throws Exception {
        when(repository.save(any(BankAccount.class))).thenAnswer(inv -> {
            BankAccount a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        mvc.perform(post("/account/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerName\":\"Ivan\",\"password\":\"1234\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ownerName").value("Ivan"))
                .andExpect(jsonPath("$.cardNumber", matchesPattern("\\d{2}-\\d{2}-\\d{4}-\\d{4}-\\d{4}")))
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void getBalance_returns200AndBalance() throws Exception {
        when(repository.findByCardNumber("C1")).thenReturn(Optional.of(account("C1", "1234", "100.00", false)));

        mvc.perform(get("/account/balance").param("cardNumber", "C1").param("password", "1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerName").value("Owner-C1"))
                .andExpect(jsonPath("$.balance").value(100.0));
    }

    @Test
    void deposit_returns200_andIncreasesBalance() throws Exception {
        BankAccount acc = account("C1", "1234", "100", false);
        when(repository.findByCardNumber("C1")).thenReturn(Optional.of(acc));

        putJson("/account/deposit", Map.of("cardNumber", "C1", "password", "1234", "amount", new BigDecimal("50")))
                .andExpect(status().isOk());

        assertThat(acc.getBalance()).isEqualByComparingTo("150");
        verify(repository).save(acc);
    }

    @Test
    void transfer_returns200_andMovesMoney() throws Exception {
        BankAccount from = account("A", "1234", "500", false);
        BankAccount to = account("B", "9999", "100", false);
        when(repository.findByCardNumber("A")).thenReturn(Optional.of(from));
        when(repository.findByCardNumber("B")).thenReturn(Optional.of(to));

        putJson("/account/transfer", Map.of("cardNumberFrom", "A", "password", "1234",
                "cardNumberTo", "B", "amount", new BigDecimal("200")))
                .andExpect(status().isOk());

        assertThat(from.getBalance()).isEqualByComparingTo("300");
        assertThat(to.getBalance()).isEqualByComparingTo("300");
        verify(repository, times(2)).save(any(BankAccount.class));
    }

    // ---------- негативные ----------

    @Test
    void withdraw_insufficientFunds_returns400() throws Exception {
        when(repository.findByCardNumber("C1")).thenReturn(Optional.of(account("C1", "1234", "10", false)));

        putJson("/account/withdraw", Map.of("cardNumber", "C1", "password", "1234", "amount", new BigDecimal("50")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bank Operation Error"));

        verify(repository, never()).save(any(BankAccount.class));
    }

    @Test
    void transfer_unknownRecipient_returns400() throws Exception {
        when(repository.findByCardNumber("A")).thenReturn(Optional.of(account("A", "1234", "500", false)));
        when(repository.findByCardNumber("NOPE")).thenReturn(Optional.empty());

        putJson("/account/transfer", Map.of("cardNumberFrom", "A", "password", "1234",
                "cardNumberTo", "NOPE", "amount", new BigDecimal("100")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        verify(repository, never()).save(any(BankAccount.class));
    }

    @Test
    void deposit_wrongPassword_returns400() throws Exception {
        when(repository.findByCardNumber("C1")).thenReturn(Optional.of(account("C1", "1234", "100", false)));
        when(valueOps.increment("failed_attempts:C1")).thenReturn(1L);

        putJson("/account/deposit", Map.of("cardNumber", "C1", "password", "0000", "amount", new BigDecimal("50")))
                .andExpect(status().isBadRequest());

        verify(repository, never()).save(any(BankAccount.class));
    }

    @Test
    void deposit_blockedCard_returns400() throws Exception {
        when(repository.findByCardNumber("C1")).thenReturn(Optional.of(account("C1", "1234", "100", true)));

        putJson("/account/deposit", Map.of("cardNumber", "C1", "password", "1234", "amount", new BigDecimal("50")))
                .andExpect(status().isBadRequest());

        verify(repository, never()).save(any(BankAccount.class));
    }

    @Test
    void deposit_negativeAmount_returns400() throws Exception {
        BankAccount acc = account("C1", "1234", "100", false);
        when(repository.findByCardNumber("C1")).thenReturn(Optional.of(acc));

        putJson("/account/deposit", Map.of("cardNumber", "C1", "password", "1234", "amount", new BigDecimal("-50")))
                .andExpect(status().isBadRequest());

        assertThat(acc.getBalance()).isEqualByComparingTo("100");
        verify(repository, never()).save(any(BankAccount.class));
    }

    @Test
    void deposit_missingAmount_returns400() throws Exception {
        when(repository.findByCardNumber("C1")).thenReturn(Optional.of(account("C1", "1234", "100", false)));

        putJson("/account/deposit", Map.of("cardNumber", "C1", "password", "1234"))
                .andExpect(status().isBadRequest());

        verify(repository, never()).save(any(BankAccount.class));
    }
}
