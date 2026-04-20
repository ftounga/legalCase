package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-133-01 : générateur de recours pour la Belgique — couvre
 * {@code RECOURS_CGRA} (asile), {@code RECOURS_CCE} (Conseil du contentieux
 * des étrangers) et {@code RECOURS_CE_BELGIQUE} (cassation). Partagent un
 * entête BE commun et des conclusions BE, les moyens diffèrent par code.
 */
final class RecoursGeneratorBelgique {

    private RecoursGeneratorBelgique() {}

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

                PARTIE REQUÉRANTE :
                %s %s
                Domicilié(e) à : %s""",
                type.juridiction(), prenom, nom, adresse);
    }

    private static String buildMoyens(String code, String autorite, LocalDate dateDecision,
                                       String prenom, String nom) {
        return switch (code) {
            case "RECOURS_CGRA" -> String.format("""
                    EN FAIT :
                    %s %s a introduit une demande de protection internationale auprès de l'Office des étrangers. \
                    Cette demande a été déclarée [irrecevable / non fondée] par décision du [date].

                    EN DROIT :
                    La décision contestée viole l'article 48/3 de la loi du 15 décembre 1980 en ce qu'elle \
                    n'a pas correctement évalué les craintes de persécution invoquées.
                    Le CGRA n'a pas respecté son obligation d'instruction prévue par la directive 2013/32/UE.""",
                    prenom, nom);

            case "RECOURS_CCE" -> String.format("""
                    PREMIER MOYEN : Violation de l'article 62 de la loi du 15 décembre 1980
                    La décision de %s en date du %s ne contient pas une motivation adéquate au regard \
                    de la situation individuelle de %s %s.

                    DEUXIÈME MOYEN : Violation du principe de proportionnalité
                    La mesure d'éloignement est disproportionnée au regard de l'article 8 de la CEDH \
                    et de l'article 7 de la Charte des droits fondamentaux de l'Union européenne.

                    TROISIÈME MOYEN : Violation de l'article 3 de la CEDH
                    Le renvoi vers le pays d'origine exposerait le requérant à des traitements inhumains \
                    ou dégradants.""",
                    autorite, dateDecision, prenom, nom);

            case "RECOURS_CE_BELGIQUE" -> """
                    MOYEN UNIQUE DE CASSATION :
                    L'arrêt du Conseil du contentieux des étrangers viole [préciser la disposition légale] \
                    en ce qu'il [préciser le grief : motivation insuffisante, erreur de droit, violation \
                    du principe du contradictoire].

                    [À compléter par l'avocat avec les moyens de cassation spécifiques]""";

            default -> throw new IllegalStateException(
                    "RecoursGeneratorBelgique ne gère pas le code : " + code);
        };
    }

    private static String buildConclusions(RecoursType type, String nom, String prenom) {
        return String.format("""
                PAR CES MOTIFS,

                La partie requérante %s %s demande qu'il plaise au %s de :
                - ANNULER la décision contestée ;
                - ORDONNER le réexamen de la demande ;
                - CONDAMNER la partie adverse aux dépens.""",
                prenom, nom, type.juridiction());
    }
}
