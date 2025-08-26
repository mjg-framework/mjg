package com.example.mjg.utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ReflectionUtils {
    public static Type getSuperclassType(Class<?> subclass, Class<?> target) {
        Type superClass = subclass.getGenericSuperclass();

        while (superClass != null) {
            if (superClass instanceof ParameterizedType parameterizedType) {
                Type rawType = parameterizedType.getRawType();
                if (rawType instanceof Class<?> rawClass) {
                    if (target.isAssignableFrom(rawClass)) {
                        return superClass;
                    }
                    // Go up the inheritance chain
                    superClass = rawClass.getGenericSuperclass();
                } else {
                    throw new RuntimeException("Could not cast raw type to Class<?>: " + rawType);
                }
            } else if (superClass instanceof Class<?> superClassRaw) {
                // Not parameterized (not generic-originated), continue up
                superClass = superClassRaw.getGenericSuperclass();
            } else {
                // Reach Object or unknown type
                break;
            }
        }

        return null;
    }
}
