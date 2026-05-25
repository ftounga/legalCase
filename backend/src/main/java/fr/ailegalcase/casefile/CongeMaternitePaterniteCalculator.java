package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-212-29 : moteur d'analyse de la durée du congé maternité / paternité,
 * des indemnités journalières CPAM, de la protection contre le licenciement
 * et des délais d'action (F-DT-77-conge-paternite-maternite, FRANCE —
 * L. 1225-1 à L. 1225-40 CT ; L. 331-3 CSS ; loi du 16/03/2021 portant
 * réforme du congé paternité).
 *
 * <p><b>Durée légale</b> (L. 1225-17 CT — maternité) :
 * <ul>
 *   <li>16 semaines (= 112 jours calendaires) — 1er ou 2e enfant.</li>
 *   <li>26 semaines (= 182 jours) — 3e enfant et plus.</li>
 *   <li>34 semaines (= 238 jours) — naissance multiple, jumeaux (rang &lt; 3).</li>
 *   <li>46 semaines (= 322 jours) — naissance multiple, triplés et plus
 *       (rang &lt; 3 mais surclassé par 3+ déclaré).</li>
 * </ul>
 *
 * <p><b>Paternité</b> (L. 1225-35 al. 1 et 2 CT — loi 16/03/2021) :
 * <ul>
 *   <li>25 jours calendaires (= 4 jours obligatoires + 21 jours
 *       supplémentaires), naissance simple.</li>
 *   <li>32 jours calendaires (= 4 + 28), naissance multiple.</li>
 * </ul>
 *
 * <p><b>Indemnités journalières CPAM</b> (L. 331-3 CSS) : 100 % du salaire
 * journalier de référence, plafonné au PMSS / 30,42 ≈ 100 € net / jour en
 * 2026. Approximation utilisée ici : salaire mensuel brut / 30,42 (×
 * facteur net 0,8 — conversion estimative pour exposer une IJ proche du
 * net effectivement perçu).
 *
 * <p><b>Protection licenciement</b> (L. 1225-4 CT pour maternité,
 * L. 1225-4-1 CT pour paternité) : interdiction de licencier pendant le
 * congé, étendue à 10 semaines (= 70 jours calendaires) après le retour
 * (sauf faute grave ou impossibilité de maintien du contrat pour motif
 * étranger à la grossesse / paternité).
 *
 * <p><b>Pays</b> : FRANCE uniquement — le régime BE (loi 16/03/1971 et
 * AR 25/11/1991) repose sur l'INAMI / mutualité ; outil jumeau hors scope.
 */
public final class CongeMaternitePaterniteCalculator {

    /** Code structuré d'un point d'alerte (F-IA-03). */
    public enum CodeCritere {
        DT77_TYPE_CONGE,
        DT77_DUREE_CONGE,
        DT77_PROTECTION_LICENCIEMENT,
        DT77_RETOUR_POSTE
    }

    /** Résultat calculé par {@link #compute}. */
    public record Result(
            int dureeCongeJours,
            LocalDate dateFinCongeTheorique,
            double ijJournaliereEstimeeEuros,
            double ijTotaleEstimeeEuros,
            LocalDate protectionLicenciementJusqua,
            boolean alerteLicenciementIllegal,
            boolean alerteRetourPosteIllegal,
            List<String> basesJuridiques,
            List<String> messages,
            String country
    ) {}

    private static final double PMSS_2026_MENSUEL = 3925.0;          // L. 241-3 CSS
    private static final double JOURS_PAR_MOIS = 30.4375;             // 365.25 / 12
    private static final double FACTEUR_NET_APPROX = 0.80;            // approximation IJ ≈ 80% brut net
    private static final int JOURS_PROTECTION_POST_RETOUR = 70;       // 10 sem. L. 1225-4
    /** Plafond de salaire mensuel acceptable en input (anti-aberration). */
    private static final double SALAIRE_MAX_PLAUSIBLE = 1_000_000.0;
    /** Rang enfant maximum plausible. */
    private static final int RANG_ENFANT_MAX_PLAUSIBLE = 20;

    private CongeMaternitePaterniteCalculator() {}

