package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-217-01 : input de l'analyse du régime de communauté légale belge.
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis ici. Tous les
 * champs proviennent du body de requête (saisie manuelle de l'avocat — aucun
 * pré-fill IA en V1).</p>
 *
 * @param dateMariage          date du mariage ({@code yyyy-MM-dd}), obligatoire, non future.
 * @param contratMariageSigne  vrai si les époux ont signé un contrat de mariage.
 * @param biens                liste obligatoire et non vide des biens du couple.
 * @param dettes               liste obligatoire (peut être vide) des dettes du couple.
 */
public record RegimeCommunauteLegaleBeInput(
        LocalDate dateMariage,
        Boolean contratMariageSigne,
        List<BienItem> biens,
        List<DetteItem> dettes
) {

    /**
     * Bien du patrimoine du couple, caractérisé par ses critères de qualification.
     *
     * @param libelle                       intitulé du bien (max 200 caractères).
     * @param categorie                     catégorie du bien.
     * @param acquisAvantMariage            vrai si le bien a été acquis avant le mariage.
     * @param acquisParDonationOuSuccession vrai si le bien a été reçu par donation/succession.
     * @param financeParFondsPropres        vrai si le bien a été financé par des fonds propres.
     * @param affectationProfessionnelle    vrai si le bien est affecté à la profession d'un époux.
     * @param outilDeTravailPersonnel       vrai si le bien est un outil de travail strictement personnel.
     * @param commentaire                   commentaire libre nullable (max 1000 caractères).
     */
    public record BienItem(
            String libelle,
            BienCategorieBe categorie,
            Boolean acquisAvantMariage,
            Boolean acquisParDonationOuSuccession,
            Boolean financeParFondsPropres,
            Boolean affectationProfessionnelle,
            Boolean outilDeTravailPersonnel,
            String commentaire
    ) {}

    /**
     * Dette du patrimoine du couple, caractérisée par ses critères de qualification.
     *
     * @param libelle                       intitulé de la dette (max 200 caractères).
     * @param anterieureAuMariage           vrai si la dette est antérieure au mariage.
     * @param contracteeDansLInteretDuMenage vrai si la dette a été contractée dans l'intérêt du ménage.
     * @param contracteeParUnSeulEpoux      vrai si la dette a été contractée par un seul époux.
     * @param commentaire                   commentaire libre nullable (max 1000 caractères).
     */
    public record DetteItem(
            String libelle,
            Boolean anterieureAuMariage,
            Boolean contracteeDansLInteretDuMenage,
            Boolean contracteeParUnSeulEpoux,
            String commentaire
    ) {}
}
