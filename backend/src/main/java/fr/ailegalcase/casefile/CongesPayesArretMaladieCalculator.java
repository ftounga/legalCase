package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SF-206-03 : moteur de chiffrage des congés payés acquis pendant un arrêt
 * maladie (FRANCE, loi 22/04/2024 art. 37 — transposition Cass. soc.
 * 13/09/2023 n°22-17.340 — art. L.3141-5 et L.3141-5-1 CT).
 *
 * <p>Calcul du rappel de congés payés (en jours ouvrables) acquis par le
 * salarié pendant ses arrêts maladie, et détermination du délai d'action
 * encore ouvert :</p>
 *
 * <ul>
 *   <li>Maladie non professionnelle : 2 jours ouvrables / mois, plafonné à
 *       <b>24 jours ouvrables par an</b> (L.3141-5-1).</li>
 *   <li>AT / maladie professionnelle : 2,5 jours ouvrables / mois,
 *       <b>sans limite de durée</b> (loi 22/04/2024 — la limite d'un an
 *       posée par l'ancien L.3141-5 est supprimée).</li>
 *   <li>Jours de rappel = jours acquis − jours déjà accordés / décomptés
 *       par l'employeur (plancher 0).</li>
 *   <li>Valorisation indicative = jours de rappel × salaire journalier
 *       (méthode du dixième : {@code salaireBrutMensuel × 12 / 10 / 24},
 *       valeur indicative — un mode comparatif strict ne fait pas partie
 *       du périmètre de cette SF).</li>
 *   <li>Délai d'action : salarié <b>encore en poste</b> → forclusion de
 *       2 ans à compter du 24/04/2024 (date butoir <b>24/04/2026</b>)
 *       pour la période antérieure à la loi ; salarié <b>sorti</b> →
 *       prescription triennale L.3245-1, soit
 *       {@code dateRuptureContrat + 3 ans}.</li>
 * </ul>
 *
 * <p>Verdict : {@code RAPPEL_SIGNIFICATIF} ({@code joursCpRappel ≥ 5} et
 * action ouverte) / {@code RAPPEL_LIMITE} (0 &lt; {@code joursCpRappel} &lt;
 * 5 et action ouverte) / {@code PAS_DE_RAPPEL} ({@code joursCpRappel = 0}
 * et action ouverte) / {@code ACTION_FORCLOSE} (date limite dépassée à la
 * date du calcul, indépendamment du nombre de jours).</p>
 *
 * <p><b>Pays</b> : FRANCE uniquement — l'acquisition des congés payés
 * pendant arrêt maladie selon le régime L.3141-5 / L.3141-5-1 CT est un
 * mécanisme purement français.</p>
 */
public final class CongesPayesArretMaladieCalculator {

    /** Type d'arrêt à l'origine de l'acquisition des congés payés. */
    public enum TypeArret {
        MALADIE_NON_PROFESSIONNELLE,
        ACCIDENT_TRAVAIL_MALADIE_PRO
    }

    /** Verdict du chiffrage de rappel. */
    public enum Verdict {
        RAPPEL_SIGNIFICATIF,
        RAPPEL_LIMITE,
        PAS_DE_RAPPEL,
        ACTION_FORCLOSE
    }

    // ---------------------------------------------------------------------
    // Règles métier
    // ---------------------------------------------------------------------

    /** 2 jours ouvrables / mois pour maladie non professionnelle (L.3141-5-1). */
    private static final BigDecimal RATE_MALADIE_NON_PRO = new BigDecimal("2");
    /** Plafond annuel pour maladie non professionnelle (L.3141-5-1). */
    private static final BigDecimal PLAFOND_ANNUEL_MALADIE_NON_PRO = new BigDecimal("24");
    /** 2,5 jours ouvrables / mois pour AT/MP — sans plafond (loi 22/04/2024). */
    private static final BigDecimal RATE_ATMP = new BigDecimal("2.5");

    /** Seuil de bascule entre rappel « limité » et « significatif » (en jours). */
    private static final BigDecimal SEUIL_RAPPEL_SIGNIFICATIF = new BigDecimal("5");

    /** Forclusion 24/04/2026 — salarié encore en poste, période antérieure à la loi. */
    private static final LocalDate FORCLUSION_SALARIE_EN_POSTE = LocalDate.of(2026, 4, 24);
    /** Prescription triennale post-rupture (L.3245-1). */
    private static final int PRESCRIPTION_POST_RUPTURE_ANNEES = 3;

    private CongesPayesArretMaladieCalculator() {}

    /**
     * Calcule le rappel de congés payés acquis pendant les arrêts maladie.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace ("FRANCE" uniquement supporté)
     * @return résultat (jours acquis, jours rappel, valorisation, délai, verdict)
     * @throws IllegalArgumentException si validation échoue ou pays non supporté
     */
    public static CongesPayesArretMaladieResult compute(
            CongesPayesArretMaladieInput input, String country) {
        if (input == null) {
            throw new IllegalArgumentException("Input requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil disponible uniquement en FRANCE — l'acquisition des "
                            + "congés payés pendant arrêt maladie au sens des "
                            + "art. L.3141-5 et L.3141-5-1 CT est un mécanisme "
                            + "purement français (loi 22/04/2024).");
        }
        if (input.typeArret() == null) {
            throw new IllegalArgumentException("typeArret requis");
        }
        if (input.nombreMoisArret() == null) {
            throw new IllegalArgumentException("nombreMoisArret requis");
        }
        if (input.nombreMoisArret() <= 0) {
            throw new IllegalArgumentException("nombreMoisArret doit être strictement positif");
        }
        if (input.salarieEncoreEnPoste() == null) {
            throw new IllegalArgumentException("salarieEncoreEnPoste requis");
        }
        if (Boolean.FALSE.equals(input.salarieEncoreEnPoste())
                && input.dateRuptureContrat() == null) {
            throw new IllegalArgumentException(
                    "dateRuptureContrat requise lorsque le salarié n'est plus en poste");
        }
        if (input.salaireBrutMensuel() == null) {
            throw new IllegalArgumentException("salaireBrutMensuel requis");
        }
        if (input.salaireBrutMensuel().signum() < 0) {
            throw new IllegalArgumentException("salaireBrutMensuel doit être positif ou nul");
        }
        BigDecimal dejaAccordes = input.joursCpDejaAccordes() != null
                ? input.joursCpDejaAccordes() : BigDecimal.ZERO;
        if (dejaAccordes.signum() < 0) {
            throw new IllegalArgumentException("joursCpDejaAccordes doit être positif ou nul");
        }

        LocalDate dateCalcul = input.dateCalcul() != null ? input.dateCalcul() : LocalDate.now();

        BigDecimal joursAcquis = calculerJoursAcquis(input.typeArret(), input.nombreMoisArret());
        BigDecimal joursRappel = joursAcquis.subtract(dejaAccordes);
        if (joursRappel.signum() < 0) {
            joursRappel = BigDecimal.ZERO;
        }
        BigDecimal valorisation = valorisationIndicative(input.salaireBrutMensuel(), joursRappel);

        LocalDate dateLimiteAction = dateLimiteAction(input);
        boolean actionEncoreOuverte = !dateCalcul.isAfter(dateLimiteAction);
        Verdict verdict = verdict(joursRappel, actionEncoreOuverte);

        List<String> bases = basesJuridiques(input.typeArret());
        List<String> messages = messages(input, joursAcquis, joursRappel, dateLimiteAction,
                actionEncoreOuverte, verdict);

        return new CongesPayesArretMaladieResult(
                joursAcquis,
                joursRappel,
                valorisation,
                dateLimiteAction,
                actionEncoreOuverte,
                verdict,
                bases,
                messages,
                countryNormalized
        );
    }

    /**
     * Calcul des jours ouvrables acquis selon le type d'arrêt.
     *
     * <ul>
     *   <li>Maladie non pro : 2 j × mois, plafond 24 j/an sur la part annuelle —
     *       prorata appliqué par tranches de 12 mois.</li>
     *   <li>AT/MP : 2,5 j × mois — sans plafond (la limite d'un an est supprimée).</li>
     * </ul>
     */
    private static BigDecimal calculerJoursAcquis(TypeArret type, int nombreMois) {
        if (type == TypeArret.ACCIDENT_TRAVAIL_MALADIE_PRO) {
            return RATE_ATMP.multiply(BigDecimal.valueOf(nombreMois))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        // Maladie non pro : plafond 24 j par tranche de 12 mois
        int anneesEntieres = nombreMois / 12;
        int moisRestants = nombreMois % 12;
        BigDecimal parTrancheComplete = PLAFOND_ANNUEL_MALADIE_NON_PRO
                .multiply(BigDecimal.valueOf(anneesEntieres));
        BigDecimal parTrancheIncomplete = RATE_MALADIE_NON_PRO
                .multiply(BigDecimal.valueOf(moisRestants));
        // Plafonner la tranche incomplète au plafond annuel.
        if (parTrancheIncomplete.compareTo(PLAFOND_ANNUEL_MALADIE_NON_PRO) > 0) {
            parTrancheIncomplete = PLAFOND_ANNUEL_MALADIE_NON_PRO;
        }
        return parTrancheComplete.add(parTrancheIncomplete)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Valorisation indicative selon la méthode du dixième :
     * {@code salaireBrutMensuel × 12 / 10 / 24 × joursRappel}.
     * Indicatif — un comparatif strict avec le maintien de salaire est hors périmètre.
     */
    private static BigDecimal valorisationIndicative(BigDecimal salaireBrut, BigDecimal joursRappel) {
        if (salaireBrut == null || joursRappel == null || joursRappel.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        // salaire journalier (méthode du dixième) = salaire × 12 / 10 / 24
        BigDecimal salaireJournalier = salaireBrut
                .multiply(BigDecimal.valueOf(12))
                .divide(BigDecimal.valueOf(10), 6, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(24), 6, RoundingMode.HALF_UP);
        return salaireJournalier.multiply(joursRappel)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Délai d'action :
     * <ul>
     *   <li>Salarié encore en poste → forclusion 24/04/2026 (loi 22/04/2024).</li>
     *   <li>Salarié sorti → dateRuptureContrat + 3 ans (prescription L.3245-1).</li>
     * </ul>
     */
    private static LocalDate dateLimiteAction(CongesPayesArretMaladieInput input) {
        if (Boolean.TRUE.equals(input.salarieEncoreEnPoste())) {
            return FORCLUSION_SALARIE_EN_POSTE;
        }
        return input.dateRuptureContrat().plusYears(PRESCRIPTION_POST_RUPTURE_ANNEES);
    }

    private static Verdict verdict(BigDecimal joursRappel, boolean actionEncoreOuverte) {
        if (!actionEncoreOuverte) {
            return Verdict.ACTION_FORCLOSE;
        }
        if (joursRappel.signum() <= 0) {
            return Verdict.PAS_DE_RAPPEL;
        }
        if (joursRappel.compareTo(SEUIL_RAPPEL_SIGNIFICATIF) >= 0) {
            return Verdict.RAPPEL_SIGNIFICATIF;
        }
        return Verdict.RAPPEL_LIMITE;
    }

    private static List<String> basesJuridiques(TypeArret type) {
        Set<String> bases = new LinkedHashSet<>();
        bases.add("Loi n°2024-364 du 22 avril 2024, art. 37");
        if (type == TypeArret.ACCIDENT_TRAVAIL_MALADIE_PRO) {
            bases.add("Art. L.3141-5 C. trav. (AT/MP — sans limite de durée depuis la loi 22/04/2024)");
        } else {
            bases.add("Art. L.3141-5-1 C. trav. (maladie non pro — plafond 24 j ouvrables/an)");
        }
        bases.add("Cass. soc. 13/09/2023 n°22-17.340 (transposition CJUE — acquisition pendant arrêt maladie)");
        bases.add("Art. L.3245-1 C. trav. (prescription triennale salariale)");
        return new ArrayList<>(bases);
    }

    private static List<String> messages(CongesPayesArretMaladieInput in,
                                          BigDecimal joursAcquis,
                                          BigDecimal joursRappel,
                                          LocalDate dateLimiteAction,
                                          boolean actionEncoreOuverte,
                                          Verdict verdict) {
        List<String> msgs = new ArrayList<>();
        switch (verdict) {
            case RAPPEL_SIGNIFICATIF -> msgs.add(
                    "Rappel significatif identifié : " + joursRappel.toPlainString()
                            + " j ouvrables de congés payés peuvent être réclamés. "
                            + "Action recevable jusqu'au " + dateLimiteAction
                            + " — engager la mise en demeure avant cette date.");
            case RAPPEL_LIMITE -> msgs.add(
                    "Rappel limité : " + joursRappel.toPlainString()
                            + " j ouvrables. Vérifier l'opportunité d'une action vu "
                            + "l'enjeu — action recevable jusqu'au " + dateLimiteAction + ".");
            case PAS_DE_RAPPEL -> msgs.add(
                    "Aucun rappel : les congés acquis (" + joursAcquis.toPlainString()
                            + " j) sont déjà couverts par les jours décomptés par l'employeur.");
            case ACTION_FORCLOSE -> msgs.add(
                    "Action forclose : la date limite (" + dateLimiteAction
                            + ") est dépassée à la date du calcul. La demande de rappel "
                            + "est irrecevable, sauf cause d'interruption à documenter.");
        }
        if (Boolean.TRUE.equals(in.salarieEncoreEnPoste())) {
            msgs.add("Salarié encore en poste : la forclusion 24/04/2026 (loi 22/04/2024) "
                    + "vise la période antérieure à la loi — agir avant cette date butoir.");
        } else {
            msgs.add("Salarié sorti : prescription triennale L.3245-1 — délai courant à compter "
                    + "de la date de rupture du contrat.");
        }
        if (in.typeArret() == TypeArret.MALADIE_NON_PROFESSIONNELLE
                && in.nombreMoisArret() != null && in.nombreMoisArret() > 12) {
            msgs.add("Arrêt maladie non professionnel > 12 mois : plafond annuel de 24 j "
                    + "ouvrables appliqué (L.3141-5-1).");
        }
        msgs.add("Valorisation indicative selon la méthode du dixième — un comparatif "
                + "avec le maintien de salaire peut affiner le chiffrage selon la "
                + "convention collective applicable.");
        return msgs;
    }
}
