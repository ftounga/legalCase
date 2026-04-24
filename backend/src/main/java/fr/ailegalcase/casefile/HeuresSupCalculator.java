package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-DT-19-01 : calculateur de rappel de salaire pour heures supplémentaires
 * impayées (FR + BE). Chef de demande très fréquent en prud'hommes (FR) et au
 * tribunal du travail (BE).
 *
 * Formule FR (art. L.3121-28 à L.3121-39, D.3121-24 Code du travail) :
 *   rappel = h25 × tauxHoraireBrut × (tauxMajo25/100)
 *          + h50 × tauxHoraireBrut × (tauxMajo50/100)
 *   heuresHorsContingent (au-delà de 220h/an) ouvrent droit à la contrepartie
 *   obligatoire en repos (COR) — 100 % du temps de chaque HS.
 *
 * Formule BE (art. 29 Loi 16 mars 1971 + Loi 4 janvier 1974) :
 *   rappel = heuresSupSemaine × tauxHoraireBrut × 0.5
 *          + heuresDimancheJoursFeries × tauxHoraireBrut × 1.0
 *   repos compensatoire obligatoire en sus du sursalaire (art. 26bis).
 */
public final class HeuresSupCalculator {

    private static final BigDecimal TAUX_MAJO_MIN = new BigDecimal("10");
    private static final BigDecimal TAUX_MAJO_MAX = new BigDecimal("50");
    private static final BigDecimal DEFAULT_TAUX_25 = new BigDecimal("25");
    private static final BigDecimal DEFAULT_TAUX_50 = new BigDecimal("50");

    private static final String BASE_FR =
            "Art. L.3121-28 à L.3121-39 Code du travail + D.3121-24 (contingent annuel 220h)";
    private static final String BASE_BE =
            "Art. 29 Loi 16 mars 1971 sur le travail";

    private static final List<String> MESSAGES_FR = List.of(
            "Prescription triennale (L.3245-1) : action en rappel de salaire prescrite dans les 3 ans.",
            "Les heures au-delà du contingent annuel (220h par défaut, voir CCN applicable) ouvrent droit à une contrepartie obligatoire en repos (COR) — L.3121-30.",
            "Les taux légaux 25 % / 50 % peuvent être réduits par CCN étendue, minimum 10 %."
    );

    private static final List<String> MESSAGES_BE = List.of(
            "Prescription quinquennale (art. 2262bis Code civil).",
            "Repos compensatoire obligatoire en sus du sursalaire (art. 26bis Loi 16/3/1971).",
            "Le sursalaire 100 % s'applique aux dimanches et jours fériés légaux (loi 4 janvier 1974)."
    );

    private HeuresSupCalculator() {}

    public static HeuresSupResult compute(HeuresSupRequest request, String country) {
        if (!"FRANCE".equals(country) && !"BELGIQUE".equals(country)) {
            throw new IllegalArgumentException(
                    "Country requis : FRANCE ou BELGIQUE (actuel : " + country + ")");
        }
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }

        BigDecimal taux = request.tauxHoraireBrut();
        if (taux == null || taux.signum() <= 0) {
            throw new IllegalArgumentException("Taux horaire brut requis et strictement positif");
        }

        Integer h25 = nonNegative(request.heuresSupDeclarees25pct(), "heuresSupDeclarees25pct");
        Integer h50 = nonNegative(request.heuresSupDeclarees50pct(), "heuresSupDeclarees50pct");
        Integer hHorsContingent = nonNegative(request.heuresHorsContingent(), "heuresHorsContingent");
        Integer hSemaineBe = nonNegative(request.heuresSupSemaine(), "heuresSupSemaine");
        Integer hDimBe = nonNegative(request.heuresDimancheJoursFeries(), "heuresDimancheJoursFeries");

