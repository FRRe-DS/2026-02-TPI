package ar.edu.utn.frre.m3.adapter.persistence;

import ar.edu.utn.frre.m3.application.port.UnitOfWork;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * UnitOfWork implementation using Spring's TransactionTemplate.
 */
@Component
public class SpringTransactionUnitOfWork implements UnitOfWork {

    private final TransactionTemplate transactionTemplate;

    public SpringTransactionUnitOfWork(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate cannot be null");
    }

    @Override
    public <T> T executeInTransaction(Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }

    @Override
    public void executeInTransaction(Runnable action) {
        transactionTemplate.executeWithoutResult(status -> action.run());
    }
}

