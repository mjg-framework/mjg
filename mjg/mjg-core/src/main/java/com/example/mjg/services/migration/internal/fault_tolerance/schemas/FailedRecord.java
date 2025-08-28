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

    private FailedRecordAction action = new FailedRecordAction(
        FailedRecordAction.Type.RETRY
    );

    private LocalDateTime timestamp;
}
