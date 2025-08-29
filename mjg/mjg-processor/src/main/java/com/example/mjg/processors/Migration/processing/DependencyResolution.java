package com.example.mjg.processors.Migration.processing;

import com.example.mjg.algorithms.super_topo_sort.SuperEdge;
import com.example.mjg.algorithms.super_topo_sort.SuperTopoSort;
import com.example.mjg.annotations.*;
import com.example.mjg.processors.ComptimeUtils;
import com.example.mjg.processors.Migration.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@AllArgsConstructor
public class DependencyResolution {
    @Getter
    private final ProcessingEnvironment processingEnv;

    /**
     * Constructs migration execution plan/ordering.
     */
    public List<ComptimeMigration> resolve(Set<TypeElement> migrationClasses) {
        Map<Integer, List<ComptimeMigration>> migrationsByOrderMap = new HashMap<>();

        boolean atLeastOneMigrationIsInvalid = false;

        for (TypeElement migrationClass : migrationClasses) {
            Map<String, Object> valuesAnnMigration = null;
            Map<String, Object> valuesAnnForEachRecordFrom = null;
            List<Map<String, Object>> valuesAnnsMatchWith = new ArrayList<>();
            Map<String, Object> valuesAnnTransformAndSaveTo = null;
            boolean thisMigrationIsInvalid = false;

            for (AnnotationMirror annotationMirror : migrationClass.getAnnotationMirrors()) {
                DeclaredType annotationType = annotationMirror.getAnnotationType();
                Name annotationQualifiedName = ((TypeElement) annotationType.asElement()).getQualifiedName();

                Map<String, Object> annotationValues = ComptimeUtils.getAllAnnotationValues(annotationMirror, processingEnv);

                if (annotationQualifiedName.contentEquals(Migration.class.getCanonicalName())) {
                    if (valuesAnnMigration == null) {
                        valuesAnnMigration = annotationValues;
                    } else {
                        processingEnv.getMessager().printMessage(
                                Diagnostic.Kind.ERROR,
                                "@Migration could only be applied to a class once"
                        );
                        thisMigrationIsInvalid = true;
                    }

                } else if (annotationQualifiedName.contentEquals(ForEachRecordFrom.class.getCanonicalName())) {
                    if (valuesAnnForEachRecordFrom == null) {
                        valuesAnnForEachRecordFrom = annotationValues;
                    } else {
                        processingEnv.getMessager().printMessage(
                                Diagnostic.Kind.ERROR,
                                "A @Migration must have exactly one @ForEachRecordFrom annotation applied"
                        );
                        thisMigrationIsInvalid = true;
                    }

                } else if (annotationQualifiedName.contentEquals(MatchWith.class.getCanonicalName())) {
                    valuesAnnsMatchWith.add(annotationValues);

                } else if (annotationQualifiedName.contentEquals(MatchWithEntries.class.getCanonicalName())) {
                    Object valueArray = annotationValues.get("value");
                    boolean ok = false;
                    if (valueArray instanceof List<?> matchWithMirrors) {
                        ok = true;
                        for (Object item : matchWithMirrors) {
                            boolean thisItemIsOk = false;

                            if (item instanceof Map<?,?> map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> matchWithValues =  (Map<String, Object>) map;
                                valuesAnnsMatchWith.add((Map<String, Object>) matchWithValues);
                                thisItemIsOk = true;
                            }

                            if (ok && !thisItemIsOk) {
                                ok = false;
                            }
                        }
                    }

                    if (!ok) {
                        processingEnv.getMessager().printMessage(
                                Diagnostic.Kind.ERROR,
                                "An empty or malformed @MatchWithEntries annotation applied"
                        );
                        thisMigrationIsInvalid = true;
                    }

                } else if (annotationQualifiedName.contentEquals(TransformAndSaveTo.class.getCanonicalName())) {
                    if (valuesAnnTransformAndSaveTo == null) {
                        valuesAnnTransformAndSaveTo = annotationValues;
                    } else {
                        processingEnv.getMessager().printMessage(
                                Diagnostic.Kind.ERROR,
                                "A @Migration must have exactly one @TransformAndSaveTo annotation applied"
                        );
                        thisMigrationIsInvalid = true;
                    }

                }
            }

            if (valuesAnnMigration == null) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "No @Migration annotation applied to this class. This should never happen, but isn't your fault."
                );
                thisMigrationIsInvalid = true;
            }

