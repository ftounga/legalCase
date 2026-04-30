package fr.ailegalcase.immigration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-172 / SF-172-01 — vérifie les libellés des événements déclencheurs immigration FR
 * après élargissement aux faits imminents documentés.
 *
 * <p>Quatre codes ont vu leur eventLabel élargi pour inclure la version "imminente"
 * (soutenance programmée, mariage publié, CDI signé non commencé, naissance/reconnaissance).
 * Les 6 autres codes (factuels par nature) restent inchangés.
 */
class ImmigrationTriggerEventReferentialTest {

    // ---------- C3 — 4 libellés élargis ----------

    @Test
    void resolve_doctoratObtenu_returnsExpandedLabel() {
        var def = ImmigrationTriggerEventReferential.resolve(ImmigrationTriggerEventCode.DOCTORAT_OBTENU);
        assertThat(def).isPresent();
        assertThat(def.get().eventLabel())
                .isEqualTo("Doctorat obtenu ou soutenance programmée en France");
    }

    @Test
    void resolve_mariageRessortissantFr_returnsExpandedLabel() {
        var def = ImmigrationTriggerEventReferential.resolve(ImmigrationTriggerEventCode.MARIAGE_RESSORTISSANT_FR);
        assertThat(def).isPresent();
        assertThat(def.get().eventLabel())
                .isEqualTo("Mariage célébré ou publié avec un ressortissant français");
    }

    @Test
    void resolve_cdiObtenuSalarie_returnsExpandedLabel() {
        var def = ImmigrationTriggerEventReferential.resolve(ImmigrationTriggerEventCode.CDI_OBTENU_SALARIE);
        assertThat(def).isPresent();
        assertThat(def.get().eventLabel())
                .isEqualTo("CDI signé (même non commencé)");
    }

    @Test
    void resolve_naissanceEnfantFr_returnsExpandedLabel() {
        var def = ImmigrationTriggerEventReferential.resolve(ImmigrationTriggerEventCode.NAISSANCE_ENFANT_FR);
        assertThat(def).isPresent();
        assertThat(def.get().eventLabel())
                .isEqualTo("Naissance ou reconnaissance imminente d'un enfant français");
    }

    // ---------- C4 — 6 libellés inchangés (factuels par nature) ----------

    @Test
    void resolve_pacsRessortissantFr_labelUnchanged() {
        var def = ImmigrationTriggerEventReferential.resolve(ImmigrationTriggerEventCode.PACS_RESSORTISSANT_FR);
        assertThat(def).isPresent();
        assertThat(def.get().eventLabel())
                .isEqualTo("PACS avec un ressortissant français (communauté de vie > 1 an)");
    }

    @Test
    void resolve_entreeLegale10Ans_labelUnchanged() {
        var def = ImmigrationTriggerEventReferential.resolve(ImmigrationTriggerEventCode.ENTREE_LEGALE_10ANS);
        assertThat(def).isPresent();
        assertThat(def.get().eventLabel())
                .isEqualTo("Présence régulière en France depuis 10 ans ou plus");
    }

    @Test
    void resolve_violencesConjugalesConstatees_labelUnchanged() {
        var def = ImmigrationTriggerEventReferential.resolve(ImmigrationTriggerEventCode.VIOLENCES_CONJUGALES_CONSTATEES);
        assertThat(def).isPresent();
        assertThat(def.get().eventLabel())
                .isEqualTo("Violences conjugales constatées (conjoint victime)");
    }

    @Test
    void resolve_demandeAsileAccordeeOfpra_labelUnchanged() {
        var def = ImmigrationTriggerEventReferential.resolve(ImmigrationTriggerEventCode.DEMANDE_ASILE_ACCORDEE_OFPRA);
        assertThat(def).isPresent();
        assertThat(def.get().eventLabel())
                .isEqualTo("Protection internationale accordée (OFPRA ou CNDA)");
    }

    @Test
    void resolve_enfantNeFr13AnsPresence_labelUnchanged() {
        var def = ImmigrationTriggerEventReferential.resolve(ImmigrationTriggerEventCode.ENFANT_NE_FR_13ANS_PRESENCE);
        assertThat(def).isPresent();
        assertThat(def.get().eventLabel())
                .isEqualTo("Enfant étranger né en France et y ayant résidé depuis l'âge de 13 ans");
    }

    @Test
    void resolve_regroupementFamilialAutorise_labelUnchanged() {
        var def = ImmigrationTriggerEventReferential.resolve(ImmigrationTriggerEventCode.REGROUPEMENT_FAMILIAL_AUTORISE);
        assertThat(def).isPresent();
        assertThat(def.get().eventLabel())
                .isEqualTo("Autorisation de regroupement familial délivrée");
    }
}
