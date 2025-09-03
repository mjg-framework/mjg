package com.example.mjg.algorithms.retrying;

import com.example.mjg.exceptions.RetriesExhaustedException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BiConsumer;

@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class RetryLogic {
    private int maxRetries;

    private int retryDelayInSeconds;

    private String debugContext;

    /**
     * Gets two arguments: the exception thrown,
     * and the argument passed to the callback
     * (of data type T).
     */
    private BiConsumer<Exception, Object> exceptionReporter;

    public static MaxRetriesStep maxRetries(int maxRetries) {
        return Builder.maxRetries(maxRetries);
    }

    public <T, R> RetryLogicExecutor<T, R> withCallback(RetryableFunction<T, R> callback) {
        return new RetryLogicExecutor<>(callback, this);
    }

    public void run(RetryableRunnable callback)
    throws RetriesExhaustedException {
        var executor = new RetryLogicExecutor<>(
            (Object ignored) -> { callback.run(); return null; },
            this
        );
        executor.apply(null);
    }

    /////////////////////////////////////////////////////
    ///////////// THE STEP BUILDER PATTERN //////////////
    /////////////////////////////////////////////////////

    public interface MaxRetriesStep {
        RetryDelayInSecondsStep retryDelayInSeconds(int retryDelayInSeconds);
    }
    public interface RetryDelayInSecondsStep {
        ExceptionReporterStep exceptionReporter(
            BiConsumer<Exception, Object> exceptionReporter
        );
    }
    public interface ExceptionReporterStep {
        RetryLogic debugContext(String debugContext);
        RetryLogic noDebugContext();
    }

    private static class Builder
    implements
        MaxRetriesStep,
        RetryDelayInSecondsStep,
        ExceptionReporterStep
    {
        private Builder copy() {
            Builder newBuilder = new Builder(maxRetries);
            newBuilder.retryDelayInSeconds = retryDelayInSeconds;
            newBuilder.debugContext = debugContext;
            newBuilder.exceptionReporter = exceptionReporter;
            return newBuilder;
        }

        private final int maxRetries;
        private int retryDelayInSeconds = 0;
        private String debugContext = null;
        private BiConsumer<Exception, Object> exceptionReporter = null;

        private Builder(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public static MaxRetriesStep maxRetries(int maxRetries) {
            return new Builder(maxRetries);
        }

        @Override
        public RetryDelayInSecondsStep retryDelayInSeconds(int retryDelayInSeconds) {
            this.retryDelayInSeconds = retryDelayInSeconds;
            return this.copy();
        }

        @Override
        public ExceptionReporterStep exceptionReporter(
            BiConsumer<Exception, Object> exceptionReporter
        ) {
            this.exceptionReporter = exceptionReporter;
            return this.copy();
        }

        @Override
        public RetryLogic debugContext(String debugContext) {
            this.debugContext = debugContext;
            return convert();
        }
        @Override
        public RetryLogic noDebugContext() {
            this.debugContext = "(no debug context)";
            return convert();
        }

        private RetryLogic convert() {
            return new RetryLogic(
                maxRetries,
                retryDelayInSeconds,
                debugContext,
                exceptionReporter
            );
        }
    }
}
