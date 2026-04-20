package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-133-01 : générateur de recours administratif France — couvre
 * {@code RECOURS_GRACIEUX_PREFET} et {@code RECOURS_CONTENTIEUX_TA}.
 * Le template d'entête et de conclusion est le même pour les deux ; seuls
 * les moyens de droit diffèrent.
 */
final class RecoursGeneratorFrance {

    private RecoursGeneratorFrance() {}

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
        String moyens = buildMoyens(recoursTypeCode, autoriteDecision, dateDecision, requerantPrenom, requerantNom);
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

    private static String buildMoyens(String code, String autorite, LocalDate dateDecision,
                                       String prenom, String nom) {
        return switch (code) {
            case "RECOURS_GRACIEUX_PREFET" -> String.format("""
                    Sur l'erreur de droit :
                    La décision de %s en date du %s méconnaît les dispositions du CESEDA en ce qu'elle n'a pas \
                    pris en compte l'ensemble des éléments de la situation personnelle de %s %s.

                    Sur l'erreur manifeste d'appréciation :
                    La décision est entachée d'une erreur manifeste d'appréciation au regard de la situation \
                    personnelle et familiale du requérant.

                    Sur la violation du droit à la vie privée et familiale :
                    La décision porte une atteinte disproportionnée au droit au respect de la vie privée et \
                    familiale garanti par l'article 8 de la Convention européenne des droits de l'homme.""",
                    autorite, dateDecision, prenom, nom);

            case "RECOURS_CONTENTIEUX_TA" -> String.format("""
                    PREMIER MOYEN : Sur l'incompétence de l'auteur de l'acte
                    [À compléter si applicable]

                    DEUXIÈME MOYEN : Sur le vice de procédure
                    La décision de %s en date du %s a été prise sans que %s %s ait été mis(e) en mesure \
                    de présenter ses observations, en violation du principe du contradictoire.

                    TROISIÈME MOYEN : Sur l'erreur de droit
                    La décision méconnaît les dispositions des articles L. 423-1 et suivants du CESEDA.

                    QUATRIÈME MOYEN : Sur l'atteinte au droit à la vie privée et familiale
                    La décision porte une atteinte disproportionnée au droit garanti par l'article 8 de la CEDH.""",
                    autorite, dateDecision, prenom, nom);

            default -> throw new IllegalStateException(
                    "RecoursGeneratorFrance ne gère pas le code : " + code);
        };
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
