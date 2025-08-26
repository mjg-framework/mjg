package com.example.mjg.utils;

import lombok.Value;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Value
public class RMethodSignature {
    private final String name;

    private final List<Class<?>> parameterTypes;

    @Override
    public String toString() {
        AtomicInteger iVar = new AtomicInteger(1);
        return "<unknown_return_value> " + name + "(\n        "
                + parameterTypes.stream().map(
                        parameterType -> "" + parameterType.toString() + " var" + iVar.getAndIncrement()
                ).collect(Collectors.joining(",\n        "))
                + "\n    )";
    }
}
