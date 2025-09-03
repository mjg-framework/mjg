package com.example.mjg.utils.functional.interfaces;

import java.io.Serializable;

@FunctionalInterface
public interface Function3<P1, P2, P3, R> extends Serializable {
    R apply(P1 param1, P2 param2, P3 param3);
}
