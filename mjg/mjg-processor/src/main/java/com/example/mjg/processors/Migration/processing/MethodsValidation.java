package com.example.mjg.processors.Migration.processing;

import com.example.mjg.exceptions.DuplicateDataException;
import com.example.mjg.processors.ComptimeUtils;
import com.example.mjg.processors.MethodPrototype;
import com.example.mjg.processors.Migration.ComptimeMigration;
import com.example.mjg.processors.Migration.ProcessedAnnotation;
import lombok.Getter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import com.example.mjg.storage.DataStoreRegistry;

public class MethodsValidation {
    @Getter
    private final ProcessingEnvironment processingEnv;

    @Getter
    private final ComptimeMigration comptimeMigration;

    private final TypeMirror srcStoreType;
    private final TypeMirror destStoreType;
    // private final List<TypeMirror> matchingStoreTypes;
    private final List<String> matchingStoreFQCNs;

    private final TypeMirror inputEntityType;
    private final TypeMirror outputEntityType;

    private final Elements elementUtils;
    private final Types typeUtils;

    public MethodsValidation(ProcessingEnvironment processingEnv, ComptimeMigration comptimeMigration) {
        this.processingEnv = processingEnv;
        this.comptimeMigration = comptimeMigration;

        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();

        this.srcStoreType = elementUtils.getTypeElement(
            comptimeMigration.getSrcDataStoreFQCN()
        ).asType();
        this.inputEntityType = ComptimeUtils.getDataStoreTypeArguments(elementUtils, typeUtils, srcStoreType).get(0);

        this.destStoreType = elementUtils.getTypeElement(
            comptimeMigration.getDestDataStoreFQCN()
        ).asType();
        this.outputEntityType = ComptimeUtils.getDataStoreTypeArguments(elementUtils, typeUtils, destStoreType).get(0);

        this.matchingStoreFQCNs = comptimeMigration.getPMatchWiths()
                .stream()
                .map(ProcessedAnnotation::getFQCN).toList();

        // this.matchingStoreTypes = matchingStoreFQCNs
        //         .stream()
        //         .map(
        //             fqcn -> elementUtils.getTypeElement(fqcn).asType()
        //         )
        //         .toList();
    }

