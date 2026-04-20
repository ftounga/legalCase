package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-139-01 : fixtures des critères de validité licenciement FR + BE pour les
 * tests d'analyzer. Les valeurs sont seedées en DB (migration 067).
 */
final class TestLicenciementCriteres {

    private TestLicenciementCriteres() {}

    static final List<LicenciementCritere> FRANCE = List.of(
            new LicenciementCritere("FR_CONVOCATION", "Convocation entretien préalable", "FRANCE",
                    "Convocation LRAR ou main propre", 15, true, "L. 1232-2"),
            new LicenciementCritere("FR_ENTRETIEN", "Tenue entretien préalable", "FRANCE",
                    "Entretien effectué", 15, true, "L. 1232-3"),
            new LicenciementCritere("FR_DELAI_NOTIFICATION", "Délai de notification", "FRANCE",
                    "2 jours ouvrables", 10, false, "L. 1232-6"),
            new LicenciementCritere("FR_MOTIVATION", "Motivation lettre licenciement", "FRANCE",
                    "Motifs précis", 20, true, "L. 1232-1"),
            new LicenciementCritere("FR_MOTIF_REEL", "Motif réel et sérieux", "FRANCE",
                    "Motif objectif", 25, true, "L. 1232-1"),
            new LicenciementCritere("FR_PROCEDURE_DISCIPLINAIRE", "Procédure disciplinaire", "FRANCE",
                    "Faits moins de 2 mois", 10, false, "L. 1332-1"),
            new LicenciementCritere("FR_ORDRE_LICENCIEMENT", "Ordre des licenciements", "FRANCE",
                    "Critères ancienneté", 5, false, "L. 1233-5")
    );

    static final List<LicenciementCritere> BELGIQUE = List.of(
            new LicenciementCritere("BE_AUDITION", "Audition préalable", "BELGIQUE",
                    "Audition effectuée", 20, true, "Loi 03/07/1978"),
            new LicenciementCritere("BE_MOTIVATION", "Motivation écrite", "BELGIQUE",
                    "Motifs clairs", 20, true, "CCT 109"),
            new LicenciementCritere("BE_DELAI_PREAVIS", "Délai de préavis respecté", "BELGIQUE",
                    "Préavis légal", 15, true, "Loi 26/12/2013"),
            new LicenciementCritere("BE_MOTIF_MANIFESTEMENT_DERAISONNABLE", "Motif non manifestement déraisonnable", "BELGIQUE",
                    "Motif raisonnable", 25, true, "CCT 109"),
            new LicenciementCritere("BE_PROTECTION", "Protection spécifique respectée", "BELGIQUE",
                    "Non-discrimination", 15, true, "Loi 10/05/2007"),
            new LicenciementCritere("BE_FORMALITES", "Formalités administratives", "BELGIQUE",
                    "Documents sociaux", 5, false, "Loi 03/07/1978")
    );
}
