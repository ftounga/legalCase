package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-06 — vérifie que le registre déclare la branche
 * {@code licenciement-be-fermeture-entreprise:default} attendue par le
 * mapping jurisprudence.
 */
class LicenciementBeFermetureEntrepriseToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new LicenciementBeFermetureEntrepriseToolBranchRegistry()
                .knownBranches())
                .containsExactly("licenciement-be-fermeture-entreprise:default");
    }
}
