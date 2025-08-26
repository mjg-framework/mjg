package com.example.mjg.processors;

import lombok.Value;

import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Value
public class MethodPrototype {
    private final TypeMirror returnType;

    private final String name;

    private final List<TypeMirror> parameterTypes;

    @Override
    public String toString() {
        AtomicInteger iVar = new AtomicInteger(1);
        return "" + returnType + "   " + name + "(\n        "
                + parameterTypes.stream().map(
                        parameterType -> "" + parameterType.toString() + " var" + iVar.getAndIncrement()
                ).collect(Collectors.joining(",\n        "))
                + "\n    )";
    }
}