    public boolean validate() {
        try {
            List<MethodPrototype> requiredMethodPrototypes = new ArrayList<>();
            requiredMethodPrototypes.addAll(
                getMatchingAndReductionMethodPrototypes()
            );
            requiredMethodPrototypes.addAll(
                getStartReductionAndTransformMethodPrototypes()
            );
            requiredMethodPrototypes.addAll(
                getDuplicateResolutionMethodPrototypes()
            );
            ensureMethodsExist(requiredMethodPrototypes);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    exception.getMessage()
            );
            return false;
        }
        return true;
    }

    private void ensureMethodsExist(List<MethodPrototype> requiredMethodPrototypes) {
        TypeElement migrationClass = comptimeMigration.getMigrationClass();

        Set<MethodPrototype> remainingMethodPrototypes = new HashSet<>(requiredMethodPrototypes);

        final Map<String, Set<MethodPrototype>> allPrototypesByNameMap = remainingMethodPrototypes
            .stream()
            .collect(
                Collectors.groupingBy(MethodPrototype::getName, Collectors.toSet())
            );

        for (Element enclosed : migrationClass.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) enclosed;
                String name = method.getSimpleName().toString();

                Set<MethodPrototype> matchingPrototypes = allPrototypesByNameMap.get(name);

                if (matchingPrototypes != null) {
                    for (MethodPrototype matchingPrototype : matchingPrototypes) {
                        if (ComptimeUtils.methodMatchesPrototypeAndIsPublic(processingEnv, matchingPrototype, method)) {
                            remainingMethodPrototypes.remove(matchingPrototype);
                        }
                    }
                }
            }
        }

        if (!remainingMethodPrototypes.isEmpty()) {
            AtomicInteger iMethod = new AtomicInteger(1);
            throw new IllegalArgumentException(
                    "Migration class " + comptimeMigration.getPMigration().getFQCN() + " is missing the following methods:\n    "
                    + String.join(
                            ",\n    ",
                            remainingMethodPrototypes.stream().map(prototype -> iMethod.getAndIncrement() + ". " + prototype.toString()).toList()
                    )
                    + "\n"
            );
        }
    }

    private List<MethodPrototype> getMatchingAndReductionMethodPrototypes() {
        return matchingStoreFQCNs
            .stream()
            .map((fqcn) -> {
                TypeMirror storeType = elementUtils.getTypeElement(fqcn).asType();
                var temp = ComptimeUtils.getDataStoreTypeArguments(elementUtils, typeUtils, storeType);
                TypeMirror storeEntityType = temp.get(0);
                TypeMirror storeFilterSetType = temp.get(2);

                String storeName = fqcn.substring(fqcn.lastIndexOf('.') + 1);
                return List.of(
                    new MethodPrototype(
                        // F matchWith...(Entity, Map<String, Object>, DataStore);
                        storeFilterSetType,

                        "matchWith" + storeName,

                        List.of(
                            inputEntityType,

                            typeUtils.getDeclaredType(
                                elementUtils.getTypeElement("java.util.Map"),
                                elementUtils.getTypeElement("java.lang.String").asType(),
                                elementUtils.getTypeElement("java.lang.Object").asType()
                            ),

                            storeType
                        )
                    ),

                    new MethodPrototype(
                        // void reduceFrom...(Map<String, Object>, List<Entity>)
                        typeUtils.getNoType(TypeKind.VOID),

                        "reduceFrom" + storeName,

                        List.of(
                            typeUtils.getDeclaredType(
                                elementUtils.getTypeElement("java.util.Map"),
                                elementUtils.getTypeElement("java.lang.String").asType(),
                                elementUtils.getTypeElement("java.lang.Object").asType()
                            ),

                            typeUtils.getDeclaredType(
                                elementUtils.getTypeElement("java.util.List"),
                                storeEntityType
                            )
                        )
                    )
                );
            })
            .flatMap(List::stream)
            .toList();
    }

    private List<MethodPrototype> getStartReductionAndTransformMethodPrototypes() {
        return List.of(
            new MethodPrototype(
                // List<Entity> transform(Map<String, Object>, Entity)
                typeUtils.getDeclaredType(
                    elementUtils.getTypeElement("java.util.List"),
                    outputEntityType
                ),

                "transform",

                List.of(
                    typeUtils.getDeclaredType(
                        elementUtils.getTypeElement("java.util.Map"),
                        elementUtils.getTypeElement("java.lang.String").asType(),
                        elementUtils.getTypeElement("java.lang.Object").asType()
                    ),

                    inputEntityType
                )
            ),

            new MethodPrototype(
                // void startReduction(Entity inputRecord, Map<String, Object> aggregates)
                typeUtils.getNoType(TypeKind.VOID),

                "startReduction",

                List.of(
                    inputEntityType,

                    typeUtils.getDeclaredType(
                        elementUtils.getTypeElement("java.util.Map"),
                        elementUtils.getTypeElement("java.lang.String").asType(),
                        elementUtils.getTypeElement("java.lang.Object").asType()
                    )
                )
            )
        );
    }

    private List<MethodPrototype> getDuplicateResolutionMethodPrototypes() {
        /*
        public List<B> handleDuplicate(
            DuplicateDataException e,
            A inputRecord,
            List<B> outputRecords, // records you returned from .transform()
            AStore inputDataStore,
            BStore outputDataStore,
            DataStoreRegistry dataStoreRegistry
        ) {
            // ...
        }
         */

        var typeListOutputRecords = typeUtils.getDeclaredType(
            elementUtils.getTypeElement("java.util.List"),
            outputEntityType
        );

        return List.of(
            new MethodPrototype(
                typeListOutputRecords,

                "handleDuplicate",

                List.of(
                    elementUtils.getTypeElement(DuplicateDataException.class.getCanonicalName()).asType(),
                    inputEntityType,
                    typeListOutputRecords,
                    srcStoreType,
                    destStoreType,
                    elementUtils.getTypeElement(DataStoreRegistry.class.getCanonicalName()).asType()
                )
            )
        );
    }
}
