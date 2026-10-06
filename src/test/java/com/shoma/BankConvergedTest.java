package com.shoma;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoma.repository.BankAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * СХОДЯЩИЕСЯ тесты: контроллер + сервис + репозиторий + H2 работают вместе.
 * Заглушка осталась только для Redis (внешняя система).
 */
@SpringBootTest
@AutoConfigureMockMvc
class BankConvergedTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired BankAccountRepository repository;

    @MockBean StringRedisTemplate redisTemplate;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        repository.deleteAll();
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    private String createAccount(String owner, String pin) throws Exception {
        String body = mvc.perform(post("/account/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("ownerName", owner, "password", pin))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("cardNumber").asText();
    }

    private ResultActions sendPut(String url, Object body) throws Exception {
        return mvc.perform(put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private void deposit(String card, String pin, String amount) throws Exception {
        sendPut("/account/deposit", Map.of("cardNumber", card, "password", pin, "amount", new BigDecimal(amount)))
                .andExpect(status().isOk());
    }

    private BigDecimal balanceInDb(String card) {
        return repository.findByCardNumber(card).orElseThrow().getBalance();
    }

    // ---------- позитивные ----------

    @Test
    void fullFlow_createDepositTransfer_balancesCorrectInDb() throws Exception {
        String from = createAccount("Sender", "1111");
        String to = createAccount("Receiver", "2222");
        deposit(from, "1111", "1000");

        sendPut("/account/transfer", Map.of("cardNumberFrom", from, "password", "1111",
                "cardNumberTo", to, "amount", new BigDecimal("300")))
                .andExpect(status().isOk());

        mvc.perform(get("/account/balance").param("cardNumber", from).param("password", "1111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(700.0));
        assertThat(balanceInDb(from)).isEqualByComparingTo("700");
        assertThat(balanceInDb(to)).isEqualByComparingTo("300");
    }

    @Test
    void fullFlow_depositThenWithdraw_balanceCorrectInDb() throws Exception {
        String card = createAccount("Ivan", "1234");
        deposit(card, "1234", "1000");

        sendPut("/account/withdraw", Map.of("cardNumber", card, "password", "1234", "amount", new BigDecimal("400")))
                .andExpect(status().isOk());

        assertThat(balanceInDb(card)).isEqualByComparingTo("600");
    }

    // ---------- негативные ----------

    @Test
    void transfer_insufficientFunds_returns400_andBalancesUnchanged() throws Exception {
        String from = createAccount("Sender", "1111");
        String to = createAccount("Receiver", "2222");
        deposit(from, "1111", "100");

        sendPut("/account/transfer", Map.of("cardNumberFrom", from, "password", "1111",
                "cardNumberTo", to, "amount", new BigDecimal("500")))
                .andExpect(status().isBadRequest());

        assertThat(balanceInDb(from)).isEqualByComparingTo("100");
        assertThat(balanceInDb(to)).isEqualByComparingTo("0");
    }

    @Test
    void withdraw_negativeAmount_returns400_andBalanceUnchanged() throws Exception {
        String card = createAccount("Ivan", "1234");
        deposit(card, "1234", "1000");

        sendPut("/account/withdraw", Map.of("cardNumber", card, "password", "1234", "amount", new BigDecimal("-100")))
                .andExpect(status().isBadRequest());

        assertThat(balanceInDb(card)).isEqualByComparingTo("1000");
    }

    @Test
    void transfer_toSameCard_returns400() throws Exception {
        String card = createAccount("Ivan", "1234");
        deposit(card, "1234", "1000");

        sendPut("/account/transfer", Map.of("cardNumberFrom", card, "password", "1234",
                "cardNumberTo", card, "amount", new BigDecimal("100")))
                .andExpect(status().isBadRequest());

        assertThat(balanceInDb(card)).isEqualByComparingTo("1000");
    }

    @Test
    void getBalance_unknownCard_returns400() throws Exception {
        mvc.perform(get("/account/balance").param("cardNumber", "NOPE").param("password", "1234"))
                .andExpect(status().isBadRequest());
    }
}
