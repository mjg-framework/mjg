package com.example.mjg.algorithms.retrying;

import com.example.mjg.exceptions.BaseMigrationException;
import com.example.mjg.exceptions.RetriesExhaustedException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BiConsumer;

@Slf4j
@AllArgsConstructor
public class RetryLogicExecutor<T, R> {
    private final RetryableFunction<T, R> callback;
    private final RetryLogic retryLogic;

    public R apply(T arg) throws RetriesExhaustedException {
        final int retryDelayInSeconds = retryLogic.getRetryDelayInSeconds();
        final int maxRetries = retryLogic.getMaxRetries();
        final String debugContext = retryLogic.getDebugContext();
        final BiConsumer<Exception, Object>
            exceptionReporter = retryLogic.getExceptionReporter();

        long retryDelayInMilliseconds = (
            retryDelayInSeconds > 0
                ? retryDelayInSeconds * 1000L
                : 0
        );

        int retriesLeft = maxRetries;
        Exception exception = null;
        while (true) {
            try {
                return callback.apply(arg);
            } catch (Exception e) {
                exception = e;
                log.error(debugContext, e);
                --retriesLeft;
                if (retriesLeft < 0) {
                    log.debug("NO MORE RETRIES LEFT, PROPAGATING ERROR");
                    break;
                }

                log.debug("Delaying {} seconds before retrying.", retryDelayInSeconds);

                try {
                    Thread.sleep(retryDelayInMilliseconds);
                } catch (InterruptedException ignored) {}
            }
        }

        exceptionReporter.accept(exception, arg);

        throw new RetriesExhaustedException(debugContext, exception);
    }
}
