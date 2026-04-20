package fr.ailegalcase.casefile;

import fr.ailegalcase.referential.LegalReferentialService;

import java.math.BigDecimal;

/**
 * SF-139-01 : fixtures des barèmes Macron + CCT 109 pour les tests de
 * {@link IndemniteComparatifCalculator}. Les valeurs effectives sont en DB
 * (migration 067, INDEMNITE_BAREMES/MACRON + CCT109).
 */
final class TestIndemniteBaremes {

    private TestIndemniteBaremes() {}

    /** CCT 109 — 3 à 17 semaines. */
    static final LegalReferentialService.Cct109Range CCT_109 =
            new LegalReferentialService.Cct109Range(new BigDecimal("3"), new BigDecimal("17"));

    /** Barème Macron selon ancienneté. */
    static IndemniteBareme macron(int ancienneteAnnees) {
        int capped = Math.min(Math.max(ancienneteAnnees, 0), 29);
        return switch (capped) {
            case 0 -> new IndemniteBareme(0, bd("0"), bd("1"));
            case 1 -> new IndemniteBareme(1, bd("1"), bd("2"));
            case 2 -> new IndemniteBareme(2, bd("3"), bd("3.5"));
            case 3 -> new IndemniteBareme(3, bd("3"), bd("4"));
            case 4 -> new IndemniteBareme(4, bd("3"), bd("5"));
            case 5 -> new IndemniteBareme(5, bd("3"), bd("6"));
            case 6 -> new IndemniteBareme(6, bd("3"), bd("7"));
            case 7 -> new IndemniteBareme(7, bd("3"), bd("8"));
            case 8 -> new IndemniteBareme(8, bd("3"), bd("8"));
            case 9 -> new IndemniteBareme(9, bd("3"), bd("9"));
            case 10 -> new IndemniteBareme(10, bd("3"), bd("10"));
            case 11 -> new IndemniteBareme(11, bd("3"), bd("10.5"));
            case 12 -> new IndemniteBareme(12, bd("3"), bd("11"));
            case 13 -> new IndemniteBareme(13, bd("3"), bd("11.5"));
            case 14 -> new IndemniteBareme(14, bd("3"), bd("12"));
            case 15 -> new IndemniteBareme(15, bd("3"), bd("13"));
            case 16 -> new IndemniteBareme(16, bd("3"), bd("13.5"));
            case 17 -> new IndemniteBareme(17, bd("3"), bd("14"));
            case 18 -> new IndemniteBareme(18, bd("3"), bd("14.5"));
            case 19 -> new IndemniteBareme(19, bd("3"), bd("15"));
            case 20 -> new IndemniteBareme(20, bd("3"), bd("15.5"));
            case 21 -> new IndemniteBareme(21, bd("3"), bd("16"));
            case 22 -> new IndemniteBareme(22, bd("3"), bd("16.5"));
            case 23 -> new IndemniteBareme(23, bd("3"), bd("17"));
            case 24 -> new IndemniteBareme(24, bd("3"), bd("17.5"));
            case 25 -> new IndemniteBareme(25, bd("3"), bd("18"));
            case 26 -> new IndemniteBareme(26, bd("3"), bd("18.5"));
            case 27 -> new IndemniteBareme(27, bd("3"), bd("19"));
            case 28 -> new IndemniteBareme(28, bd("3"), bd("19.5"));
            case 29 -> new IndemniteBareme(29, bd("3"), bd("20"));
            default -> throw new IllegalStateException("unreachable");
        };
    }

    private static BigDecimal bd(String v) { return new BigDecimal(v); }
}