        if ("FRANCE".equals(country)) {
            if (hSemaineBe != null && hSemaineBe > 0) {
                throw new IllegalArgumentException(
                        "Champ heuresSupSemaine réservé à la Belgique — incompatible workspace FRANCE");
            }
            if (hDimBe != null && hDimBe > 0) {
                throw new IllegalArgumentException(
                        "Champ heuresDimancheJoursFeries réservé à la Belgique — incompatible workspace FRANCE");
            }
            return computeFr(taux, h25, h50, hHorsContingent,
                    request.tauxMajoration25(), request.tauxMajoration50());
        } else {
            if (h25 != null && h25 > 0) {
                throw new IllegalArgumentException(
                        "Champ heuresSupDeclarees25pct réservé à la France — incompatible workspace BELGIQUE");
            }
            if (h50 != null && h50 > 0) {
                throw new IllegalArgumentException(
                        "Champ heuresSupDeclarees50pct réservé à la France — incompatible workspace BELGIQUE");
            }
            if (hHorsContingent != null && hHorsContingent > 0) {
                throw new IllegalArgumentException(
                        "Champ heuresHorsContingent réservé à la France — incompatible workspace BELGIQUE");
            }
            return computeBe(taux, hSemaineBe, hDimBe);
        }
    }

    private static HeuresSupResult computeFr(BigDecimal taux,
                                              Integer h25In, Integer h50In, Integer hHorsContingentIn,
                                              BigDecimal tauxMajo25In, BigDecimal tauxMajo50In) {
        int h25 = h25In != null ? h25In : 0;
        int h50 = h50In != null ? h50In : 0;
        int hHors = hHorsContingentIn != null ? hHorsContingentIn : 0;

        if (h25 + h50 + hHors == 0) {
            throw new IllegalArgumentException("Au moins une heure supplémentaire requise");
        }

        BigDecimal tauxMajo25 = tauxMajo25In != null ? tauxMajo25In : DEFAULT_TAUX_25;
        BigDecimal tauxMajo50 = tauxMajo50In != null ? tauxMajo50In : DEFAULT_TAUX_50;
        checkTauxMajoration(tauxMajo25, "tauxMajoration25");
        checkTauxMajoration(tauxMajo50, "tauxMajoration50");

        BigDecimal tauxRounded = taux.setScale(2, RoundingMode.HALF_UP);

        BigDecimal rappel25 = BigDecimal.valueOf(h25)
                .multiply(tauxRounded)
                .multiply(tauxMajo25)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal rappel50 = BigDecimal.valueOf(h50)
                .multiply(tauxRounded)
                .multiply(tauxMajo50)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal rappel100 = BigDecimal.ZERO.setScale(2);
        BigDecimal rappelTotal = rappel25.add(rappel50).setScale(2, RoundingMode.HALF_UP);

        BigDecimal reposCompHeures = BigDecimal.valueOf(hHors).setScale(2, RoundingMode.HALF_UP);

        String formule = String.format(
                "Rappel = %dh × %s €/h × %s%% + %dh × %s €/h × %s%% = %s €. "
                        + "Heures hors contingent (%dh) → repos compensateur dû (L.3121-30).",
                h25, fmt(tauxRounded), fmt(tauxMajo25),
                h50, fmt(tauxRounded), fmt(tauxMajo50),
                fmt(rappelTotal), hHors);

        List<String> messages = new ArrayList<>(MESSAGES_FR);

        return new HeuresSupResult(
                tauxRounded, h25, h50, hHors,
                tauxMajo25.setScale(2, RoundingMode.HALF_UP),
                tauxMajo50.setScale(2, RoundingMode.HALF_UP),
                0, 0,
                "FRANCE",
                rappel25, rappel50, rappel100, rappelTotal,
                reposCompHeures,
                formule, BASE_FR, messages);
    }

    private static HeuresSupResult computeBe(BigDecimal taux, Integer hSemIn, Integer hDimIn) {
        int hSem = hSemIn != null ? hSemIn : 0;
        int hDim = hDimIn != null ? hDimIn : 0;

        if (hSem + hDim == 0) {
            throw new IllegalArgumentException("Au moins une heure supplémentaire requise");
        }

        BigDecimal tauxRounded = taux.setScale(2, RoundingMode.HALF_UP);

        BigDecimal rappel50 = BigDecimal.valueOf(hSem)
                .multiply(tauxRounded)
                .multiply(new BigDecimal("0.5"))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal rappel100 = BigDecimal.valueOf(hDim)
                .multiply(tauxRounded)
                .multiply(BigDecimal.ONE)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal rappel25 = BigDecimal.ZERO.setScale(2);
        BigDecimal rappelTotal = rappel50.add(rappel100).setScale(2, RoundingMode.HALF_UP);

        BigDecimal reposCompHeures = BigDecimal.valueOf(hSem + hDim).setScale(2, RoundingMode.HALF_UP);

        String formule = String.format(
                "Rappel = %dh × %s €/h × 50%% + %dh × %s €/h × 100%% = %s €. "
                        + "Repos compensatoire dû : %dh (art. 26bis).",
                hSem, fmt(tauxRounded),
                hDim, fmt(tauxRounded),
                fmt(rappelTotal), hSem + hDim);

        List<String> messages = new ArrayList<>(MESSAGES_BE);

        return new HeuresSupResult(
                tauxRounded, 0, 0, 0,
                BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2),
                hSem, hDim,
                "BELGIQUE",
                rappel25, rappel50, rappel100, rappelTotal,
                reposCompHeures,
                formule, BASE_BE, messages);
    }

    private static Integer nonNegative(Integer value, String name) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException("Le champ " + name + " ne peut pas être négatif");
        }
        return value;
    }

    private static void checkTauxMajoration(BigDecimal taux, String name) {
        if (taux.compareTo(TAUX_MAJO_MIN) < 0 || taux.compareTo(TAUX_MAJO_MAX) > 0) {
            throw new IllegalArgumentException(
                    name + " doit être entre 10 % et 50 % (actuel : " + fmt(taux) + " %)");
        }
    }

    private static String fmt(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
