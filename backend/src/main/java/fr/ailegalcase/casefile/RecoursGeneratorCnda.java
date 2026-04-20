package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-133-01 : générateur de recours {@code RECOURS_CNDA} (asile France, CNDA).
 * Format distinct des recours administratifs classiques : argumentaire sur
 * la protection conventionnelle (Convention de Genève) et la protection
 * subsidiaire (CESEDA L. 512-1).
 */
final class RecoursGeneratorCnda {

    private RecoursGeneratorCnda() {}

    static GeneratedRecours generate(
            RecoursType type, String recoursTypeCode, LocalDate dateNotification,
            String requerantNom, String requerantPrenom, String requerantNationalite,
            String requerantAdresse, String autoriteDecision, LocalDate dateDecision,
            String referenceDecision, String exposeFaits
    ) {
        var dateInfo = RecoursGeneratorCommon.computeDateLimite(type, dateNotification);
        String enTete = buildEnTete(type, requerantNom, requerantPrenom, requerantAdresse);
        String objet = RecoursGeneratorCommon.buildObjet(autoriteDecision, dateDecision, referenceDecision);
        String visaTextes = RecoursGeneratorCommon.buildVisaTextes(type);
        String moyens = buildMoyens(requerantPrenom, requerantNom);
        String conclusions = buildConclusions(type, requerantNom, requerantPrenom);
        String faits = exposeFaits != null && !exposeFaits.isBlank() ? exposeFaits : "[À compléter par l'avocat]";

        return new GeneratedRecours(
                recoursTypeCode, enTete, objet, visaTextes, faits, moyens, conclusions,
                type.piecesStandard(), dateInfo.dateLimite(), dateInfo.depassee(), dateInfo.avertissement());
    }

    private static String buildEnTete(RecoursType type, String nom, String prenom, String adresse) {
        return String.format("""
                %s

                REQUÉRANT :
                %s %s
                Demeurant : %s""",
                type.juridiction(), prenom, nom, adresse);
    }

    private static String buildMoyens(String prenom, String nom) {
        return String.format("""
                SUR LA PROTECTION CONVENTIONNELLE (article 1er A 2 de la Convention de Genève) :
                %s %s craint avec raison d'être persécuté(e) du fait de [sa race / sa religion / sa nationalité / \
                son appartenance à un groupe social / ses opinions politiques] et ne peut se réclamer de la \
                protection de son pays d'origine.

                SUR LA PROTECTION SUBSIDIAIRE (article L. 512-1 du CESEDA) :
                À défaut de la reconnaissance du statut de réfugié, le requérant est exposé dans son pays \
                d'origine à des menaces graves au sens de l'article L. 512-1 du CESEDA.""",
                prenom, nom);
    }

    private static String buildConclusions(RecoursType type, String nom, String prenom) {
        return String.format("""
                PAR CES MOTIFS,

                %s %s a l'honneur de demander à %s de bien vouloir :
                - ANNULER la décision contestée ;
                - ENJOINDRE à l'autorité compétente de réexaminer la situation du requérant dans un délai \
                de deux mois à compter de la notification du jugement ;
                - CONDAMNER l'État aux entiers dépens.""",
                prenom, nom, type.juridiction());
    }
}
