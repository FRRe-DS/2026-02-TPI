package ar.edu.utn.frre.m3.application.port;

import java.util.function.Supplier;

/**
 * Output port for managing transactional boundaries (Unit of Work).
 */
public interface UnitOfWork {

    <T> T executeInTransaction(Supplier<T> action);

    void executeInTransaction(Runnable action);
}

