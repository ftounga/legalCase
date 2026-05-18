package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-44 — tests de la cellule CA_FAM_BE / APPEL / APPELANT (Belgique) :
 * combinaison déclarée et prompt système ancré dans la procédure d'appel familial
 * belge (Code judiciaire, jugement entrepris, mise à néant).
 */
class CaFamBeAppelAppelantPromptProviderTest {

    private final CaFamBeAppelAppelantPromptProvider provider =
            new CaFamBeAppelAppelantPromptProvider();

    @Test
    void combination_isFamilyCourtOfAppealBelgiumAppealAppellant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_FAMILLE);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.BELGIQUE);
        assertThat(key.jurisdiction()).isEqualTo("CA_FAM_BE");
        assertThat(key.stage()).isEqualTo("APPEL");
        assertThat(key.position()).isEqualTo("APPELANT");
    }

    @Test
    void systemPrompt_targetsBelgianFamilyCourtOfAppealAndAppellantRole() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat de l'appelant");
        assertThat(prompt).contains("chambre de la famille de la cour d'appel");
        assertThat(prompt).contains("Code judiciaire");
        assertThat(prompt).contains("art. 1050");
        assertThat(prompt).contains("art. 1051");
        assertThat(prompt).contains("748bis");
        assertThat(prompt).contains("Code civil belge");
        assertThat(prompt).contains("jugement entrepris");
        assertThat(prompt).contains("mettre à néant");
        assertThat(prompt).contains("PROJET DE CONCLUSIONS D'APPEL");
        assertThat(prompt).contains("PAR CES MOTIFS, plaise à la Cour");
        assertThat(prompt).contains("Pièce n° X");
    }

    @Test
    void systemPrompt_hasNoFrenchLawReference() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).doesNotContain("article 954");
        assertThat(prompt).doesNotContain("INFIRMER");
        assertThat(prompt).doesNotContain("juge aux affaires familiales");
    }
}
