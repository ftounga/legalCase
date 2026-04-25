package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-DT-31-01 : moteur d'analyse de validité d'un protocole transactionnel
 * en droit du travail français — art. 2044 à 2052 du Code civil + jurisprudence
 * Cass. soc. 16/12/2010 n° 09-67.679 (concessions réciproques).
 *
 * <p>Produit un score 0-100, un verdict {@code VALIDE / CONTESTABLE / NULLE} et
 * un drapeau {@code risqueNulliteRetenu} actif si vice du consentement allégué
 * OU absence de concessions réciproques.</p>
 *
 * <p><b>Pays</b> : FRANCE uniquement. La transaction belge (Loi 03/07/1978 + CCT)
 * a une mécanique différente et fera l'objet d'une feature jumelle dédiée.</p>
 */
public final class TransactionCalculator {

    /** Concessions employeur reconnues. */
    public enum ConcessionEmployeur {
        INDEMNITE_TRANSACTIONNELLE,
        MAINTIEN_AVANTAGES_SOCIAUX,
        LETTRE_RECOMMANDATION,
        LEVEE_NON_CONCURRENCE,
        DELAI_PRESCRIPTION_ANTICIPE,
        AUTRE
    }

    /** Concessions salarié reconnues. */
    public enum ConcessionSalarie {
        RENONCIATION_ACTION_PRUDHOMALE,
        RENONCIATION_PARTAGE_BENEFICES,
        AUTRE
    }

    /** Nature de la rupture préalable à la transaction. */
    public enum RupturePrealable {
        LICENCIEMENT_PERSONNEL,
        LICENCIEMENT_ECONOMIQUE,
        RUPTURE_CONVENTIONNELLE,
        DEMISSION,
        FIN_CDD,
        AUTRE
    }

    /** Verdict synthétique de validité. */
    public enum VerdictValidite {
        VALIDE,        // score ≥ 70
        CONTESTABLE,   // score 40-69
        NULLE          // score < 40
    }

    private static final int SCORE_RECIPROQUES = 30;
    private static final int SCORE_DELAI_15J = 20;
    private static final int SCORE_AVOCAT = 15;
    private static final int SCORE_ABSENCE_VICE = 15;
    private static final int SCORE_INDEMNITE_SUP_MACRON = 20;

    private static final int SCORE_VALIDE_MIN = 70;
    private static final int SCORE_CONTESTABLE_MIN = 40;

    private static final BigDecimal HALF = new BigDecimal("0.5");
    private static final int MACRON_PLAFOND_ANS = 30;

    private TransactionCalculator() {}

