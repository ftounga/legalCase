package fr.ailegalcase.casefile;

import java.util.List;

/**
 * Référentiel statique des critères de validité du licenciement France + Belgique.
 */
public final class LicenciementCritereReferentiel {

    private LicenciementCritereReferentiel() {}

    private static final List<LicenciementCritere> ALL = List.of(

            // ========== FRANCE ==========

            new LicenciementCritere("FR_CONVOCATION", "Convocation à l'entretien préalable", "FRANCE",
                    "Le salarié a-t-il été convoqué par lettre recommandée ou remise en main propre, avec un délai minimum de 5 jours ouvrables avant l'entretien ?",
                    15, true, "Article L. 1232-2 du Code du travail"),

            new LicenciementCritere("FR_ENTRETIEN", "Tenue de l'entretien préalable", "FRANCE",
                    "L'entretien préalable a-t-il eu lieu ? Le salarié a-t-il pu se faire assister ?",
                    15, true, "Article L. 1232-3 du Code du travail"),

            new LicenciementCritere("FR_DELAI_NOTIFICATION", "Délai de notification", "FRANCE",
                    "La lettre de licenciement a-t-elle été envoyée au moins 2 jours ouvrables après l'entretien (7 jours pour un cadre) ?",
                    10, false, "Article L. 1232-6 du Code du travail"),

            new LicenciementCritere("FR_MOTIVATION", "Motivation de la lettre de licenciement", "FRANCE",
                    "La lettre de licenciement énonce-t-elle des motifs précis et matériellement vérifiables ? (cause réelle et sérieuse)",
                    20, true, "Articles L. 1232-1 et L. 1235-1 du Code du travail"),

            new LicenciementCritere("FR_MOTIF_REEL", "Motif réel et sérieux", "FRANCE",
                    "Le motif invoqué est-il objectif, exact et suffisamment grave pour justifier le licenciement ?",
                    25, true, "Article L. 1232-1 du Code du travail"),

            new LicenciementCritere("FR_PROCEDURE_DISCIPLINAIRE", "Respect de la procédure disciplinaire", "FRANCE",
                    "Si licenciement pour faute : les faits ont-ils été sanctionnés dans les 2 mois suivant leur connaissance ? Le salarié a-t-il déjà été sanctionné pour les mêmes faits (non bis in idem) ?",
                    10, false, "Articles L. 1332-1 et L. 1332-4 du Code du travail"),

            new LicenciementCritere("FR_ORDRE_LICENCIEMENT", "Respect de l'ordre des licenciements", "FRANCE",
                    "En cas de licenciement économique : les critères d'ordre (ancienneté, charges de famille, qualités professionnelles) ont-ils été appliqués ?",
                    5, false, "Article L. 1233-5 du Code du travail"),

            // ========== BELGIQUE ==========

            new LicenciementCritere("BE_NOTIFICATION", "Notification du licenciement", "BELGIQUE",
                    "Le licenciement a-t-il été notifié par lettre recommandée ou par exploit d'huissier ?",
                    10, true, "Loi du 3 juillet 1978, article 37"),

            new LicenciementCritere("BE_PREAVIS", "Respect du délai de préavis", "BELGIQUE",
                    "Le délai de préavis légal (calculé selon l'ancienneté, loi du 26/12/2013) a-t-il été respecté ou une indemnité compensatoire versée ?",
                    15, true, "Loi du 26 décembre 2013 concernant le statut unique"),

            new LicenciementCritere("BE_MOTIVATION", "Motivation du licenciement (CCT 109)", "BELGIQUE",
                    "L'employeur peut-il démontrer que le licenciement est lié au comportement du travailleur, à son aptitude ou aux nécessités de fonctionnement de l'entreprise ?",
                    25, true, "CCT n° 109 du 12 février 2014"),

            new LicenciementCritere("BE_AUDITION", "Audition préalable du travailleur", "BELGIQUE",
                    "Le travailleur a-t-il été entendu avant la décision de licenciement ? (recommandé mais non obligatoire sauf convention d'entreprise)",
                    10, false, "Bonne pratique — recommandation SPF Emploi"),

            new LicenciementCritere("BE_NON_DISCRIMINATION", "Absence de motif discriminatoire", "BELGIQUE",
                    "Le licenciement n'est-il pas fondé sur un critère protégé (genre, origine, handicap, conviction, activité syndicale) ?",
                    20, true, "Loi du 10 mai 2007 tendant à lutter contre certaines formes de discrimination"),

            new LicenciementCritere("BE_PROTECTION_SPECIALE", "Absence de protection spéciale", "BELGIQUE",
                    "Le travailleur n'est-il pas protégé contre le licenciement (délégué syndical, conseiller en prévention, travailleuse enceinte, crédit-temps) ?",
                    15, true, "Diverses lois de protection — Loi du 19/03/1991 (délégués), Loi du 16/03/1971 (grossesse)"),

            new LicenciementCritere("BE_INDEMNITE_MANIFESTE", "Risque d'indemnité pour licenciement manifestement déraisonnable", "BELGIQUE",
                    "Le licenciement pourrait-il être qualifié de manifestement déraisonnable au sens de la CCT 109 (indemnité de 3 à 17 semaines) ?",
                    5, false, "CCT n° 109, article 9")
    );

    public static List<LicenciementCritere> getByCountry(String country) {
        return ALL.stream().filter(c -> c.country().equals(country)).toList();
    }

    public static LicenciementCritere getByCode(String code) {
        return ALL.stream().filter(c -> c.code().equals(code)).findFirst().orElse(null);
    }

    public static List<LicenciementCritere> getAll() {
        return ALL;
    }

    public static boolean isCountryValid(String country) {
        return "FRANCE".equals(country) || "BELGIQUE".equals(country);
    }
}
