package com.brasper.platform;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Enforces the layering rules from AGENTS.md. Rules pass vacuously until the packages exist. */
@AnalyzeClasses(packages = "com.brasper.platform", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainDoesNotDependOnOuterLayersOrSpring = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application..", "..infrastructure..", "..interfaces..", "org.springframework..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule applicationDoesNotDependOnInfrastructureOrInterfaces = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage("..infrastructure..", "..interfaces..")
            .allowEmptyShould(true);

    private static final String[] CONTEXTS = {
        "iam", "projects", "billing", "expenses", "staff", "treasury", "accounting", "reporting"};

    /**
     * A context never uses another context's repositories, persistence or services: it goes through the
     * {@code interfaces.acl} facade or a domain event.
     */
    @ArchTest
    static void contextsDoNotReachIntoEachOthersInternals(JavaClasses classes) {
        for (String owner : CONTEXTS) {
            noClasses()
                    .that().resideOutsideOfPackages("com.brasper.platform." + owner + "..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.brasper.platform." + owner + ".domain.repositories..",
                            "com.brasper.platform." + owner + ".infrastructure..",
                            "com.brasper.platform." + owner + ".application..")
                    .allowEmptyShould(true)
                    .because("only the ACL facade and the domain events of '" + owner + "' are public")
                    .check(classes);
        }
    }
}