    /**
     * Calcule le score de validité, le verdict et le risque de nullité
     * d'un protocole transactionnel (FRANCE uniquement).
     *
     * @param input  données saisies par l'avocat
     * @param country pays du workspace ({@code "FRANCE"} uniquement supporté)
     * @return résultat structuré (score, verdict, ratio concessions, base juridique)
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static TransactionResult compute(TransactionInput input, String country) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (input.dateSignature() == null) {
            throw new IllegalArgumentException("Date de signature requise");
        }
        if (input.salaireMensuelBrutEur() == null || input.salaireMensuelBrutEur().signum() <= 0) {
            throw new IllegalArgumentException("Salaire mensuel brut requis et strictement positif");
        }
        if (input.ancienneteAnnees() == null || input.ancienneteAnnees() < 0) {
            throw new IllegalArgumentException("Ancienneté requise et ≥ 0");
        }
        if (input.indemniteTransactionnelleEur() == null
                || input.indemniteTransactionnelleEur().signum() < 0) {
            throw new IllegalArgumentException("Indemnité transactionnelle requise et ≥ 0");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil disponible uniquement en FRANCE — pendant BE en cours de scoping");
        }

        List<ConcessionEmployeur> concEmp = input.concessionsEmployeur() == null
                ? List.of() : List.copyOf(input.concessionsEmployeur());
        List<ConcessionSalarie> concSal = input.concessionsSalarie() == null
                ? List.of() : List.copyOf(input.concessionsSalarie());

        boolean reciproques = !concEmp.isEmpty() && !concSal.isEmpty();
        BigDecimal ratioEmpPct = computeRatioEmployeurPct(concEmp.size(), concSal.size());

        BigDecimal salaire = input.salaireMensuelBrutEur().setScale(2, RoundingMode.HALF_UP);
        int anc = Math.min(input.ancienneteAnnees(), MACRON_PLAFOND_ANS);
        BigDecimal plancherMacron = salaire.multiply(HALF).multiply(new BigDecimal(anc))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal indemnite = input.indemniteTransactionnelleEur()
                .setScale(2, RoundingMode.HALF_UP);
        boolean indemniteSupMacron = indemnite.compareTo(plancherMacron) > 0;

        boolean delai15 = Boolean.TRUE.equals(input.delaiReflexion15jOk());
        boolean avocat = Boolean.TRUE.equals(input.presenceAvocatAssistance());
        boolean viceAllegue = Boolean.TRUE.equals(input.viceConsentementAllegue());
        boolean absenceVice = !viceAllegue;

        int score = 0;
        if (reciproques) score += SCORE_RECIPROQUES;
        if (delai15) score += SCORE_DELAI_15J;
        if (avocat) score += SCORE_AVOCAT;
        if (absenceVice) score += SCORE_ABSENCE_VICE;
        if (indemniteSupMacron) score += SCORE_INDEMNITE_SUP_MACRON;

        VerdictValidite verdict = verdictPour(score);
        boolean risqueNullite = viceAllegue || !reciproques;

        String baseJuridique = "Art. 2044 + 2052 Cciv + Cass. soc. 16/12/2010 n° 09-67.679";
        String formule = construireFormule(score, verdict, reciproques, delai15, avocat,
                absenceVice, indemniteSupMacron, indemnite, plancherMacron);
        List<String> messages = construireMessages(reciproques, viceAllegue, delai15, avocat,
                indemniteSupMacron, verdict, risqueNullite, indemnite, plancherMacron);

        return new TransactionResult(
                reciproques,
                ratioEmpPct,
                indemniteSupMacron,
                score,
                verdict,
                risqueNullite,
                baseJuridique,
                formule,
                messages,
                countryNormalized
        );
    }

    private static BigDecimal computeRatioEmployeurPct(int nbEmp, int nbSal) {
        int total = nbEmp + nbSal;
        if (total == 0) return BigDecimal.ZERO.setScale(1);
        return new BigDecimal(nbEmp * 100)
                .divide(new BigDecimal(total), 1, RoundingMode.HALF_UP);
    }

    private static VerdictValidite verdictPour(int score) {
        if (score >= SCORE_VALIDE_MIN) return VerdictValidite.VALIDE;
        if (score >= SCORE_CONTESTABLE_MIN) return VerdictValidite.CONTESTABLE;
        return VerdictValidite.NULLE;
    }

    private static String construireFormule(int score, VerdictValidite verdict,
                                            boolean reciproques, boolean delai15,
                                            boolean avocat, boolean absenceVice,
                                            boolean indemniteSupMacron,
                                            BigDecimal indemnite,
                                            BigDecimal plancherMacron) {
        StringBuilder sb = new StringBuilder();
        sb.append("Score ").append(score).append("/100 — ").append(verdict.name());
        sb.append(" | ");
        List<String> badges = new ArrayList<>();
        badges.add(reciproques ? "concessions réciproques" : "PAS de concessions réciproques");
        badges.add(delai15 ? "délai 15j OK" : "délai 15j absent");
        badges.add(avocat ? "avocat" : "sans avocat");
        badges.add(absenceVice ? "absence vice" : "vice allégué");
        badges.add(indemniteSupMacron
                ? "indemnité " + format(indemnite) + " > Macron " + format(plancherMacron)
                : "indemnité " + format(indemnite) + " ≤ Macron " + format(plancherMacron));
        sb.append(String.join(" | ", badges));
        return sb.toString();
    }

    private static List<String> construireMessages(boolean reciproques, boolean viceAllegue,
                                                   boolean delai15, boolean avocat,
                                                   boolean indemniteSupMacron,
                                                   VerdictValidite verdict,
                                                   boolean risqueNullite,
                                                   BigDecimal indemnite,
                                                   BigDecimal plancherMacron) {
        List<String> msgs = new ArrayList<>();
        msgs.add("Validité d'un protocole transactionnel — art. 2044 à 2052 Code civil.");
        if (reciproques) {
            msgs.add("Concessions réciproques caractérisées (≥1 employeur + ≥1 salarié) "
                    + "— condition de validité Cass. soc. 16/12/2010 satisfaite.");
        } else {
            msgs.add("ATTENTION : pas de concessions réciproques — la transaction est NULLE "
                    + "selon Cass. soc. 16/12/2010 n° 09-67.679.");
        }
        if (viceAllegue) {
            msgs.add("Vice du consentement allégué — annulation possible (art. 2053 Cciv) : "
                    + "violence, dol, erreur sur l'objet ou la cause.");
        }
        if (!delai15) {
            msgs.add("Délai de réflexion 15 jours non respecté ou non documenté — "
                    + "risque de contestation pour consentement précipité.");
        }
        if (!avocat) {
            msgs.add("Absence d'avocat assistance — recommandé pour écarter toute "
                    + "contestation ultérieure pour vice du consentement.");
        }
        if (!indemniteSupMacron) {
            msgs.add("Indemnité transactionnelle (" + format(indemnite) + " €) n'excède pas "
                    + "l'indemnité barémée Macron (" + format(plancherMacron) + " €) — "
                    + "concession de l'employeur affaiblie.");
        }
        msgs.add("Verdict : " + verdict.name() + (risqueNullite
                ? " — risque de nullité retenu, contester l'opposabilité de la transaction."
                : " — la transaction sécurise la rupture sous réserve d'exécution."));
        return msgs;
    }

    private static String format(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
