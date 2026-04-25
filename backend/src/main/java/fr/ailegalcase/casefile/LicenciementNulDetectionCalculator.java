package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SF-DT-16-01 : moteur de détection automatique des protections légales encadrant
 * un licenciement et calcul de l'indemnité plancher 6 mois prévue par
 * l'art. L.1235-3-1 al. 2 du Code du travail français.
 *
 * <p>À la différence de {@link HarcelementNulliteCalculator} (F-DT-11) qui requiert
 * un motif unique choisi par l'avocat, F-DT-16 détecte simultanément plusieurs
 * protections actives à partir des faits (booléens + dates), produit un score 0-100
 * de probabilité de nullité et confirme l'ouverture de la réintégration.</p>
 *
 * <p><b>Pays</b> : FRANCE uniquement. La nullité belge repose sur des mécaniques
 * différentes (Loi 1978 art. 63 + CCT 109 + Loi 4/8/1996 art. 32tredecies) et fera
 * l'objet d'une feature jumelle dédiée.</p>
 */
public final class LicenciementNulDetectionCalculator {

    /** Protections détectables — 7 codes structurés (8e cas LIBERTE_FONDAMENTALE non auto-détecté). */
    public enum Protection {
        MATERNITE,         // L.1225-4 — grossesse / 10 semaines après accouchement
        ACCIDENT_TRAVAIL,  // L.1226-9 — pendant suspension AT/MP
        HARCELEMENT,       // L.1152-3 — moral ou sexuel avéré
        DISCRIMINATION,    // L.1132-4 — fondée sur un motif L.1132-1
        LANCEUR_ALERTE,    // L.1132-3-3 — alerte éthique de bonne foi
        MANDAT_SYNDICAL,   // L.2411-1 et s. — salarié protégé
        ACTION_JUSTICE     // L.1132-1 al. dernier — représailles action en justice
    }

    /** Verdict de probabilité de nullité dérivé du score. */
    public enum VerdictProbabilite {
        FAIBLE,     // 0
        MOYENNE,    // 35-69
        ELEVEE      // ≥ 70
    }

    private static final BigDecimal SIX = new BigDecimal("6");
    private static final int INDEMNITE_MINIMUM_MOIS = 6;
    private static final int SCORE_PAR_PROTECTION = 35;
    private static final int SCORE_MAX = 100;

    /** Protection MATERNITE active si accouchement < 10 semaines avant la notification. */
    private static final long MATERNITE_FENETRE_SEMAINES = 10;
    /** Protection ACCIDENT_TRAVAIL active si consolidation < 6 mois avant la notification. */
    private static final long AT_FENETRE_MOIS = 6;

    private LicenciementNulDetectionCalculator() {}

