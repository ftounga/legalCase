package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

import static fr.ailegalcase.casefile.ConventionBareme.CongesSupplementaire;
import static fr.ailegalcase.casefile.ConventionBareme.PrimeAnciennete;

/**
 * Fixtures {@link ConventionBareme} utilisées par les tests unitaires du calculateur.
 * Les barèmes réels sont en base (cf. migrations 086/087 + {@code LegalReferentialService}) ;
 * ces fixtures ne servent qu'à tester la logique pure du {@code AncienneteCalculator}.
 */
final class TestConventionBaremes {

    private TestConventionBaremes() {}

    static final ConventionBareme METALLURGIE = new ConventionBareme(
            "METALLURGIE", "Métallurgie (IDCC 3248)", "FRANCE", 25,
            List.of(
                    new CongesSupplementaire(5, 1, "+1 jour après 5 ans"),
                    new CongesSupplementaire(10, 2, "+2 jours après 10 ans"),
                    new CongesSupplementaire(15, 3, "+3 jours après 15 ans"),
                    new CongesSupplementaire(20, 4, "+4 jours après 20 ans")
            ),
            List.of(
                    new PrimeAnciennete(3, new BigDecimal("3"), "3% après 3 ans"),
                    new PrimeAnciennete(6, new BigDecimal("6"), "6% après 6 ans"),
                    new PrimeAnciennete(9, new BigDecimal("9"), "9% après 9 ans"),
                    new PrimeAnciennete(12, new BigDecimal("12"), "12% après 12 ans"),
                    new PrimeAnciennete(15, new BigDecimal("15"), "15% après 15 ans")
            ),
            "Convention collective nationale de la métallurgie (IDCC 3248)"
    );

    static final ConventionBareme BTP = new ConventionBareme(
            "BTP", "Bâtiment et travaux publics (IDCC 1596)", "FRANCE", 25,
            List.of(
                    new CongesSupplementaire(5, 2, "+2 jours après 5 ans"),
                    new CongesSupplementaire(10, 4, "+4 jours après 10 ans"),
                    new CongesSupplementaire(20, 6, "+6 jours après 20 ans")
            ),
            List.of(
                    new PrimeAnciennete(3, new BigDecimal("2"), "2% après 3 ans"),
                    new PrimeAnciennete(5, new BigDecimal("4"), "4% après 5 ans"),
                    new PrimeAnciennete(10, new BigDecimal("8"), "8% après 10 ans"),
                    new PrimeAnciennete(15, new BigDecimal("12"), "12% après 15 ans")
            ),
            "CCN des ouvriers du bâtiment (IDCC 1596)"
    );

    static final ConventionBareme SYNTEC = new ConventionBareme(
            "SYNTEC", "Syntec — bureaux d'études (IDCC 1486)", "FRANCE", 25,
            List.of(
                    new CongesSupplementaire(5, 1, "+1 jour après 5 ans"),
                    new CongesSupplementaire(10, 2, "+2 jours après 10 ans"),
                    new CongesSupplementaire(15, 3, "+3 jours après 15 ans"),
                    new CongesSupplementaire(20, 4, "+4 jours après 20 ans")
            ),
            List.of(
                    new PrimeAnciennete(2, new BigDecimal("2"), "2% après 2 ans"),
                    new PrimeAnciennete(5, new BigDecimal("5"), "5% après 5 ans"),
                    new PrimeAnciennete(10, new BigDecimal("10"), "10% après 10 ans"),
                    new PrimeAnciennete(15, new BigDecimal("15"), "15% après 15 ans")
            ),
            "CCN Syntec — bureaux d'études techniques (IDCC 1486)"
    );

    static final ConventionBareme CP200 = new ConventionBareme(
            "CP200", "CP 200 — Commission paritaire auxiliaire pour employés", "BELGIQUE", 20,
            List.of(
                    new CongesSupplementaire(5, 1, "+1 jour après 5 ans"),
                    new CongesSupplementaire(10, 2, "+2 jours après 10 ans"),
                    new CongesSupplementaire(15, 3, "+3 jours après 15 ans")
            ),
            List.of(
                    new PrimeAnciennete(5, new BigDecimal("2"), "2% après 5 ans (selon CCT sectorielle)"),
                    new PrimeAnciennete(10, new BigDecimal("4"), "4% après 10 ans"),
                    new PrimeAnciennete(20, new BigDecimal("7"), "7% après 20 ans")
            ),
            "Loi du 28 juin 1971 sur les vacances annuelles ; CCT CP 200"
    );

    /** Échantillon FR + BE servant au test paramétré {@code allConventions_calculateWithoutError}. */
    static final List<ConventionBareme> SAMPLE = List.of(METALLURGIE, BTP, SYNTEC, CP200);
}
