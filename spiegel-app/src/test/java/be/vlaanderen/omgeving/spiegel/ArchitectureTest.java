package be.vlaanderen.omgeving.spiegel;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Enforces the dependency rules of the hexagonal design (NFR-Q-01, see
 * {@code docs/analysis/05-architecture/module-structure.md}).
 */
@AnalyzeClasses(packages = "be.vlaanderen.omgeving.spiegel", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule coreIsFrameworkFree = noClasses()
            .that().resideInAPackage("..spiegel.core..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.servlet..",
                    "..spiegel.adapter..")
            .because("the domain core must not depend on frameworks or adapters");

    @ArchTest
    static final ArchRule adaptersAreIndependent = slices()
            .matching("be.vlaanderen.omgeving.spiegel.adapter.(*)..")
            .should().notDependOnEachOther()
            .allowEmptyShould(true)
            .because("adapters depend on the core only, never on each other");
}
