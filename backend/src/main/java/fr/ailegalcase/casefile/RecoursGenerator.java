package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.stream.Collectors;

/**
 * Génère un document de recours structuré à partir des données du dossier et du type de recours.
 */
public final class RecoursGenerator {

    private RecoursGenerator() {}

    /**
     * @param recoursTypeCode  code du type de recours
     * @param dateNotification date de notification du refus
     * @param requerantNom     nom du requérant
     * @param requerantPrenom  prénom du requérant
     * @param requerantNationalite nationalité
     * @param requerantAdresse adresse
     * @param autoriteDecision autorité ayant pris la décision contestée
     * @param dateDecision     date de la décision contestée
     * @param referenceDecision référence de la décision (nullable)
     * @param exposeFaits      exposé des faits (nullable)
     * @return le document structuré généré
     * @throws IllegalArgumentException si le type de recours est inconnu
     */
    public static GeneratedRecours generate(
            String recoursTypeCode,
            LocalDate dateNotification,
            String requerantNom,
            String requerantPrenom,
            String requerantNationalite,
            String requerantAdresse,
            String autoriteDecision,
            LocalDate dateDecision,
            String referenceDecision,
            String exposeFaits
    ) {
        RecoursType type = ImmigrationRecoursReferentiel.getByCode(recoursTypeCode);
        if (type == null) {
            throw new IllegalArgumentException("Type de recours inconnu : " + recoursTypeCode);
        }

        LocalDate dateLimite = dateNotification.plusDays(type.delaiJours());
        boolean depassee = LocalDate.now().isAfter(dateLimite);
        String avertissement = depassee
                ? "ATTENTION : le délai de recours de " + type.delaiJours() + " jours est dépassé depuis le "
                  + dateLimite + ". Le recours risque d'être déclaré irrecevable."
                : null;

        String enTete = buildEnTete(type, requerantNom, requerantPrenom, requerantAdresse);
        String objet = buildObjet(type, autoriteDecision, dateDecision, referenceDecision);
        String visaTextes = buildVisaTextes(type);
        String moyens = buildMoyensDroit(type, requerantNom, requerantPrenom, autoriteDecision, dateDecision);
        String conclusions = buildConclusions(type, requerantNom, requerantPrenom);
        String faits = exposeFaits != null && !exposeFaits.isBlank() ? exposeFaits : "[À compléter par l'avocat]";

        return new GeneratedRecours(
                recoursTypeCode,
                enTete,
                objet,
                visaTextes,
                faits,
                moyens,
                conclusions,
                type.piecesStandard(),
                dateLimite,
                depassee,
                avertissement
        );
    }

    private static String buildEnTete(RecoursType type, String nom, String prenom, String adresse) {
        return switch (type.country()) {
            case "FRANCE" -> String.format("""
                    %s

                    REQUÉRANT :
                    %s %s
                    Demeurant : %s""",
                    type.juridiction(), prenom, nom, adresse);
            case "BELGIQUE" -> String.format("""
                    %s

                    PARTIE REQUÉRANTE :
                    %s %s
                    Domicilié(e) à : %s""",
                    type.juridiction(), prenom, nom, adresse);
            default -> type.juridiction() + "\n\n" + prenom + " " + nom;
        };
    }

    private static String buildObjet(RecoursType type, String autorite, LocalDate dateDecision, String reference) {
        String ref = reference != null && !reference.isBlank() ? " (réf. " + reference + ")" : "";
        return String.format("Recours contre la décision de %s en date du %s%s portant refus de titre de séjour.",
                autorite, dateDecision, ref);
    }

    private static String buildVisaTextes(RecoursType type) {
        return type.textesApplicables().stream()
                .map(t -> "Vu " + t + " ;")
                .collect(Collectors.joining("\n"));
    }

    private static String buildMoyensDroit(RecoursType type, String nom, String prenom,
                                            String autorite, LocalDate dateDecision) {
        return switch (type.code()) {
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

            case "RECOURS_CNDA" -> String.format("""
                    SUR LA PROTECTION CONVENTIONNELLE (article 1er A 2 de la Convention de Genève) :
                    %s %s craint avec raison d'être persécuté(e) du fait de [sa race / sa religion / sa nationalité / \
                    son appartenance à un groupe social / ses opinions politiques] et ne peut se réclamer de la \
                    protection de son pays d'origine.

                    SUR LA PROTECTION SUBSIDIAIRE (article L. 512-1 du CESEDA) :
                    À défaut de la reconnaissance du statut de réfugié, le requérant est exposé dans son pays \
                    d'origine à des menaces graves au sens de l'article L. 512-1 du CESEDA.""",
                    prenom, nom);

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

            default -> "[Moyens de droit à compléter]";
        };
    }

    private static String buildConclusions(RecoursType type, String nom, String prenom) {
        return switch (type.country()) {
            case "FRANCE" -> String.format("""
                    PAR CES MOTIFS,

                    %s %s a l'honneur de demander à %s de bien vouloir :
                    - ANNULER la décision contestée ;
                    - ENJOINDRE à l'autorité compétente de réexaminer la situation du requérant dans un délai \
                    de deux mois à compter de la notification du jugement ;
                    - CONDAMNER l'État aux entiers dépens.""",
                    prenom, nom, type.juridiction());
            case "BELGIQUE" -> String.format("""
                    PAR CES MOTIFS,

                    La partie requérante %s %s demande qu'il plaise au %s de :
                    - ANNULER la décision contestée ;
                    - ORDONNER le réexamen de la demande ;
                    - CONDAMNER la partie adverse aux dépens.""",
                    prenom, nom, type.juridiction());
            default -> "PAR CES MOTIFS, le requérant demande l'annulation de la décision contestée.";
        };
    }
}
