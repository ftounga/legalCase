package fr.ailegalcase.casefile;

import fr.ailegalcase.document.DocumentPieceType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SF-IM-01-05 : tests du mapping libellé → DocumentPieceType.
 */
class ImmigrationPieceAutoFillServiceTest {

    @Test
    void U01_passeport_mappePasseport() {
        Set<DocumentPieceType> result = ImmigrationPieceAutoFillService.matchPieceTypes(
                "Passeport en cours de validité");
        assertThat(result).containsExactly(DocumentPieceType.PASSEPORT);
    }

    @Test
    void U02_acteMariage_mappeActeMariage() {
        Set<DocumentPieceType> result = ImmigrationPieceAutoFillService.matchPieceTypes(
                "Acte de mariage intégral (copie < 3 mois)");
        assertThat(result).containsExactly(DocumentPieceType.ACTE_MARIAGE);
    }

    @Test
    void U03_acteNaissanceEnfant_prioritaire() {
        Set<DocumentPieceType> result = ImmigrationPieceAutoFillService.matchPieceTypes(
                "Acte de naissance intégral de l'enfant français");
        assertThat(result).containsExactly(DocumentPieceType.ACTE_NAISSANCE_ENFANT);
    }

    @Test
    void U04_contratTravail_mappeContrat() {
        Set<DocumentPieceType> result = ImmigrationPieceAutoFillService.matchPieceTypes(
                "Contrat de travail signé par l'employeur");
        assertThat(result).containsExactly(DocumentPieceType.CONTRAT);
    }

    @Test
    void U05_justificatifDomicile_mappePlusieurs() {
        Set<DocumentPieceType> result = ImmigrationPieceAutoFillService.matchPieceTypes(
                "Justificatif de domicile < 3 mois");
        assertThat(result).contains(DocumentPieceType.BAIL_LOCATION,
                DocumentPieceType.QUITTANCE_LOYER,
                DocumentPieceType.ATTESTATION_HEBERGEMENT);
    }

    @Test
    void U06_communauteDeVie_mappeBailEtAttestations() {
        Set<DocumentPieceType> result = ImmigrationPieceAutoFillService.matchPieceTypes(
                "Justificatif de communauté de vie effective");
        assertThat(result).contains(DocumentPieceType.BAIL_LOCATION,
                DocumentPieceType.ATTESTATION,
                DocumentPieceType.QUITTANCE_LOYER);
    }

    @Test
    void U07_titreSejour_mappeTitreDeSejour() {
        Set<DocumentPieceType> result = ImmigrationPieceAutoFillService.matchPieceTypes(
                "Titre de séjour valide depuis au moins 5 ans");
        assertThat(result).containsExactly(DocumentPieceType.TITRE_DE_SEJOUR);
    }

    @Test
    void U08_casierJudiciaire_noMatch() {
        // "Casier judiciaire vierge" n'a pas d'équivalent dans DocumentPieceType V1
        Set<DocumentPieceType> result = ImmigrationPieceAutoFillService.matchPieceTypes(
                "Casier judiciaire vierge");
        assertThat(result).isEmpty();
    }

    @Test
    void U09_formulaireCerfa_noMatch() {
        Set<DocumentPieceType> result = ImmigrationPieceAutoFillService.matchPieceTypes(
                "Formulaire CERFA 15187 (demande carte VPF conjoint)");
        assertThat(result).isEmpty();
    }

    @Test
    void U10_null_setVide() {
        assertThat(ImmigrationPieceAutoFillService.matchPieceTypes(null)).isEmpty();
    }

    @Test
    void U11_bulletinPaie_mappeBulletin() {
        Set<DocumentPieceType> result = ImmigrationPieceAutoFillService.matchPieceTypes(
                "Bulletins de paie des 12 derniers mois");
        assertThat(result).containsExactly(DocumentPieceType.BULLETIN_PAIE);
    }

    @Test
    void U12_avisImposition() {
        Set<DocumentPieceType> result = ImmigrationPieceAutoFillService.matchPieceTypes(
                "Avis d'imposition sur le revenu");
        assertThat(result).containsExactly(DocumentPieceType.AVIS_IMPOSITION);
    }

    @Test
    void U13_photo() {
        Set<DocumentPieceType> result = ImmigrationPieceAutoFillService.matchPieceTypes(
                "Photo d'identité récente (norme ANTS)");
        assertThat(result).contains(DocumentPieceType.PHOTO, DocumentPieceType.PIECE_IDENTITE);
    }
}
