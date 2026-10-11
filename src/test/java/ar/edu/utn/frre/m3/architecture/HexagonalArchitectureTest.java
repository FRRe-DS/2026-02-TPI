package ar.edu.utn.frre.m3.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Pruebas de reglas de arquitectura hexagonal con ArchUnit (RNF-02).
 * Garantiza de forma automatizada e inviolable:
 * 1. La capa de Dominio no depende de ningún framework (Spring, JPA, Web) ni de adaptadores.
 * 2. La capa de Aplicación no depende de adaptadores concretos (Persistencia, REST, Web).
 */
@AnalyzeClasses(packages = "ar.edu.utn.frre.m3", importOptions = {ImportOption.DoNotIncludeTests.class})
public class HexagonalArchitectureTest {

    @ArchTest
    public static final ArchRule domainMustNotDependOnFrameworksOrAdapters =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "..adapter.."
                    )
                    .as("El Dominio debe ser agnóstico y libre de dependencias de Spring, JPA y adaptadores.");

    @ArchTest
    public static final ArchRule applicationMustNotDependOnAdapters =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..adapter.."
                    )
                    .as("La capa de Aplicación no debe acoplarse ni depender de adaptadores concretos.");

    @ArchTest
    public static final ArchRule portsShouldBeInterfaces =
            classes().that().resideInAPackage("..application.port..")
                    .should().beInterfaces()
                    .as("Todos los puertos definidos en application.port deben ser interfaces.");
}
