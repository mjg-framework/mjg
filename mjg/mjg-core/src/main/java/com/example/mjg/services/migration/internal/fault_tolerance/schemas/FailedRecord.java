package com.example.mjg.services.migration.internal.fault_tolerance.schemas;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class FailedRecord implements Serializable {
    private Object id = null;

    private String description = "";

    private String cause = "";

    private String effect = "";

    /**
     * One of "RETRY" | "IGNORE"
     * | "TAKE(0)" | "TAKE(1)" | "TAKE(N-2)" | "TAKE(N-1)"
     * | ... TAKE(x) or TAKE(N - x) with x being a natural number
     * See README.md
     */
    private String action = "RETRY";

    private LocalDateTime timestamp;
}
