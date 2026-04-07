package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

import static fr.ailegalcase.casefile.ConventionBareme.*;

/**
 * Référentiel statique des barèmes d'ancienneté par convention collective (FR) et commission paritaire (BE).
 */
public final class ConventionBaremeReferentiel {

    private ConventionBaremeReferentiel() {}

    private static final List<ConventionBareme> ALL = List.of(

            // ========== FRANCE ==========

            new ConventionBareme("METALLURGIE", "Métallurgie (IDCC 3248)", "FRANCE", 25,
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
            ),
            new ConventionBareme("COMMERCE", "Commerce de détail (IDCC 2216)", "FRANCE", 25,
                    List.of(
                            new CongesSupplementaire(10, 1, "+1 jour après 10 ans"),
                            new CongesSupplementaire(15, 2, "+2 jours après 15 ans"),
                            new CongesSupplementaire(20, 3, "+3 jours après 20 ans")
                    ),
                    List.of(
                            new PrimeAnciennete(3, new BigDecimal("3"), "3% après 3 ans"),
                            new PrimeAnciennete(6, new BigDecimal("6"), "6% après 6 ans"),
                            new PrimeAnciennete(9, new BigDecimal("9"), "9% après 9 ans")
                    ),
                    "CCN du commerce de détail et de gros à prédominance alimentaire (IDCC 2216)"
            ),
            new ConventionBareme("BTP", "Bâtiment et travaux publics (IDCC 1596)", "FRANCE", 25,
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
            ),
            new ConventionBareme("HCR", "Hôtels, cafés, restaurants (IDCC 1979)", "FRANCE", 25,
                    List.of(
                            new CongesSupplementaire(10, 1, "+1 jour après 10 ans"),
                            new CongesSupplementaire(20, 2, "+2 jours après 20 ans")
                    ),
                    List.of(
                            new PrimeAnciennete(5, new BigDecimal("2"), "2% après 5 ans"),
                            new PrimeAnciennete(10, new BigDecimal("5"), "5% après 10 ans"),
                            new PrimeAnciennete(15, new BigDecimal("8"), "8% après 15 ans")
                    ),
                    "CCN des hôtels, cafés, restaurants (IDCC 1979)"
            ),
            new ConventionBareme("SYNTEC", "Syntec — bureaux d'études (IDCC 1486)", "FRANCE", 25,
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
            ),

            // ========== BELGIQUE ==========

            new ConventionBareme("CP200", "CP 200 — Commission paritaire auxiliaire pour employés", "BELGIQUE", 20,
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
            ),
            new ConventionBareme("CP124", "CP 124 — Construction", "BELGIQUE", 20,
                    List.of(
                            new CongesSupplementaire(5, 1, "+1 jour après 5 ans"),
                            new CongesSupplementaire(10, 3, "+3 jours après 10 ans"),
                            new CongesSupplementaire(20, 5, "+5 jours après 20 ans")
                    ),
                    List.of(
                            new PrimeAnciennete(5, new BigDecimal("3"), "3% après 5 ans"),
                            new PrimeAnciennete(10, new BigDecimal("6"), "6% après 10 ans"),
                            new PrimeAnciennete(15, new BigDecimal("9"), "9% après 15 ans"),
                            new PrimeAnciennete(20, new BigDecimal("12"), "12% après 20 ans")
                    ),
                    "Loi du 28 juin 1971 ; CCT CP 124 construction"
            ),
            new ConventionBareme("CP302", "CP 302 — Industrie hôtelière", "BELGIQUE", 20,
                    List.of(
                            new CongesSupplementaire(10, 1, "+1 jour après 10 ans"),
                            new CongesSupplementaire(20, 2, "+2 jours après 20 ans")
                    ),
                    List.of(
                            new PrimeAnciennete(5, new BigDecimal("1.5"), "1.5% après 5 ans"),
                            new PrimeAnciennete(10, new BigDecimal("3"), "3% après 10 ans"),
                            new PrimeAnciennete(15, new BigDecimal("5"), "5% après 15 ans")
                    ),
                    "Loi du 28 juin 1971 ; CCT CP 302 industrie hôtelière"
            )
    );

    public static ConventionBareme getByCode(String code) {
        return ALL.stream().filter(b -> b.code().equals(code)).findFirst().orElse(null);
    }

    public static List<ConventionBareme> getByCountry(String country) {
        return ALL.stream().filter(b -> b.country().equals(country)).toList();
    }

    public static List<ConventionBareme> getAll() {
        return ALL;
    }

    public static boolean isCodeValid(String code) {
        return ALL.stream().anyMatch(b -> b.code().equals(code));
    }
}