    /**
     * Calcule la détection de protections + l'indemnité minimum.
     *
     * @param input  données saisies par l'avocat
     * @param country pays du workspace ("FRANCE" uniquement supporté pour cette SF)
     * @return résultat structuré (protections détectées, score, indemnité, base juridique)
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static LicenciementNulDetectionResult compute(LicenciementNulDetectionInput input, String country) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (input.salaireMensuelBrutEur() == null || input.salaireMensuelBrutEur().signum() <= 0) {
            throw new IllegalArgumentException("Salaire mensuel brut requis et strictement positif");
        }
        if (input.dateNotificationLicenciement() == null) {
            throw new IllegalArgumentException("Date de notification du licenciement requise");
        }
        if (input.ancienneteAnnees() != null && input.ancienneteAnnees() < 0) {
            throw new IllegalArgumentException("Ancienneté ne peut être négative");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil disponible uniquement en FRANCE — pendant BE en cours de scoping");
        }

        Set<Protection> protectionsDetectees = detecterProtections(input);
        int nombre = protectionsDetectees.size();
        boolean nulliteProbable = nombre >= 1;
        int scoreNullite = Math.min(SCORE_MAX, nombre * SCORE_PAR_PROTECTION);
        VerdictProbabilite verdict = verdictPour(scoreNullite);

        BigDecimal salaire = input.salaireMensuelBrutEur().setScale(2, RoundingMode.HALF_UP);
        BigDecimal indemnite = salaire.multiply(SIX).setScale(2, RoundingMode.HALF_UP);

        String baseJuridique = baseJuridiquePour(protectionsDetectees);
        String formule = construireFormule(salaire, indemnite, protectionsDetectees, verdict);
        List<String> messages = construireMessages(protectionsDetectees, nulliteProbable, verdict);

        return new LicenciementNulDetectionResult(
                List.copyOf(protectionsDetectees),
                nombre,
                nulliteProbable,
                scoreNullite,
                verdict,
                indemnite,
                INDEMNITE_MINIMUM_MOIS,
                nulliteProbable,
                baseJuridique,
                formule,
                messages,
                countryNormalized
        );
    }

    private static Set<Protection> detecterProtections(LicenciementNulDetectionInput in) {
        // LinkedHashSet pour conserver un ordre stable (test JSON déterministe).
        Set<Protection> set = new LinkedHashSet<>();
        LocalDate notif = in.dateNotificationLicenciement();

        // MATERNITE — L.1225-4
        if (Boolean.TRUE.equals(in.salarieEnceinte())) {
            set.add(Protection.MATERNITE);
        } else if (in.dateAccouchement() != null) {
            long semaines = ChronoUnit.WEEKS.between(in.dateAccouchement(), notif);
            if (semaines >= 0 && semaines < MATERNITE_FENETRE_SEMAINES) {
                set.add(Protection.MATERNITE);
            }
        }

        // ACCIDENT_TRAVAIL — L.1226-9
        if (Boolean.TRUE.equals(in.salarieAccidentTravail())) {
            set.add(Protection.ACCIDENT_TRAVAIL);
        } else if (in.dateConsolidationAT() != null) {
            long mois = ChronoUnit.MONTHS.between(in.dateConsolidationAT(), notif);
            if (mois >= 0 && mois < AT_FENETRE_MOIS) {
                set.add(Protection.ACCIDENT_TRAVAIL);
            }
        }

        if (Boolean.TRUE.equals(in.salarieHarceleAvere())) {
            set.add(Protection.HARCELEMENT);
        }
        if (Boolean.TRUE.equals(in.salarieDiscriminationAlleguee())) {
            set.add(Protection.DISCRIMINATION);
        }
        if (Boolean.TRUE.equals(in.salarieMotifLanceurAlerte())) {
            set.add(Protection.LANCEUR_ALERTE);
        }
        if (Boolean.TRUE.equals(in.salarieMandatRepresentant())) {
            set.add(Protection.MANDAT_SYNDICAL);
        }
        if (Boolean.TRUE.equals(in.salarieActionJustice())) {
            set.add(Protection.ACTION_JUSTICE);
        }
        return set;
    }

    private static VerdictProbabilite verdictPour(int score) {
        if (score >= 70) return VerdictProbabilite.ELEVEE;
        if (score >= 35) return VerdictProbabilite.MOYENNE;
        return VerdictProbabilite.FAIBLE;
    }

    private static String baseJuridiquePour(Set<Protection> protections) {
        if (protections.isEmpty()) {
            return "Art. L.1235-3-1 al. 2 (aucune protection détectée — vérifier manuellement)";
        }
        StringBuilder sb = new StringBuilder("Art. L.1235-3-1 al. 2");
        for (Protection p : protections) {
            sb.append(" + ");
            sb.append(switch (p) {
                case MATERNITE -> "L.1225-4 (maternité)";
                case ACCIDENT_TRAVAIL -> "L.1226-9 (AT/MP)";
                case HARCELEMENT -> "L.1152-3 (harcèlement)";
                case DISCRIMINATION -> "L.1132-4 (discrimination)";
                case LANCEUR_ALERTE -> "L.1132-3-3 (lanceur d'alerte)";
                case MANDAT_SYNDICAL -> "L.2411-1 (salarié protégé)";
                case ACTION_JUSTICE -> "L.1132-1 al. dernier (action en justice)";
            });
        }
        return sb.toString();
    }

    private static String construireFormule(BigDecimal salaire,
                                            BigDecimal indemnite,
                                            Set<Protection> protections,
                                            VerdictProbabilite verdict) {
        StringBuilder sb = new StringBuilder();
        sb.append("Plancher 6 mois × ").append(format(salaire)).append(" € = ")
                .append(format(indemnite)).append(" €");
        if (protections.isEmpty()) {
            sb.append(" | Aucune protection détectée — nullité ").append(verdict.name());
        } else {
            sb.append(" | ").append(protections.size()).append(" protection")
                    .append(protections.size() > 1 ? "s" : "").append(" détectée")
                    .append(protections.size() > 1 ? "s" : "").append(" (");
            List<String> codes = new ArrayList<>(protections.size());
            for (Protection p : protections) codes.add(p.name());
            sb.append(String.join(" + ", codes));
            sb.append(") → nullité ").append(verdict.name());
        }
        return sb.toString();
    }

    private static List<String> construireMessages(Set<Protection> protections,
                                                   boolean nulliteProbable,
                                                   VerdictProbabilite verdict) {
        List<String> msgs = new ArrayList<>();
        msgs.add("Plancher 6 mois (art. L.1235-3-1 al. 2) — le juge peut allouer davantage selon le préjudice effectif.");
        if (nulliteProbable) {
            msgs.add("Le salarié peut demander la réintégration dans l'entreprise OU une indemnité minimum 6 mois (art. L.1235-3-1).");
            msgs.add("Verdict : probabilité de nullité " + verdict.name() + " (" + protections.size() + " protection(s) active(s)).");
        } else {
            msgs.add("Aucune protection automatiquement détectée — examiner les autres causes possibles (liberté fondamentale, etc.) avant d'écarter la nullité.");
        }
        if (protections.contains(Protection.MANDAT_SYNDICAL)) {
            msgs.add("Salarié protégé (mandat) : licenciement subordonné à autorisation administrative préalable. "
                    + "L'indemnité forfaitaire 6 mois n'est pas cumulable avec l'indemnité de licenciement.");
        }
        if (protections.contains(Protection.MATERNITE)) {
            msgs.add("Période de protection maternité : licenciement nul sauf faute grave non liée à la grossesse "
                    + "ou impossibilité de maintenir le contrat (art. L.1225-4).");
        }
        if (protections.contains(Protection.ACCIDENT_TRAVAIL)) {
            msgs.add("Période de suspension AT/MP : licenciement nul sauf faute grave ou impossibilité de maintenir "
                    + "le contrat pour motif étranger à l'accident (art. L.1226-9).");
        }
        return msgs;
    }

    private static String format(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
