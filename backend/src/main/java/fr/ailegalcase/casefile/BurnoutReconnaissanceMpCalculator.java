package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-212-27 : moteur d'évaluation des chances de reconnaissance du burn-out
 * comme maladie professionnelle hors tableau (FRANCE) — art. L. 461-1 al. 4
 * et 5 du Code de la sécurité sociale ; tableau 57 des maladies
 * professionnelles (affections péri-articulaires, sans le burn-out) ;
 * comité régional de reconnaissance des maladies professionnelles (CRRMP) ;
 * circulaire DGT 2016/01 sur la prise en charge des pathologies psychiques.
 *
 * <p>Le burn-out (syndrome d'épuisement professionnel) n'est inscrit à
 * aucun tableau de maladies professionnelles — la reconnaissance passe
 * exclusivement par la <b>procédure hors tableau</b> (L. 461-1 al. 4) qui
 * exige :
 * <ul>
 *   <li>un <b>diagnostic médical</b> caractérisant la pathologie,</li>
 *   <li>un <b>taux d'IPP ≥ 25 %</b> (al. 4) OU le <b>décès du salarié</b> (al. 5),</li>
 *   <li>un <b>lien direct</b> entre la pathologie et le travail (rapport
 *       médical, expertise CRRMP).</li>
 * </ul>
 *
 * <p>Le CRRMP statue après instruction par la CPAM (médecin-conseil,
 * médecin inspecteur régional du travail, médecin spécialiste). Délai
 * d'instruction : <b>4 à 6 mois</b>. La reconnaissance ouvre droit à la
 * rente / indemnité MP et permet l'action en faute inexcusable
 * (L. 452-1 CSS, couverte par F-DT-91).</p>
 *
 * <p><b>Pays</b> : FRANCE uniquement — la procédure CRRMP est franco-française.
 * La BE reconnaît le burn-out via Fedris dans un cadre distinct, hors
 * périmètre de cet outil.</p>
 */
public final class BurnoutReconnaissanceMpCalculator {

    /** Verdict d'évaluation des chances de reconnaissance CRRMP. */
    public enum AnalyseBurnoutMp {
        CHANCES_IMPORTANTES,
        CHANCES_MODEREES,
        CHANCES_FAIBLES
    }

    /** Code structuré d'un facteur d'analyse F-IA-03. */
    public enum CodeCritere {
        DT64_DIAGNOSTIC_POSE,
        DT64_TAUX_IPP,
        DT64_LIEN_CAUSAL,
        DT64_SURCHARGE_DOCUMENTEE
    }

    /** Facteur du dossier — exposé dans la réponse API. */
    public record FacteurDossier(
            CodeCritere code,
            String libelle,
            String fondement,
            int poids
    ) {}

    /** Seuil légal d'IPP — L. 461-1 al. 4 CSS. */
    public static final double SEUIL_IPP_PROCEDURE_HORS_TABLEAU = 25.0;

    /** Délai minimum d'instruction CRRMP (mois) — circulaire DGT. */
    public static final int DELAI_INSTRUCTION_MIN_MOIS = 4;

    /** Délai maximum d'instruction CRRMP (mois) — circulaire DGT. */
    public static final int DELAI_INSTRUCTION_MAX_MOIS = 6;

    /** Résultat calculé par {@link #compute}. */
    public record Result(
            AnalyseBurnoutMp analyseChancesCRRMP,
            int scoreChances,
            boolean conditionIPPRemplie,
            boolean alerteIPPInsuffisante,
            int delaiInstructionMois,
            List<FacteurDossier> facteursDossier,
            List<String> basesJuridiques,
            List<String> messages,
            String country
    ) {}

    private BurnoutReconnaissanceMpCalculator() {}

    /**
     * Point d'entrée principal.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace
     * @return résultat calculé
     * @throws IllegalArgumentException si {@code country != "FRANCE"} ou si
     *         {@code input} est null.
     */
    public static Result compute(BurnoutReconnaissanceMpInput input, String country) {
        if (!"FRANCE".equalsIgnoreCase(country)) {
            throw new IllegalArgumentException(
                    "Outil réservé au droit du travail FRANCE (CRRMP L. 461-1 CSS) — pays détecté : " + country);
        }
        if (input == null) {
            throw new IllegalArgumentException("Données d'entrée manquantes");
        }

        List<FacteurDossier> facteurs = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        int score = 0;

        // ── 1. Diagnostic posé ────────────────────────────────────────────────
        boolean diagnostic = Boolean.TRUE.equals(input.diagnosticBurnoutPose());
        if (diagnostic) {
            facteurs.add(new FacteurDossier(
                    CodeCritere.DT64_DIAGNOSTIC_POSE,
                    "Diagnostic de burn-out posé par une pièce médicale",
                    "Circulaire DGT 2016/01 — pathologies psychiques",
                    20));
            score += 20;
            bases.add("Circulaire DGT 2016/01 — prise en charge des pathologies psychiques");
        } else {
            facteurs.add(new FacteurDossier(
                    CodeCritere.DT64_DIAGNOSTIC_POSE,
                    "Diagnostic médical absent ou non caractérisé",
                    "Circulaire DGT 2016/01",
                    0));
            messages.add("ALERTE : aucun diagnostic médical caractérisant le burn-out n'est documenté — pièce-clef pour la saisine CRRMP.");
        }

        // ── 2. Taux IPP (condition légale L. 461-1 al. 4) ─────────────────────
        double taux = input.tauxIPPEstime() != null ? input.tauxIPPEstime() : 0.0;
        boolean conditionIPPRemplie = taux >= SEUIL_IPP_PROCEDURE_HORS_TABLEAU;
        boolean alerteIPPInsuffisante = taux < SEUIL_IPP_PROCEDURE_HORS_TABLEAU;
        if (conditionIPPRemplie) {
            facteurs.add(new FacteurDossier(
                    CodeCritere.DT64_TAUX_IPP,
                    String.format("Taux IPP estimé %.0f %% — seuil légal 25 %% atteint", taux),
                    "L. 461-1 al. 4 CSS — seuil minimal IPP pour procédure hors tableau",
                    30));
            score += 30;
            bases.add("L. 461-1 al. 4 CSS — procédure hors tableau ouverte au taux IPP ≥ 25 %");
        } else {
            facteurs.add(new FacteurDossier(
                    CodeCritere.DT64_TAUX_IPP,
                    String.format("Taux IPP estimé %.0f %% — INFÉRIEUR au seuil légal 25 %%", taux),
                    "L. 461-1 al. 4 CSS — condition d'ouverture non remplie",
                    0));
            messages.add(String.format(
                    "CONDITION LÉGALE NON REMPLIE : taux IPP estimé %.0f %% < 25 %% — la procédure CRRMP hors tableau est irrecevable (L. 461-1 al. 4 CSS), sauf décès du salarié (al. 5).",
                    taux));
        }

        // ── 3. Lien causal direct ─────────────────────────────────────────────
        boolean lienCausal = Boolean.TRUE.equals(input.lienCausalDirectEtabli());
        if (lienCausal) {
            facteurs.add(new FacteurDossier(
                    CodeCritere.DT64_LIEN_CAUSAL,
                    "Lien direct travail / pathologie établi par pièce médicale",
                    "L. 461-1 al. 4 CSS — exigence du lien direct et essentiel",
                    25));
            score += 25;
            bases.add("L. 461-1 al. 4 CSS — exigence du lien direct entre la pathologie et le travail");
        } else {
            facteurs.add(new FacteurDossier(
                    CodeCritere.DT64_LIEN_CAUSAL,
                    "Lien direct travail / pathologie non documenté",
                    "L. 461-1 al. 4 CSS",
                    0));
            messages.add("ALERTE : le lien direct entre la pathologie et le travail n'est pas établi — le CRRMP statue sur ce critère.");
        }

        // ── 4. Surcharge / manquements sécurité ──────────────────────────────
        boolean surcharge = Boolean.TRUE.equals(input.surchargeChargeDocumentee());
        boolean manquements = Boolean.TRUE.equals(input.manquementsSecuriteDocumentes());
        boolean harcelement = Boolean.TRUE.equals(input.harcelementConcomitant());
        int poidsSurcharge = 0;
        StringBuilder libelleSurcharge = new StringBuilder("Facteurs contextuels documentés : ");
        if (surcharge) { poidsSurcharge += 7; libelleSurcharge.append("surcharge, "); }
        if (manquements) { poidsSurcharge += 8; libelleSurcharge.append("manquements L. 4121-1, "); }
        if (harcelement) { poidsSurcharge += 5; libelleSurcharge.append("harcèlement, "); }
        if (Boolean.TRUE.equals(input.arretsMaladieMultiples())) {
            poidsSurcharge += 5;
            libelleSurcharge.append("arrêts maladie multiples, ");
        }
        if (poidsSurcharge > 0) {
            String libelle = libelleSurcharge.substring(0, libelleSurcharge.length() - 2);
            facteurs.add(new FacteurDossier(
                    CodeCritere.DT64_SURCHARGE_DOCUMENTEE,
                    libelle,
                    "L. 4121-1 CT — obligation de sécurité de l'employeur ; L. 1152-1 CT",
                    poidsSurcharge));
            score += poidsSurcharge;
            if (manquements) {
                bases.add("L. 4121-1 CT — obligation de sécurité (manquements documentés)");
            }
            if (harcelement) {
                bases.add("L. 1152-1 CT — harcèlement moral");
            }
        } else {
            facteurs.add(new FacteurDossier(
                    CodeCritere.DT64_SURCHARGE_DOCUMENTEE,
                    "Aucun facteur contextuel (surcharge / manquements / harcèlement) documenté",
                    "L. 4121-1 CT",
                    0));
            messages.add("Aucun facteur contextuel n'est documenté — le faisceau d'indices au soutien du CRRMP est faible.");
        }

        // Bonus ancienneté d'exposition (≥ 5 ans = facteur de gravité).
        Integer annees = input.anneesExpositionProfessionnelle();
        if (annees != null && annees >= 5) {
            score += 5;
        }

        // ── 5. Verdict ────────────────────────────────────────────────────────
        AnalyseBurnoutMp verdict;
        if (!conditionIPPRemplie) {
            verdict = AnalyseBurnoutMp.CHANCES_FAIBLES;
            messages.add(0, "CHANCES FAIBLES — la condition légale d'IPP ≥ 25 % n'est pas remplie ; la procédure CRRMP hors tableau est en l'état irrecevable (L. 461-1 al. 4 CSS).");
        } else if (diagnostic && lienCausal && (surcharge || manquements)) {
            verdict = AnalyseBurnoutMp.CHANCES_IMPORTANTES;
            messages.add(0, "CHANCES IMPORTANTES — diagnostic posé, lien causal établi et faisceau d'indices contextuel documenté. Le dossier CRRMP est solide.");
        } else if (score >= 50) {
            verdict = AnalyseBurnoutMp.CHANCES_MODEREES;
            messages.add(0, "CHANCES MODÉRÉES — éléments présents mais incomplets ; pièces complémentaires recommandées avant saisine CRRMP.");
        } else {
            verdict = AnalyseBurnoutMp.CHANCES_FAIBLES;
            messages.add(0, "CHANCES FAIBLES — faisceau d'indices insuffisant ; consolider le dossier médical et la documentation des conditions de travail.");
        }

        // ── 6. Bases juridiques transverses ──────────────────────────────────
        bases.add("L. 461-1 CSS — reconnaissance des maladies professionnelles (procédure CRRMP hors tableau)");
        bases.add("Circulaire DGT 2016/01 — instruction des pathologies psychiques par les CRRMP");
        bases.add("Tableau 57 des maladies professionnelles — affections péri-articulaires (n'inclut PAS le burn-out)");
        if (verdict == AnalyseBurnoutMp.CHANCES_IMPORTANTES
                || verdict == AnalyseBurnoutMp.CHANCES_MODEREES) {
            messages.add("Si la reconnaissance MP est obtenue, une action en faute inexcusable de l'employeur (L. 452-1 CSS) peut être engagée — couvert par F-DT-91.");
        }

        // Borner le score à [0, 100].
        score = Math.max(0, Math.min(100, score));

        // Délai d'instruction CRRMP (médiane = 5 mois ; on retourne la borne basse
        // par défaut, l'écran affiche le range 4-6 mois dans l'UI).
        int delaiMois = DELAI_INSTRUCTION_MIN_MOIS;

        return new Result(
                verdict,
                score,
                conditionIPPRemplie,
                alerteIPPInsuffisante,
                delaiMois,
                List.copyOf(facteurs),
                List.copyOf(bases),
                List.copyOf(messages),
                "FRANCE"
        );
    }
}
