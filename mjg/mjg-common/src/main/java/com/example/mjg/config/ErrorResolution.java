package com.example.mjg.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ErrorResolution {
    /**
     * The resolution strategy when retried
     * enough times and errors still occur.
     *
     * No matter the strategy, we will always
     * report the incident. So the "REPORT"
     * words below sound redundant, but we
     * wanted to be explicit.
     */
    public static enum Strategy {
        /**
         * The strongest, strictest option.
         *
         * Stop the migration process altogether,
         * meaning that any later migrations
         * will not run.
         *
         * The incident will be reported.
         */
        STOP_IMMEDIATELY_AND_REPORT,

        /**
         * This is usually preferred. Finish
         * migrating all the other records of the
         * current migration, then stop and
         * report.
         */
        FINISH_THE_MIGRATION_THEN_STOP_AND_REPORT,

        /**
         * Report, then just proceed as usual.
         */
        REPORT_AND_PROCEED
    }

    Strategy strategy();

    int retryTimes() default 1;

    int retryDelayInSeconds() default 3;
}
