package com.example.mjg.processors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.*;
import java.util.stream.Collectors;

public class ComptimeUtils {
    /**
     * By Claude
     */
    public static Map<String, Object> getAllAnnotationValues(AnnotationMirror annotationMirror, ProcessingEnvironment processingEnv) {
        Map<String, Object> result = new HashMap<>();

        // Get the annotation type
        DeclaredType annotationType = annotationMirror.getAnnotationType();
        TypeElement annotationElement = (TypeElement) annotationType.asElement();

        // Get all methods (annotation parameters) from the annotation type
        for (Element element : annotationElement.getEnclosedElements()) {
            if (element.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) element;
                String paramName = method.getSimpleName().toString();

                // Try to get explicit value first
                AnnotationValue explicitValue = getExplicitValue(annotationMirror, method);

                if (explicitValue != null) {
                    result.put(paramName, convertAnnotationValue(explicitValue, processingEnv));
                } else {
                    // Get default value
                    AnnotationValue defaultValue = method.getDefaultValue();
                    if (defaultValue != null) {
                        result.put(paramName, convertAnnotationValue(defaultValue, processingEnv));
                    }
                }
            }
        }

        return result;
    }

    /**
     * By Claude
     */
    private static AnnotationValue getExplicitValue(AnnotationMirror mirror, ExecutableElement method) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
                mirror.getElementValues().entrySet()) {
            if (entry.getKey().equals(method)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * By Claude
     */
    private static Object convertAnnotationValue(AnnotationValue annotationValue, ProcessingEnvironment processingEnv) {
        Object value = annotationValue.getValue();

        // Handle different types returned by getValue()
        if (value instanceof TypeMirror) {
            // Class<?> parameters become TypeMirror
            return ((TypeMirror) value).toString();
        } else if (value instanceof VariableElement) {
            // Enum values become VariableElement
            return ((VariableElement) value).getSimpleName().toString();
        } else if (value instanceof List<?> list) {
            // Array values become List<AnnotationValue>
            return list.stream()
                    .map(item -> item instanceof AnnotationValue ?
                            convertAnnotationValue((AnnotationValue) item, processingEnv) : item)
                    .collect(Collectors.toList());
        } else if (value instanceof AnnotationMirror) {
            // Nested annotations
            return getAllAnnotationValues((AnnotationMirror) value, processingEnv);
        } else {
            // Primitive types (String, int, boolean, etc.) - these are safe
            return value;
        }
    }

    public static boolean methodMatchesPrototypeAndIsPublic(
            ProcessingEnvironment processingEnv,
            MethodPrototype prototype,
            ExecutableElement method
    ) {
        return method.getModifiers().contains(Modifier.PUBLIC) && methodMatchesPrototype(
                processingEnv,
                prototype,
                method
        );
    }

    public static boolean methodMatchesPrototype(
            ProcessingEnvironment processingEnv,
            MethodPrototype prototype,
            ExecutableElement method
    ) {
        Types typeUtils = processingEnv.getTypeUtils();

        // Check return value
        if (!typeUtils.isSameType(method.getReturnType(), prototype.getReturnType())) {
            return false;
        }

        // Check parameters
        final int N = prototype.getParameterTypes().size();
        if (N != method.getParameters().size()) {
            return false;
        }

        for (int i = 0; i < N; i++) {
            TypeMirror methodParamType = method.getParameters().get(i).asType();
            TypeMirror prototypeParamType = prototype.getParameterTypes().get(i);

            if (!typeUtils.isSameType(methodParamType, prototypeParamType)) {
                return false;
            }
        }

        return true;
    }

    public static List<? extends TypeMirror> getDataStoreTypeArguments(
        Elements elementUtils,
        Types typeUtils,
        TypeMirror storeType
    ) {
        // if (storeType.getKind() != TypeKind.DECLARED) {
        //     throw new IllegalArgumentException("not a declared type.");
        // }

        // DeclaredType declaredType = (DeclaredType) storeType;
        // TypeElement storeClass = (TypeElement) declaredType.asElement();
        // TypeMirror superType = storeClass.getSuperclass();
        // if (superType.getKind() != TypeKind.DECLARED) {
        //     throw new IllegalArgumentException("not a declared type..");
        // }
        // DeclaredType superDeclaredType = (DeclaredType) superType;

        // List<? extends TypeMirror> typeArguments = superDeclaredType.getTypeArguments();
        // if (typeArguments.size() < 3) {
        //     throw new IllegalArgumentException("not enough arguments for a DataStore??? (at least 3 iirc)");
        // }

        // return typeArguments;

        // Cre ChatGPT, this is hell
        if (storeType.getKind() != TypeKind.DECLARED) {
            throw new IllegalArgumentException("Not a declared type: " + storeType);
        }

        TypeElement rootDataStoreElement =
            elementUtils.getTypeElement("com.example.mjg.data.DataStore");
        TypeMirror rootDataStoreTypeMirror = rootDataStoreElement.asType();

        // Worklist for BFS/DFS of supertypes
        Deque<DeclaredType> worklist = new ArrayDeque<>();
        worklist.add((DeclaredType) storeType);

        while (!worklist.isEmpty()) {
            DeclaredType current = worklist.removeFirst();

            if (typeUtils.isSameType(
                typeUtils.erasure(current),
                typeUtils.erasure(rootDataStoreTypeMirror))) {
                return current.getTypeArguments();
            }

            for (TypeMirror superType : typeUtils.directSupertypes(current)) {
                if (superType.getKind() == TypeKind.DECLARED) {
                    worklist.add((DeclaredType) superType);
                }
            }
        }

        throw new IllegalArgumentException(storeType + " does not extend DataStore");
    }
}