            if (valuesAnnForEachRecordFrom == null) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "A @Migration must have a @ForEachRecordFrom annotation applied"
                );
                thisMigrationIsInvalid = true;
            }

            if (valuesAnnTransformAndSaveTo == null) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "A @Migration must have a @TransformAndSaveTo annotation applied"
                );
                thisMigrationIsInvalid = true;
            }

            if (thisMigrationIsInvalid) {
                atLeastOneMigrationIsInvalid = true;
            }

            if (atLeastOneMigrationIsInvalid) {
                migrationsByOrderMap.clear();
            } else {
                // Only try to build the task graph
                // when the migrations are not (or
                // have not been) invalid.
                ComptimeMigration m = createComptimeMigration(
                        migrationClass,
                        valuesAnnMigration,
                        valuesAnnForEachRecordFrom,
                        valuesAnnsMatchWith,
                        valuesAnnTransformAndSaveTo
                );
                if (m == null) {
                    atLeastOneMigrationIsInvalid = true;
                } else {
                    Integer migrationOrder = m.getPMigration().getOrder();
                    List<ComptimeMigration> migrationsOfTheSameOrder = migrationsByOrderMap.computeIfAbsent(
                            migrationOrder,
                            k -> new ArrayList<>()
                    );
                    migrationsOfTheSameOrder.add(m);
                }
            } // if (atLeastOneMigrationIsInvalid)
        }

        if (!atLeastOneMigrationIsInvalid) {
            // Build the task graph, or more accurately,
            // a topologically sorted list of tasks/migrations.
            List<ComptimeMigration> sortedComptimeMigrations = buildMigrationExecutionPlan(migrationsByOrderMap);

            if (sortedComptimeMigrations == null) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Stopped building migration execution plan earlier.."
                );
            } else {
                List<RuntimeMigration> sortedRuntimeMigrations = sortedComptimeMigrations.stream()
                        .map(
                                comptimeMigration -> new RuntimeMigration(
                                        comptimeMigration.getPMigration().getFQCN()
                                )
                        )
                        .toList();

                boolean successful = embedTaskGraph(sortedRuntimeMigrations);
                if (successful) {
                    return sortedComptimeMigrations;
                }
            }
        } else {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "At least one Migration is invalid."
            );
        }

        return null;
    }

    private ComptimeMigration createComptimeMigration(
            TypeElement migrationClass,
            Map<String, Object> valuesAnnMigration,
            Map<String, Object> valuesAnnForEachRecordFrom,
            List<Map<String, Object>> valuesAnnsMatchWith,
            Map<String, Object> valuesAnnTransformAndSaveTo
    ) {
        boolean migrationIsInvalid = false;

        PMigration pMigration = null;
        {
            String migrationClassFQCN = migrationClass.getQualifiedName().toString();
            Integer order = (Integer) valuesAnnMigration.get("order");
            if (order == null) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "@Migration annotation has no value for migration order???"
                );
                migrationIsInvalid = true;
            } else {
                pMigration = new PMigration(migrationClassFQCN, valuesAnnMigration, order);
            }
        }


        PForEachRecordFrom pForEachRecordFrom = null;
        {
            String srcDataStoreFQCN = (String) valuesAnnForEachRecordFrom.get("value");
            if (srcDataStoreFQCN == null) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "@ForEachRecordFrom annotation has no value for data store class???"
                );
                migrationIsInvalid = true;
            } else {
                pForEachRecordFrom = new PForEachRecordFrom(srcDataStoreFQCN, valuesAnnForEachRecordFrom);
            }
        }

        List<PMatchWith> pMatchWiths = new ArrayList<>(valuesAnnsMatchWith.size());
        {
            List<String> matchingDataStoreFQCNs = new ArrayList<>(valuesAnnsMatchWith.size());
            boolean someMatchWithsAreInvalid = false;
            for (Map<String, Object> map : valuesAnnsMatchWith) {
                String matchingDataStoreFQCN = (String) map.get("value");
                if (matchingDataStoreFQCN == null) {
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            "@MatchWith annotation has no value for data store class???"
                    );
                    migrationIsInvalid = true;
                    someMatchWithsAreInvalid = true;
                } else {
                    if (!someMatchWithsAreInvalid) {
                        matchingDataStoreFQCNs.add(matchingDataStoreFQCN);
                    }
                }
            }

            for (int i = 0; i < matchingDataStoreFQCNs.size(); i++) {
                String matchingDataStoreFQCN = matchingDataStoreFQCNs.get(i);
                Map<String, Object> map = valuesAnnsMatchWith.get(i);
                PMatchWith pMatchWith = new PMatchWith(matchingDataStoreFQCN, map);
                pMatchWiths.add(pMatchWith);
            }
        }

        PTransformAndSaveTo pTransformAndSaveTo = null;
        {
            String destDataStoreFQCN = (String) valuesAnnTransformAndSaveTo.get("value");
            if (destDataStoreFQCN == null) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "@TransformAndSaveTo annotation has no value for data store class???"
                );
                migrationIsInvalid = true;
            } else {
                pTransformAndSaveTo = new PTransformAndSaveTo(destDataStoreFQCN, valuesAnnTransformAndSaveTo);
            }
        }

        if (migrationIsInvalid) {
            return null;
        }

        return new ComptimeMigration(
                migrationClass,
                pMigration,
                pForEachRecordFrom,
                pMatchWiths,
                pTransformAndSaveTo
        );
    }

    private boolean embedTaskGraph(List<RuntimeMigration> sortedMigrations) {
        String source = "package " + RuntimeMigrationDataLocation.PACKAGE_NAME + ";"
                + "import java.util.List;"
                + "import " + RuntimeMigration.class.getCanonicalName() + ";"
                + "public final class " + RuntimeMigrationDataLocation.CLASS_NAME + "{"
                + """
            public static final List<RuntimeMigration> sortedMigrations = List.of(
        """ +
                sortedMigrations.stream()
                        .map(RuntimeMigration::repr)
                        .collect(Collectors.joining(",\n"))
                + """
            );
        }
        """;

        try {
            JavaFileObject file = processingEnv.getFiler()
                    .createSourceFile(RuntimeMigrationDataLocation.FQCN);

            try (Writer writer = file.openWriter()) {
                writer.write(source);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Could not create source file: "
                            + RuntimeMigrationDataLocation.FQCN
                            + "\n\nOriginal exception:"
                            + e.getMessage()
            );
            return false;
        }
        return true;
    }

    private List<ComptimeMigration> buildMigrationExecutionPlan(Map<Integer, List<ComptimeMigration>> migrationsByOrderMap) {
        List<ComptimeMigration> sortedMigrations = new ArrayList<>(migrationsByOrderMap.size());

        List<Map.Entry<Integer, List<ComptimeMigration>>> migrationEntriesSortedByMigrationOrder = migrationsByOrderMap
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();

        for (Map.Entry<Integer, List<ComptimeMigration>> entry : migrationEntriesSortedByMigrationOrder) {
            List<ComptimeMigration> sortedSameOrderMigrations = buildMigrationExecutionPlan(
                    entry.getValue()
            );
            if (sortedSameOrderMigrations == null) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Stopped building migration execution plan earlier.\nNO MIGRATIONS' ORDER OF EXECUTION ESTABLISHED"
                );
                return null;
            }
            sortedMigrations.addAll(
                    sortedSameOrderMigrations
            );

            AtomicInteger counter = new AtomicInteger(0);
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.NOTE,
                "MIGRATIONS' ORDER OF EXECUTION:\n        "
                    + String.join(
                    ",\n        ",
                    sortedMigrations.stream().map(
                        comptimeMigration -> {
                            return counter.incrementAndGet() + ". "
                                + comptimeMigration.getPMigration().getFQCN();
                        }
                    ).toList()
                )
            );
        }

        return sortedMigrations;
    }

    private List<ComptimeMigration> buildMigrationExecutionPlan(List<ComptimeMigration> migrations) {
        Map<SuperEdge<String>, ComptimeMigration> edgeToMigrationMap = new HashMap<>();

        List<SuperEdge<String>> allEdges = migrations.stream().map(
                m -> {
                    Set<String> startEdges = new HashSet<>();
                    startEdges.add(m.getSrcDataStoreFQCN());
                    startEdges.addAll(m.getMatchingDataStoreFQCNs());

                    Set<String> endEdges = Set.of(m.getDestDataStoreFQCN());

                    SuperEdge<String> edge = new SuperEdge<>(
                            startEdges,
                            endEdges
                    );

                    edgeToMigrationMap.put(edge, m);

                    return edge;
                }
        ).toList();

        List<SuperEdge<String>> sortedEdges;
        try {
            SuperTopoSort<String> sorter = new SuperTopoSort<>(allEdges);
            sortedEdges = sorter.toSorted();
        } catch (IllegalArgumentException e) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Cycle detected while resolving migrations' dependency.\n"
                            + "Check that there are no loops, no mutual dependencies\n"
                            + "(e.g. A -> B and B -> A), and most importantly, there\n"
                            + "must be no two migration with the same source and/or\n"
                            + "target data stores (which are defined in @ForEachRecordFrom\n"
                            + "and @TransformAndSaveTo)\n"
                            + "\n\n"
                            + "Original exception: "
                            + e
            );
            return null;
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "An unexpected error occurred while resolving migrations' dependency. "
                            + "This is an internal error, not your fault. "
                            + "\n\n\n"
                            + "Original exception: "
                            + e
            );
            return null;
        }

        List<ComptimeMigration> sortedMigrations = sortedEdges.stream().map(
                e -> edgeToMigrationMap.getOrDefault(e, null)
        ).toList();

        if (sortedMigrations.stream().anyMatch(Objects::isNull)) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Could not remap sorted migrations. This is an internal error, not your fault."
            );
            return null;
        }

        return sortedMigrations;
    }
}
