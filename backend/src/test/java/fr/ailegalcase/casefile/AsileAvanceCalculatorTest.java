package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AsileAvanceCalculatorTest {

    // ---- DUBLIN III ---------------------------------------------------------

    @Test
    void dublin_empreintesAutreEm_returnsRecevableTransfert() {
        AsileAvanceResult r = AsileAvanceCalculator.compute(
                "DUBLIN_III", null, null, null, true, false,
                null, null, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("RECEVABLE_TRANSFERT");
        assertThat(r.delaiInstructionMois()).isEqualTo(6.0);
        assertThat(r.baseJuridique()).contains("604/2013");
        assertThat(r.recoursPossible()).contains("suspensif").contains("15 jours");
        assertThat(r.documentsRequis()).isNotEmpty();
        assertThat(r.formule()).contains("transfert");
    }

    @Test
    void dublin_pasEmpreintes_returnsFranceCompetente() {
        AsileAvanceResult r = AsileAvanceCalculator.compute(
                "DUBLIN_III", null, null, null, false, false,
                null, null, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("FRANCE_COMPETENTE");
        assertThat(r.delaiInstructionMois()).isEqualTo(6.0);
        assertThat(r.formule()).contains("France compétente");
    }

    @Test
    void dublin_enFuite_delai18Mois() {
        AsileAvanceResult r = AsileAvanceCalculator.compute(
                "DUBLIN_III", null, null, null, true, true,
                null, null, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("RECEVABLE_TRANSFERT");
        assertThat(r.delaiInstructionMois()).isEqualTo(18.0);
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("fuite"));
    }

    // ---- PROCEDURE ACCELEREE -----------------------------------------------

    @Test
    void acceleree_paysSur_returnsApplicable() {
        AsileAvanceResult r = AsileAvanceCalculator.compute(
                "PROCEDURE_ACCELEREE", null, null, true, null, null,
                null, null, false, false, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("ACCELEREE_APPLICABLE");
        assertThat(r.delaiInstructionMois()).isEqualTo(1.5);
        assertThat(r.baseJuridique()).contains("L.531-24");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("pays"));
    }

    @Test
    void acceleree_fraudeDocumentaire_returnsApplicable() {
        AsileAvanceResult r = AsileAvanceCalculator.compute(
                "PROCEDURE_ACCELEREE", null, null, false, null, null,
                null, null, true, false, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("ACCELEREE_APPLICABLE");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("fraude"));
    }

    @Test
    void acceleree_aucunMotif_returnsNonApplicable() {
        AsileAvanceResult r = AsileAvanceCalculator.compute(
                "PROCEDURE_ACCELEREE", null, null, false, null, null,
                null, null, false, false, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("ACCELEREE_NON_APPLICABLE");
        assertThat(r.formule()).contains("normale");
    }

    // ---- REEXAMEN ----------------------------------------------------------

    @Test
    void reexamen_elementsNouveaux_dateOk_returnsRecevable() {
        AsileAvanceResult r = AsileAvanceCalculator.compute(
                "REEXAMEN", LocalDate.of(2024, 6, 1), true, null, null, null,
                null, null, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("RECEVABLE_REEXAMEN");
        assertThat(r.delaiInstructionMois()).isEqualTo(0.3);
        assertThat(r.baseJuridique()).contains("L.531-32");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("nouveaux"));
    }

    @Test
    void reexamen_pasElementsNouveaux_returnsIrrecevable() {
        AsileAvanceResult r = AsileAvanceCalculator.compute(
                "REEXAMEN", LocalDate.of(2024, 6, 1), false, null, null, null,
                null, null, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("IRRECEVABLE");
        assertThat(r.risqueRefus()).anySatisfy(rr -> assertThat(rr).contains("nouveaux"));
    }

    @Test
    void reexamen_dateAbsente_returnsIrrecevable() {
        AsileAvanceResult r = AsileAvanceCalculator.compute(
                "REEXAMEN", null, true, null, null, null,
                null, null, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("IRRECEVABLE");
        assertThat(r.risqueRefus()).anySatisfy(rr -> assertThat(rr).contains("Date"));
    }

    // ---- APATRIDIE ---------------------------------------------------------

    @Test
    void apatridie_pasExclusion_returnsRecevable() {
        AsileAvanceResult r = AsileAvanceCalculator.compute(
                "APATRIDIE", null, null, null, null, null,
                false, null, null, null, true);

        assertThat(r.verdictRecevabilite()).isEqualTo("RECEVABLE_APATRIDIE");
        assertThat(r.delaiInstructionMois()).isEqualTo(12.0);
        assertThat(r.baseJuridique()).contains("L.512-1");
    }

    @Test
    void apatridie_motifsExclusion_returnsIrrecevable() {
        AsileAvanceResult r = AsileAvanceCalculator.compute(
                "APATRIDIE", null, null, null, null, null,
                true, null, null, null, true);

        assertThat(r.verdictRecevabilite()).isEqualTo("IRRECEVABLE");
        assertThat(r.risqueRefus()).anySatisfy(rr -> assertThat(rr).contains("exclusion"));
    }

    @Test
    void apatridie_presenceIrreguliere_returnsIrrecevable() {
        AsileAvanceResult r = AsileAvanceCalculator.compute(
                "APATRIDIE", null, null, null, null, null,
                false, null, null, null, false);

        assertThat(r.verdictRecevabilite()).isEqualTo("IRRECEVABLE");
        assertThat(r.risqueRefus()).anySatisfy(rr -> assertThat(rr).contains("régulière"));
    }

    // ---- PROTECTION SUBSIDIAIRE --------------------------------------------

    @Test
    void protectionSubsidiaire_traitementsOk_pasExclusion_returnsRecevable() {
        AsileAvanceResult r = AsileAvanceCalculator.compute(
                "PROTECTION_SUBSIDIAIRE", null, null, null, null, null,
                false, true, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("RECEVABLE_PROTECTION_SUBSIDIAIRE");
        assertThat(r.delaiInstructionMois()).isEqualTo(18.0);
        assertThat(r.baseJuridique()).contains("L.512-1");
        assertThat(r.formule()).contains("4 ans");
    }

    @Test
    void protectionSubsidiaire_pasTraitements_returnsIrrecevable() {
        AsileAvanceResult r = AsileAvanceCalculator.compute(
                "PROTECTION_SUBSIDIAIRE", null, null, null, null, null,
                false, false, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("IRRECEVABLE");
    }

    @Test
    void protectionSubsidiaire_motifsExclusion_returnsIrrecevable() {
        AsileAvanceResult r = AsileAvanceCalculator.compute(
                "PROTECTION_SUBSIDIAIRE", null, null, null, null, null,
                true, true, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("IRRECEVABLE");
        assertThat(r.risqueRefus()).anySatisfy(rr -> assertThat(rr).contains("exclusion"));
    }

    // ---- Validation et erreurs ---------------------------------------------

    @Test
    void dispositifInconnu_throws() {
        assertThatThrownBy(() -> AsileAvanceCalculator.compute(
                "INCONNU", null, null, null, null, null,
                null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non supporté");
    }

    @Test
    void dispositifNull_throws() {
        assertThatThrownBy(() -> AsileAvanceCalculator.compute(
                null, null, null, null, null, null,
                null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dispositifAsile");
    }

    @Test
    void dispositifBlank_throws() {
        assertThatThrownBy(() -> AsileAvanceCalculator.compute(
                "   ", null, null, null, null, null,
                null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dispositifAsile");
    }

    // ---- Méta --------------------------------------------------------------

    @Test
    void caseInsensitive_dispositif() {
        AsileAvanceResult r = AsileAvanceCalculator.compute(
                "dublin_iii", null, null, null, true, false,
                null, null, null, null, null);

        assertThat(r.dispositifAsile()).isEqualTo("DUBLIN_III");
        assertThat(r.verdictRecevabilite()).isEqualTo("RECEVABLE_TRANSFERT");
    }

    @Test
    void formule_containsDispositifKey() {
        AsileAvanceResult dub = AsileAvanceCalculator.compute(
                "DUBLIN_III", null, null, null, true, false,
                null, null, null, null, null);
        AsileAvanceResult acc = AsileAvanceCalculator.compute(
                "PROCEDURE_ACCELEREE", null, null, true, null, null,
                null, null, false, false, null);
        AsileAvanceResult apa = AsileAvanceCalculator.compute(
                "APATRIDIE", null, null, null, null, null,
                false, null, null, null, true);
        AsileAvanceResult prs = AsileAvanceCalculator.compute(
                "PROTECTION_SUBSIDIAIRE", null, null, null, null, null,
                false, true, null, null, null);

        assertThat(dub.formule()).contains("Dublin");
        assertThat(acc.formule()).contains("Procédure accélérée");
        assertThat(apa.formule()).contains("Apatridie");
        assertThat(prs.formule()).contains("Protection subsidiaire");
    }

    @Test
    void documentsRequis_neverEmpty() {
        for (String dispositif : new String[]{"DUBLIN_III", "PROCEDURE_ACCELEREE",
                "REEXAMEN", "APATRIDIE", "PROTECTION_SUBSIDIAIRE"}) {
            AsileAvanceResult r = AsileAvanceCalculator.compute(
                    dispositif, LocalDate.of(2024, 1, 1), true, true, true, false,
                    false, true, false, false, true);
            assertThat(r.documentsRequis()).isNotEmpty();
            assertThat(r.risqueRefus()).isNotNull();
            assertThat(r.recoursPossible()).isNotBlank();
        }
    }

    @Test
    void recours_dublinSuspensif_autresCNDA() {
        AsileAvanceResult dub = AsileAvanceCalculator.compute(
                "DUBLIN_III", null, null, null, true, false,
                null, null, null, null, null);
        AsileAvanceResult acc = AsileAvanceCalculator.compute(
                "PROCEDURE_ACCELEREE", null, null, true, null, null,
                null, null, false, false, null);

        assertThat(dub.recoursPossible()).contains("suspensif");
        assertThat(acc.recoursPossible()).contains("CNDA");
    }
}
