package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-139-01 : fixtures des critères de validité rupture conventionnelle FR
 * pour les tests d'analyzer. Les valeurs sont seedées en DB (migration 092).
 * Ce helper évite à chaque test de passer par un lookup DB.
 */
final class TestRuptureConvCriteres {

    private TestRuptureConvCriteres() {}

    static final List<RuptureConvCritere> FRANCE = List.of(
            new RuptureConvCritere("RC_CONSENTEMENT", "Consentement libre et éclairé", "FRANCE",
                    "Consentement exempt de vice", 25, true, "Art. L1237-11"),
            new RuptureConvCritere("RC_DELAI_RETRACTATION", "Délai 15 jours", "FRANCE",
                    "Délai calendaire respecté", 20, true, "Art. L1237-13 al.2"),
            new RuptureConvCritere("RC_HOMOLOGATION", "Homologation DREETS", "FRANCE",
                    "Homologation obtenue", 25, true, "Art. L1237-14"),
            new RuptureConvCritere("RC_ASSISTANCE", "Assistance documentée", "FRANCE",
                    "Assistance documentée", 10, false, "Art. L1237-12"),
            new RuptureConvCritere("RC_INDEMNITE", "Indemnité spécifique", "FRANCE",
                    "Indemnité ≥ indemnité légale", 15, true, "Art. L1237-13 al.1"),
            new RuptureConvCritere("RC_ENTRETIENS", "Entretien préalable", "FRANCE",
                    "Entretien documenté", 5, false, "Art. L1237-12 al.1")
    );
}
