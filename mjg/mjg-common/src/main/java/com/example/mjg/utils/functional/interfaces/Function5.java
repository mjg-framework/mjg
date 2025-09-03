package com.example.mjg.utils.functional.interfaces;

import java.io.Serializable;

@FunctionalInterface
public interface Function5<P1, P2, P3, P4, P5, R> extends Serializable {
    R apply(P1 param1, P2 param2, P3 param3, P4 param4, P5 param5);
}
