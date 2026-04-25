package fr.ailegalcase.referential;

import fr.ailegalcase.casefile.IndemnitePreavisFonction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * SF-F-138-01 : tests unitaires complémentaires sur
 * {@link LegalReferentialService#getConventionPreavis} pour les CCN ajoutées par la
 * migration 163 ({@code CONVENTION_PREAVIS}).
 *
 * <p>Pattern identique aux tests existants ({@link LegalReferentialServiceTest}) :
 * mock du repository, vérification du contrat (durée, fonction, article, fallback null).
 */
class LegalReferentialServicePreavisExtensionTest {

    private LegalReferentialRepository repository;
    private LegalReferentialService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(LegalReferentialRepository.class);
        service = new LegalReferentialService(repository);
    }

    @Test
    void getConventionPreavis_idcc1597_ouvrier_36mois_retourne_2_mois() {
        // CCN Bâtiment Ouvriers > 10 sal. (IDCC 1597) — OUVRIER ≥ 24 mois → 2 mois
        LegalReferential entry = entryWith("""
                {
                  "fonctions": {
                    "OUVRIER": [
                      {"min": 0,  "max": 3,    "mois": 0},
                      {"min": 3,  "max": 6,    "mois": 1},
                      {"min": 6,  "max": 24,   "mois": 1},
                      {"min": 24, "max": null, "mois": 2}
                    ],
                    "EMPLOYE":         [{"min": 0, "max": null, "mois": 1}],
                    "AGENT_MAITRISE":  [{"min": 0, "max": null, "mois": 2}],
                    "CADRE":           [{"min": 0, "max": null, "mois": 3}]
                  },
                  "article": "CCN Bâtiment Ouvriers art. 9.1"
                }
                """);
        when(repository.findSystemEntry("DROIT_DU_TRAVAIL", "CONVENTION_PREAVIS", "IDCC_1597"))
                .thenReturn(List.of(entry));

        LegalReferentialService.ConventionPreavis result =
                service.getConventionPreavis("IDCC_1597", IndemnitePreavisFonction.OUVRIER, 36);

        assertThat(result).isNotNull();
        assertThat(result.code()).isEqualTo("IDCC_1597");
        assertThat(result.fonction()).isEqualTo(IndemnitePreavisFonction.OUVRIER);
        assertThat(result.dureeMois()).isEqualTo(2);
        assertThat(result.article()).isEqualTo("CCN Bâtiment Ouvriers art. 9.1");
    }

    @Test
    void getConventionPreavis_idcc1979_employe_12mois_retourne_1_mois() {
        // CCN HCR (IDCC 1979) — EMPLOYE 6 ≤ ancienneté < 24 mois → 1 mois
        LegalReferential entry = entryWith("""
                {
                  "fonctions": {
                    "OUVRIER":  [{"min": 0, "max": null, "mois": 1}],
                    "EMPLOYE": [
                      {"min": 0,  "max": 6,    "mois": 0},
                      {"min": 6,  "max": 24,   "mois": 1},
                      {"min": 24, "max": null, "mois": 2}
                    ],
                    "AGENT_MAITRISE":  [{"min": 0, "max": null, "mois": 2}],
                    "CADRE":           [{"min": 0, "max": null, "mois": 3}]
                  },
                  "article": "CCN HCR art. 30"
                }
                """);
        when(repository.findSystemEntry("DROIT_DU_TRAVAIL", "CONVENTION_PREAVIS", "IDCC_1979"))
                .thenReturn(List.of(entry));

        LegalReferentialService.ConventionPreavis result =
                service.getConventionPreavis("IDCC_1979", IndemnitePreavisFonction.EMPLOYE, 12);

        assertThat(result).isNotNull();
        assertThat(result.dureeMois()).isEqualTo(1);
        assertThat(result.fonction()).isEqualTo(IndemnitePreavisFonction.EMPLOYE);
        assertThat(result.article()).isEqualTo("CCN HCR art. 30");
    }

    @Test
    void getConventionPreavis_idcc2609_cadre_0mois_retourne_3_mois_des_embauche() {
        // CCN Bâtiment ETAM (IDCC 2609) — CADRE dès embauche → 3 mois (plus favorable que légale)
        LegalReferential entry = entryWith("""
                {
                  "fonctions": {
                    "OUVRIER":         [{"min": 0, "max": null, "mois": 1}],
                    "EMPLOYE":         [{"min": 0, "max": null, "mois": 1}],
                    "AGENT_MAITRISE":  [{"min": 0, "max": null, "mois": 2}],
                    "CADRE":           [{"min": 0, "max": null, "mois": 3}]
                  },
                  "article": "CCN Bâtiment ETAM art. 5"
                }
                """);
        when(repository.findSystemEntry("DROIT_DU_TRAVAIL", "CONVENTION_PREAVIS", "IDCC_2609"))
                .thenReturn(List.of(entry));

        LegalReferentialService.ConventionPreavis result =
                service.getConventionPreavis("IDCC_2609", IndemnitePreavisFonction.CADRE, 0);

        assertThat(result).isNotNull();
        assertThat(result.dureeMois()).isEqualTo(3);
        assertThat(result.fonction()).isEqualTo(IndemnitePreavisFonction.CADRE);
    }

    @Test
    void getConventionPreavis_idcc1996_employe_24mois_retourne_2_mois() {
        // CCN Pharmacie d'officine (IDCC 1996) — EMPLOYE ≥ 24 mois → 2 mois
        LegalReferential entry = entryWith("""
                {
                  "fonctions": {
                    "EMPLOYE": [
                      {"min": 0,  "max": 6,    "mois": 1},
                      {"min": 6,  "max": 24,   "mois": 2},
                      {"min": 24, "max": null, "mois": 2}
                    ],
                    "CADRE":   [{"min": 0, "max": null, "mois": 3}]
                  },
                  "article": "CCN Pharmacie d'officine art. 23"
                }
                """);
        when(repository.findSystemEntry("DROIT_DU_TRAVAIL", "CONVENTION_PREAVIS", "IDCC_1996"))
                .thenReturn(List.of(entry));

        LegalReferentialService.ConventionPreavis result =
                service.getConventionPreavis("IDCC_1996", IndemnitePreavisFonction.EMPLOYE, 24);

        assertThat(result).isNotNull();
        assertThat(result.dureeMois()).isEqualTo(2);
    }

    @Test
    void getConventionPreavis_ccn_inconnue_retourne_null() {
        // CCN non seedée → fallback null → IndemnitePreavisCalculator bascule sur LEGALE
        when(repository.findSystemEntry("DROIT_DU_TRAVAIL", "CONVENTION_PREAVIS", "IDCC_9999"))
                .thenReturn(List.of());

        LegalReferentialService.ConventionPreavis result =
                service.getConventionPreavis("IDCC_9999", IndemnitePreavisFonction.EMPLOYE, 12);

        assertThat(result).isNull();
    }

    @Test
    void getConventionPreavis_anciennete_negative_retourne_null() {
        // Garde-fou contractuel : valeur d'ancienneté négative invalidée en amont
        LegalReferentialService.ConventionPreavis result =
                service.getConventionPreavis("IDCC_1597", IndemnitePreavisFonction.OUVRIER, -1);

        assertThat(result).isNull();
    }

    private static LegalReferential entryWith(String valueJson) {
        LegalReferential entry = new LegalReferential();
        entry.setValueJson(valueJson);
        return entry;
    }
}
