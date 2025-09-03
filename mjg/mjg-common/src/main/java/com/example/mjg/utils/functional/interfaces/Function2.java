package com.example.mjg.utils.functional.interfaces;

import java.io.Serializable;

@FunctionalInterface
public interface Function2<P1, P2, R> extends Serializable {
    R apply(P1 param1, P2 param2);
}
