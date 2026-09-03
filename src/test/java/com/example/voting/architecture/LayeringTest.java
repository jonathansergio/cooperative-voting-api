package com.example.voting.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.time.Instant;
import org.springframework.web.bind.annotation.RestController;

/**
 * Guards the direction of the dependencies described in ADR 0001. The layering is a convention the
 * compiler cannot check, so it is checked here instead.
 */
@AnalyzeClasses(packages = "com.example.voting", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeringTest {

    @ArchTest
    static final ArchRule repositories_stay_underneath = noClasses()
            .that()
            .haveSimpleNameEndingWith("Repository")
            .should()
            .dependOnClassesThat()
            .haveSimpleNameEndingWith("Controller")
            .orShould()
            .dependOnClassesThat()
            .haveSimpleNameEndingWith("Service")
            .because("a repository only reads and writes data");

    @ArchTest
    static final ArchRule services_do_not_reach_up_into_http = noClasses()
            .that()
            .haveSimpleNameEndingWith("Service")
            .should()
            .dependOnClassesThat()
            .haveSimpleNameEndingWith("Controller")
            .because("business logic must not know how it is exposed")
            // No service exists yet; the rule is in place so the first one cannot break it.
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule nothing_outside_a_controller_speaks_http = noClasses()
            .that()
            .haveSimpleNameNotEndingWith("Controller")
            .and()
            .resideOutsideOfPackage("com.example.voting.shared.errors")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("org.springframework.web.server.ResponseStatusException")
            .because("the domain raises domain exceptions; only the error handler maps them to HTTP");

    @ArchTest
    static final ArchRule controllers_are_named_as_such = classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .should()
            .haveSimpleNameEndingWith("Controller")
            .because("the naming is what the other rules key on");

    @ArchTest
    static final ArchRule time_is_read_from_the_injected_clock = noClasses()
            .should()
            .callMethod(Instant.class, "now")
            .because("time comes from the injected Clock, so time-dependent rules stay testable");

    @ArchTest
    static final ArchRule dependencies_arrive_through_the_constructor = fields().should()
            .notBeAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
            .because("field injection hides dependencies and blocks plain construction in tests");
}
