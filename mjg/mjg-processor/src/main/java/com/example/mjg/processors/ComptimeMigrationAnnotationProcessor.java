package com.example.mjg.processors;


import com.example.mjg.processors.Migration.*;
import com.example.mjg.processors.Migration.processing.DataStoreNamesValidation;
import com.example.mjg.processors.Migration.processing.DependencyResolution;
import com.example.mjg.processors.Migration.processing.MethodsValidation;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import java.util.*;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes("com.example.mjg.annotations.Migration")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class ComptimeMigrationAnnotationProcessor extends AbstractProcessor {
    final Set<TypeElement> migrationClasses = new HashSet<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.NOTE,
                getClass().getCanonicalName() + ": discovering annotated elements..."
        );

        for (TypeElement annotation : annotations) {
//            if (!annotation.getQualifiedName().contentEquals(Migration.class.getCanonicalName())) {
//                continue;
//            }
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);

            for (Element element : elements) {
                if (element.getKind() == ElementKind.CLASS) {
                    TypeElement typeElement = (TypeElement) element;
                    migrationClasses.add(typeElement);

                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.NOTE,
                            "Found @Migration on class: " + typeElement.getQualifiedName().toString()
                    );
                } else {
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            "@Migration can only be applied to a class, not " + element.getKind()
                    );
                }
            }
        }

        if (roundEnv.processingOver()) {
            DependencyResolution resolver = new DependencyResolution(processingEnv);
            List<ComptimeMigration> sortedComptimeMigrations = resolver.resolve(migrationClasses);

            if (sortedComptimeMigrations != null) {
                sortedComptimeMigrations.forEach(comptimeMigration -> {
                    MethodsValidation methodsValidator = new MethodsValidation(processingEnv, comptimeMigration);
                    methodsValidator.validate();

                    DataStoreNamesValidation dataStoreNamesValidator = new DataStoreNamesValidation(processingEnv, comptimeMigration);
                    dataStoreNamesValidator.validate();
                });
            }
        }

        return true;
    }

}