    /**
     * Point d'entrée principal.
     *
     * @param input   données saisies par l'avocat (snapshot UI)
     * @param country pays du workspace
     * @return résultat calculé
     * @throws IllegalArgumentException si {@code country != "FRANCE"}, si
     *         {@code input} est null, ou si la combinaison est aberrante.
     */
    public static Result compute(CongeMaternitePaterniteInput input, String country) {
        if (!"FRANCE".equalsIgnoreCase(country)) {
            throw new IllegalArgumentException(
                    "Outil réservé au droit du travail FRANCE — pays détecté : " + country);
        }
        if (input == null) {
            throw new IllegalArgumentException("Données d'entrée manquantes");
        }
        if (input.typeConge() == null) {
            throw new IllegalArgumentException("Type de congé requis (MATERNITE ou PATERNITE)");
        }
        if (input.rangEnfant() < 1 || input.rangEnfant() > RANG_ENFANT_MAX_PLAUSIBLE) {
            throw new IllegalArgumentException(
                    "Rang d'enfant aberrant : " + input.rangEnfant() + " (attendu entre 1 et "
                            + RANG_ENFANT_MAX_PLAUSIBLE + ")");
        }
        if (input.salaireMensuelBrutEuros() < 0
                || input.salaireMensuelBrutEuros() > SALAIRE_MAX_PLAUSIBLE) {
            throw new IllegalArgumentException(
                    "Salaire mensuel brut hors plage plausible : " + input.salaireMensuelBrutEuros());
        }
        if (input.dateDebutConge() == null) {
            throw new IllegalArgumentException("Date de début de congé requise");
        }

        boolean isMaternite =
                input.typeConge() == CongeMaternitePaterniteInput.TypeConge.MATERNITE;

        // ── 1. Durée du congé ────────────────────────────────────────────────
        int dureeJours = computeDureeJours(input, isMaternite);

        // ── 2. Date de fin théorique ─────────────────────────────────────────
        // dureeJours = nombre de jours calendaires de congé → date début + dureeJours.
        LocalDate dateFin = input.dateDebutConge().plusDays(dureeJours);

        // ── 3. IJ CPAM ───────────────────────────────────────────────────────
        // SJR ≈ salaire mensuel brut / 30.4375 ; IJ ≈ SJR × 80 % (approx net plafonnée PMSS).
        double salaireJournalierBrut = input.salaireMensuelBrutEuros() / JOURS_PAR_MOIS;
        double plafondJournalier = PMSS_2026_MENSUEL / JOURS_PAR_MOIS; // ≈ 128.96 €
        double ijBase = Math.min(salaireJournalierBrut, plafondJournalier) * FACTEUR_NET_APPROX;
        double ijJournaliere = round2(ijBase);
        double ijTotale = round2(ijJournaliere * dureeJours);

        // ── 4. Protection licenciement ───────────────────────────────────────
        // Maternité : 10 semaines post-retour (L. 1225-4). Paternité : équivalent
        // période courte post-congé (10 semaines aussi par alignement L. 1225-4-1).
        LocalDate protectionJusqua = dateFin.plusDays(JOURS_PROTECTION_POST_RETOUR);

        // ── 5. Alertes ───────────────────────────────────────────────────────
        boolean alerteLicenciement = input.licenciementPendantConge();
        boolean alerteRetourPoste = input.retourPosteDifferent();

        // ── 6. Bases juridiques + messages ──────────────────────────────────
        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        if (isMaternite) {
            bases.add("L. 1225-17 CT — durée légale du congé maternité (16/26/34/46 sem.)");
            bases.add("L. 331-3 CSS — indemnités journalières maternité (100 % SJR plafonné PMSS)");
            bases.add("L. 1225-4 CT — protection contre le licenciement (congé + 10 sem. post-retour)");
            bases.add("L. 1225-25 CT et L. 1225-27 CT — retour à un poste identique ou équivalent + entretien professionnel");
            messages.add("Congé maternité — durée estimée " + dureeJours
                    + " jours calendaires ; fin théorique " + dateFin + ".");
        } else {
            bases.add("L. 1225-35 CT — congé paternité (25 j calendaires, 32 j naissance multiple ; 4 j obligatoires) — loi 16/03/2021");
            bases.add("L. 1225-36 CT — modalités de prise du congé paternité");
            bases.add("L. 331-3 CSS — indemnités journalières (régime aligné sur la maternité)");
            bases.add("L. 1225-4-1 CT — protection contre le licenciement (équivalent paternité)");
            messages.add("Congé paternité — durée estimée " + dureeJours
                    + " jours calendaires ; fin théorique " + dateFin
                    + ". Loi du 16/03/2021 — 4 jours obligatoires consécutifs à la naissance.");
        }
        messages.add("IJ CPAM estimée " + ijJournaliere + " €/jour ; total estimé "
                + ijTotale + " € sur la durée du congé (approximation ; voir CPAM pour le calcul officiel).");
        messages.add("Période de protection contre le licenciement jusqu'au "
                + protectionJusqua + " (congé + 10 semaines post-retour).");

        if (alerteLicenciement) {
            messages.add("ALERTE — un licenciement notifié pendant la période protégée est nul de plein droit (sauf faute grave ou impossibilité de maintien sans lien avec la grossesse/paternité — Cass. soc. 28/01/2016 n°14-26.706).");
            bases.add(isMaternite
                    ? "L. 1225-4 CT al. 1 — nullité du licenciement maternité"
                    : "L. 1225-4-1 CT — nullité du licenciement paternité");
        }
        if (alerteRetourPoste) {
            messages.add("ALERTE — la réintégration sur un poste différent au retour est susceptible de caractériser un manquement à l'obligation de retour au poste identique ou équivalent (L. 1225-25 CT) ; ouvre la voie à une demande de résiliation judiciaire ou à des dommages-intérêts.");
            bases.add("L. 1225-25 CT — retour à un poste identique ou équivalent");
        }

        return new Result(
                dureeJours,
                dateFin,
                ijJournaliere,
                ijTotale,
                protectionJusqua,
                alerteLicenciement,
                alerteRetourPoste,
                List.copyOf(bases),
                List.copyOf(messages),
                "FRANCE"
        );
    }

    /** Détermine la durée du congé en jours calendaires selon les règles L. 1225-17 / L. 1225-35. */
    private static int computeDureeJours(CongeMaternitePaterniteInput input, boolean isMaternite) {
        if (isMaternite) {
            // 1er/2e enfant → 16 sem ; 3e+ → 26 sem ; jumeaux → 34 sem ; triplés+ → 46 sem.
            // Naissance multiple écrase le rang : on traite "jumeaux" comme défaut multiple,
            // mais si rangEnfant ≥ 3 ET multiple → on prend 46 sem (triplés et plus déclarés).
            if (input.naissanceMultiple()) {
                return input.rangEnfant() >= 3 ? 7 * 46 : 7 * 34;
            }
            return input.rangEnfant() >= 3 ? 7 * 26 : 7 * 16;
        }
        // Paternité — 25 j ou 32 j (multiple). Le rang ne joue pas.
        return input.naissanceMultiple() ? 32 : 25;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
