package com.microvault.finance;

import com.microvault.finance.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class FinanceApplicationTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void startsWithDatabase() {
        assertNotNull(transactionRepository);
    }
}
