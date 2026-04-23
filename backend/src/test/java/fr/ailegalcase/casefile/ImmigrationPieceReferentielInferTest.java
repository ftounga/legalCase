package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SF-IM-01-04 : tests du mapping {@link ImmigrationPieceReferentiel#inferChecklistType}.
 */
class ImmigrationPieceReferentielInferTest {

    @Test
    void U01_mariage_FR_CST_VPF_conjoint_FR() {
        // Cas CHEN : carte pluriannuelle étudiant, mariage → cible CST_VPF via L.423-1
        String result = ImmigrationPieceReferentiel.inferChecklistType(
                "CARTE_PLURIANNUELLE", "CST_VPF", "MARIAGE_RESSORTISSANT_FR");
        assertThat(result).isEqualTo("CST_VPF_CONJOINT_FR");
    }

    @Test
    void U01bis_naissance_enfant_FR_parent_enfant() {
        String result = ImmigrationPieceReferentiel.inferChecklistType(
                "VLS_TS_ETUDIANT", "CST_VPF", "NAISSANCE_ENFANT_FR");
        assertThat(result).isEqualTo("CST_VPF_PARENT_ENFANT_FR");
    }

    @Test
    void U01ter_regroupement_autorise_regroupementFamilial() {
        String result = ImmigrationPieceReferentiel.inferChecklistType(
                "VLS_TS_ETUDIANT", "CST_VPF", "REGROUPEMENT_FAMILIAL_AUTORISE");
        assertThat(result).isEqualTo("REGROUPEMENT_FAMILIAL");
    }

    @Test
    void U02_refus_renouvellement_CST_SALARIE_titreSalarie() {
        // Cas AMADOU : CST_SALARIE en cours, pas de cible → titre salarié
        String result = ImmigrationPieceReferentiel.inferChecklistType(
                "CST_SALARIE", null);
        assertThat(result).isEqualTo("TITRE_SALARIE");
    }

    @Test
    void U03_vls_ts_etudiant_sansCible_visaEtudiant() {
        String result = ImmigrationPieceReferentiel.inferChecklistType(
                "VLS_TS_ETUDIANT", null);
        assertThat(result).isEqualTo("VISA_ETUDIANT");
    }

    @Test
    void U04_pluriannuel_sansCible_visaEtudiant_parDefaut() {
        // CARTE_PLURIANNUELLE → VISA_ETUDIANT en V1 (cas majoritaire)
        String result = ImmigrationPieceReferentiel.inferChecklistType(
                "CARTE_PLURIANNUELLE", null);
        assertThat(result).isEqualTo("VISA_ETUDIANT");
    }

    @Test
    void U05_carteResident_cible_carte10ans() {
        // Titre cible CARTE_RESIDENT → checklist CARTE_RESIDENT_10ANS (pas NATURALISATION)
        String result = ImmigrationPieceReferentiel.inferChecklistType(
                "CST_SALARIE", "CARTE_RESIDENT", null);
        assertThat(result).isEqualTo("CARTE_RESIDENT_10ANS");
    }

    @Test
    void U06_carteResident_actuel_carte10ans() {
        String result = ImmigrationPieceReferentiel.inferChecklistType(
                "CARTE_RESIDENT", null, null);
        assertThat(result).isEqualTo("CARTE_RESIDENT_10ANS");
    }

    @Test
    void U06bis_entree10ans_event_carte10ans() {
        String result = ImmigrationPieceReferentiel.inferChecklistType(
                "CARTE_PLURIANNUELLE", null, "ENTREE_LEGALE_10ANS");
        assertThat(result).isEqualTo("CARTE_RESIDENT_10ANS");
    }

    @Test
    void U06ter_doctorat_event_passeportTalent() {
        String result = ImmigrationPieceReferentiel.inferChecklistType(
                "CARTE_PLURIANNUELLE", null, "DOCTORAT_OBTENU");
        assertThat(result).isEqualTo("PASSEPORT_TALENT");
    }

    @Test
    void U07_cstSalarie_cible_prioriteSurActuel() {
        // Titre cible prime sur actuel
        String result = ImmigrationPieceReferentiel.inferChecklistType(
                "VLS_TS_ETUDIANT", "CST_SALARIE");
        assertThat(result).isEqualTo("TITRE_SALARIE");
    }

    @Test
    void U08_aucuneInfo_null() {
        assertThat(ImmigrationPieceReferentiel.inferChecklistType(null, null)).isNull();
        assertThat(ImmigrationPieceReferentiel.inferChecklistType("", "")).isNull();
    }

    @Test
    void U09_codeInconnu_null() {
        String result = ImmigrationPieceReferentiel.inferChecklistType(
                "TITRE_INCONNU_XXX", null);
        assertThat(result).isNull();
    }

    @Test
    void U10_cstVpf_actuel_sansEvent_null_car_ambigu() {
        // CST_VPF existant sans événement déclencheur → ambigu entre
        // conjoint (L.423-1) / parent (L.423-7) / liens perso (L.423-23).
        // On renvoie null pour forcer choix manuel précis (gouvernance précision).
        String result = ImmigrationPieceReferentiel.inferChecklistType(
                "CST_VPF", null, null);
        assertThat(result).isNull();
    }
}
