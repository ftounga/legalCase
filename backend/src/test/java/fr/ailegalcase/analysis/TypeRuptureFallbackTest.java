package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TypeRuptureFallbackTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void derive_licenciementValidityFr_retourneLicenciement() throws Exception {
        JsonNode root = mapper.readTree("""
                {"licenciement_validity_detection": {
                   "FR_CONVOCATION": {"reponse": "OUI", "justification": "Lettre du 12/03"},
                   "FR_MOTIVATION":  {"reponse": "NON", "justification": "Motif vague"}
                }}""");
        assertThat(TypeRuptureFallback.derive(root)).isEqualTo("LICENCIEMENT");
    }

    @Test
    void derive_licenciementValidityBe_retourneLicenciementOrdinaire() throws Exception {
        JsonNode root = mapper.readTree("""
                {"licenciement_validity_detection": {
                   "BE_NOTIFICATION": {"reponse": "OUI", "justification": ""},
                   "BE_PREAVIS":      {"reponse": "INCONNU", "justification": ""}
                }}""");
        assertThat(TypeRuptureFallback.derive(root)).isEqualTo("LICENCIEMENT_ORDINAIRE");
    }

    @Test
    void derive_typeLitigeLicenciementSansCause_retourneLicenciement() throws Exception {
        JsonNode root = mapper.readTree("""
                {"type_litige_detecte": "LICENCIEMENT_SANS_CAUSE_REELLE"}""");
        assertThat(TypeRuptureFallback.derive(root)).isEqualTo("LICENCIEMENT");
    }

    @Test
    void derive_typeLitigePriseActe_retournePriseActe() throws Exception {
        JsonNode root = mapper.readTree("""
                {"type_litige_detecte": "PRISE_ACTE_RUPTURE"}""");
        assertThat(TypeRuptureFallback.derive(root)).isEqualTo("PRISE_ACTE");
    }

    @Test
    void derive_typeLitigeHarcelement_sansDetection_retourneNull() throws Exception {
        JsonNode root = mapper.readTree("""
                {"type_litige_detecte": "HARCELEMENT_MORAL"}""");
        assertThat(TypeRuptureFallback.derive(root)).isNull();
    }

    @Test
    void derive_aucunSignal_retourneNull() throws Exception {
        JsonNode root = mapper.readTree("{}");
        assertThat(TypeRuptureFallback.derive(root)).isNull();
    }

    @Test
    void derive_detectionVide_retourneNull() throws Exception {
        JsonNode root = mapper.readTree("""
                {"licenciement_validity_detection": {}}""");
        assertThat(TypeRuptureFallback.derive(root)).isNull();
    }

    @Test
    void derive_detectionAvecReponsesNull_retourneNull() throws Exception {
        JsonNode root = mapper.readTree("""
                {"licenciement_validity_detection": {
                   "FR_CONVOCATION": {"reponse": null, "justification": ""}
                }}""");
        assertThat(TypeRuptureFallback.derive(root)).isNull();
    }

    @Test
    void derive_typeLitigePrioritaireSurDetection() throws Exception {
        // type_litige_detecte plus précis est retenu même si detection est présente.
        JsonNode root = mapper.readTree("""
                {"type_litige_detecte": "LICENCIEMENT_ECONOMIQUE",
                 "licenciement_validity_detection": {
                   "FR_CONVOCATION": {"reponse": "OUI", "justification": ""}
                }}""");
        assertThat(TypeRuptureFallback.derive(root)).isEqualTo("LICENCIEMENT_ECONOMIQUE");
    }

    @Test
    void normalize_valeurValide_retourneUpperCase() {
        assertThat(TypeRuptureFallback.normalize(" licenciement ")).isEqualTo("LICENCIEMENT");
    }

    @Test
    void normalize_valeurHorsEnum_retourneNull() {
        assertThat(TypeRuptureFallback.normalize("FANTAISIE")).isNull();
    }

    @Test
    void normalize_null_retourneNull() {
        assertThat(TypeRuptureFallback.normalize(null)).isNull();
    }

    // Fix F-DT-09 — LICENCIEMENT_MANIFESTEMENT_DERAISONNABLE retiré de l'enum
    @Test
    void normalize_licenciementManifestementDeraisonnable_mappeVersLicenciementOrdinaire() {
        // C'est une qualification CCT 109 art. 8, pas une nature de rupture — le type
        // sous-jacent est toujours un licenciement ordinaire. Mapping de rétrocompat
        // pour les analyses déjà persistées avec l'ancien enum.
        assertThat(TypeRuptureFallback.normalize("LICENCIEMENT_MANIFESTEMENT_DERAISONNABLE"))
                .isEqualTo("LICENCIEMENT_ORDINAIRE");
    }

    @Test
    void normalize_licenciementManifestementDeraisonnable_caseInsensitive_mappeAussi() {
        assertThat(TypeRuptureFallback.normalize("licenciement_manifestement_deraisonnable"))
                .isEqualTo("LICENCIEMENT_ORDINAIRE");
        assertThat(TypeRuptureFallback.normalize(" Licenciement_Manifestement_Deraisonnable "))
                .isEqualTo("LICENCIEMENT_ORDINAIRE");
    }

    @Test
    void validTypes_neContientPlusLicenciementManifestementDeraisonnable() {
        assertThat(TypeRuptureFallback.VALID_TYPES)
                .doesNotContain("LICENCIEMENT_MANIFESTEMENT_DERAISONNABLE")
                .contains("LICENCIEMENT_ORDINAIRE", "RUPTURE_AMIABLE");
    }
}
