package com.example.mjg.utils;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ReflectionUtils {
    private ReflectionUtils() {}

//    public static Type getSuperclassType(Class<?> subclass, Class<?> target) {
//        Type superClass = subclass.getGenericSuperclass();
//
//        while (superClass != null) {
//            if (superClass instanceof ParameterizedType parameterizedType) {
//                Type rawType = parameterizedType.getRawType();
//                if (rawType instanceof Class<?> rawClass) {
//                    if (rawClass.equals(target)) {
//                        return superClass;
//                    }
//                    // Go up the inheritance chain
//                    superClass = rawClass.getGenericSuperclass();
//                } else {
//                    throw new RuntimeException("Could not cast raw type to Class<?>: " + rawType);
//                }
//            } else if (superClass instanceof Class<?> superClassRaw) {
//                // Not parameterized (not generic-originated), continue up
//                superClass = superClassRaw.getGenericSuperclass();
//            } else {
//                // Reach Object or unknown type
//                break;
//            }
//        }
//
//        return null;
//    }

    /**
     * By ChatGPT
     */
    public static ParameterizedType getResolvedSuperclassType(Class<?> subclass, Class<?> target) {
        Map<TypeVariable<?>, Type> typeVarMap = new HashMap<>();
        Type superClass = subclass.getGenericSuperclass();

        while (superClass != null) {
            if (superClass instanceof ParameterizedType pt) {
                Class<?> rawClass = (Class<?>) pt.getRawType();

                // build mapping from rawClass's type variables to actual args
                TypeVariable<?>[] typeVars = rawClass.getTypeParameters();
                Type[] actualArgs = pt.getActualTypeArguments();
                for (int i = 0; i < typeVars.length; i++) {
                    Type arg = actualArgs[i];
                    if (arg instanceof TypeVariable<?> tv && typeVarMap.containsKey(tv)) {
                        // substitute if we already know
                        arg = typeVarMap.get(tv);
                    }
                    typeVarMap.put(typeVars[i], arg);
                }

                if (rawClass.equals(target)) {
                    // substitute args here
                    Type[] resolvedArgs = new Type[actualArgs.length];
                    for (int i = 0; i < actualArgs.length; i++) {
                        Type arg = actualArgs[i];
                        if (arg instanceof TypeVariable<?> tv && typeVarMap.containsKey(tv)) {
                            resolvedArgs[i] = typeVarMap.get(tv);
                        } else {
                            resolvedArgs[i] = arg;
                        }
                    }

                    // return a new ParameterizedType with resolved args
                    final Type[] fResolvedArgs = resolvedArgs;
                    return new ParameterizedType() {
                        @Override public Type[] getActualTypeArguments() { return fResolvedArgs; }
                        @Override public Type getRawType() { return rawClass; }
                        @Override public Type getOwnerType() { return null; }
                        @Override public String toString() { return rawClass.getTypeName() + Arrays.toString(fResolvedArgs); }
                    };
                }

                superClass = rawClass.getGenericSuperclass();
            } else if (superClass instanceof Class<?> c) {
                if (c.equals(target)) {
                    return null; // raw, not parameterized
                }
                superClass = c.getGenericSuperclass();
            } else {
                break;
            }
        }

        return null;
    }

    /**
     * ChatGPT
     */
    public static Method extractMemberMethodFromLambda(Serializable lambda) {
        try {
            // 1. Get SerializedLambda via writeReplace()
            Method writeReplace = lambda.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            SerializedLambda sl = (SerializedLambda) writeReplace.invoke(lambda);

            String implClassName = sl.getImplClass().replace('/', '.');
            String implMethodName = sl.getImplMethodName();
            String implDescriptor = sl.getImplMethodSignature();

            // 2. Load the implementation class
            Class<?> implClass = Class.forName(implClassName);

            // 3. Parse descriptor with ASM
            org.objectweb.asm.Type methodType = org.objectweb.asm.Type.getMethodType(implDescriptor);
            org.objectweb.asm.Type[] argTypes = methodType.getArgumentTypes();
            org.objectweb.asm.Type returnType = methodType.getReturnType();

            // 4. Find the matching method
            for (Method m : implClass.getMethods()) { // use getDeclaredMethods() if you want private too
                if (!m.getName().equals(implMethodName)) continue;

                org.objectweb.asm.Type[] mArgTypes = org.objectweb.asm.Type.getArgumentTypes(m);
                if (mArgTypes.length != argTypes.length) continue;

                boolean match = true;
                for (int i = 0; i < argTypes.length; i++) {
                    if (!mArgTypes[i].equals(argTypes[i])) {
                        match = false;
                        break;
                    }
                }

                if (match && org.objectweb.asm.Type.getReturnType(m).equals(returnType)) {
                    return m;
                }
            }

            throw new NoSuchMethodException(
                "No matching method for " + implMethodName + " with descriptor " + implDescriptor +
                    " in " + implClassName
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to extract method from lambda", e);
        }
    }
}
