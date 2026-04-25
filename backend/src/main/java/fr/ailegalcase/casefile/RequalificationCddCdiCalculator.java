package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SF-DT-22-01 : analyseur de requalification CDD → CDI
 * (art. L.1245-1, L.1245-2, L.1242-12, L.1244-3 Code du travail).
 *
 * Score additif (0-100, plafonné) sur 4 critères :
 * - motif structurellement interdit (art. L.1242-1, L.1242-3) → +50
 * - succession ≥ 3 CDD → +20
 * - délai de carence non respecté (art. L.1244-3) → +20
 * - motif invoqué AUTRE ou EMPLOI_USAGE (souvent contesté) → +10
 *
 * Verdict : ELEVEE ≥ 60, MOYENNE 30-59, FAIBLE < 30.
 *
 * Indemnité de requalification : plancher 1 mois de salaire (art. L.1245-2).
 * Rappel précarité (art. L.1243-8) : 10 % × salaire mensuel × durée contrat.
 */
public final class RequalificationCddCdiCalculator {

    static final Set<String> MOTIFS_CDD_INVOQUES = Set.of(
            "ACCROISSEMENT_TEMPORAIRE",
            "REMPLACEMENT_SALARIE",
            "EMPLOI_SAISONNIER",
            "EMPLOI_USAGE",
            "CONTRAT_VENDANGE",
            "INSERTION_PROFESSIONNELLE",
            "AUTRE"
    );

    static final Set<String> MOTIFS_INTERDITS_TYPES = Set.of(
            "EMPLOI_PERMANENT",
            "REMPLACEMENT_GREVISTE",
            "TRAVAUX_DANGEREUX",
            "AUTRE"
    );

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal PRECARITE_RATE = new BigDecimal("10");
    private static final String BASE_JURIDIQUE =
            "Art. L.1245-2 Code travail (1 mois minimum) + L.1243-8 (précarité)";

    private RequalificationCddCdiCalculator() {}

    public static RequalificationCddCdiResult compute(RequalificationCddCdiRequest request) {
        // -- Validations ---------------------------------------------------
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.motifCddInvoque() == null || request.motifCddInvoque().isBlank()) {
            throw new IllegalArgumentException("Motif CDD invoqué requis");
        }
        if (!MOTIFS_CDD_INVOQUES.contains(request.motifCddInvoque())) {
            throw new IllegalArgumentException("Motif CDD invoqué inconnu : "
                    + request.motifCddInvoque() + ". Valeurs acceptées : " + MOTIFS_CDD_INVOQUES);
        }
        if (request.motifInterdit() == null) {
            throw new IllegalArgumentException("motifInterdit requis (true/false)");
        }
        if (Boolean.TRUE.equals(request.motifInterdit())) {
            if (request.motifInterditType() == null || request.motifInterditType().isBlank()) {
                throw new IllegalArgumentException("Type de motif interdit requis quand motifInterdit=true");
            }
            if (!MOTIFS_INTERDITS_TYPES.contains(request.motifInterditType())) {
                throw new IllegalArgumentException("Type de motif interdit inconnu : "
                        + request.motifInterditType() + ". Valeurs acceptées : " + MOTIFS_INTERDITS_TYPES);
            }
        }
        if (request.successionCdd() == null || request.successionCdd().isEmpty()) {
            throw new IllegalArgumentException("Au moins un CDD requis dans la succession");
        }
        for (int i = 0; i < request.successionCdd().size(); i++) {
            RequalificationCddCdiRequest.CddSuccessionEntry e = request.successionCdd().get(i);
            if (e == null || e.dateDebut() == null || e.dateFin() == null) {
                throw new IllegalArgumentException("Dates requises pour le CDD #" + (i + 1));
            }
            if (e.dateDebut().isAfter(e.dateFin())) {
                throw new IllegalArgumentException("Dates incohérentes pour le CDD #" + (i + 1)
                        + " : dateDebut=" + e.dateDebut() + " > dateFin=" + e.dateFin());
            }
        }
        if (request.delaiCarenceRespecte() == null) {
            throw new IllegalArgumentException("delaiCarenceRespecte requis (true/false)");
        }
        if (request.dureeContratMois() == null || request.dureeContratMois() <= 0) {
            throw new IllegalArgumentException("Durée contrat (mois) requise et > 0");
        }
        if (request.salaireMensuelBrutEur() == null || request.salaireMensuelBrutEur().signum() <= 0) {
            throw new IllegalArgumentException("Salaire mensuel brut requis et > 0");
        }
        if (request.dateFinDernierContrat() == null) {
            throw new IllegalArgumentException("Date de fin du dernier contrat requise");
        }

        // -- Scoring -------------------------------------------------------
        int score = 0;
        if (Boolean.TRUE.equals(request.motifInterdit())) {
            score += 50;
        }
        if (request.successionCdd().size() >= 3) {
            score += 20;
        }
        if (Boolean.FALSE.equals(request.delaiCarenceRespecte())) {
            score += 20;
        }
        if ("AUTRE".equals(request.motifCddInvoque()) || "EMPLOI_USAGE".equals(request.motifCddInvoque())) {
            score += 10;
        }
        if (score > 100) {
            score = 100;
        }

        String verdict;
        if (score >= 60) {
            verdict = "ELEVEE";
        } else if (score >= 30) {
            verdict = "MOYENNE";
        } else {
            verdict = "FAIBLE";
        }

        // -- Indemnités ----------------------------------------------------
        BigDecimal salaire = request.salaireMensuelBrutEur().setScale(2, RoundingMode.HALF_UP);
        // Plancher art. L.1245-2 : 1 mois de salaire
        BigDecimal indemniteRequalif = salaire.max(salaire).setScale(2, RoundingMode.HALF_UP);

        // Rappel précarité art. L.1243-8 : 10 % × total rémunérations
        BigDecimal totalRemunerations = salaire.multiply(BigDecimal.valueOf(request.dureeContratMois()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal indemnitePrecarite = totalRemunerations.multiply(PRECARITE_RATE)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

        BigDecimal total = indemniteRequalif.add(indemnitePrecarite).setScale(2, RoundingMode.HALF_UP);

        // -- Formule -------------------------------------------------------
        String formule = String.format(
                "Indemnité requalification = 1 × %s € (plancher art. L.1245-2). "
                        + "Précarité 10 %% × %s € = %s €.",
                formatMontant(salaire),
                formatMontant(totalRemunerations),
                formatMontant(indemnitePrecarite));

        // -- Messages ------------------------------------------------------
        List<String> messages = new ArrayList<>();
        // Prescription
        LocalDate prescriptionLimit = request.dateFinDernierContrat().plusYears(1);
        messages.add("Action en requalification prescrite par 12 mois suivant le terme du dernier contrat "
                + "(art. L.1471-1 + Cass. soc. 29 janv. 2020) — date limite : " + prescriptionLimit + ".");

        if (Boolean.TRUE.equals(request.motifInterdit())) {
            messages.add("Motif interdit identifié (" + request.motifInterditType()
                    + ") : forte présomption de requalification (art. L.1242-1 / L.1242-3).");
        }
        if (request.successionCdd().size() >= 3) {
            messages.add("Succession de " + request.successionCdd().size()
                    + " CDD : risque accru de requalification — vérifier l'art. L.1244-1 (succession sur même poste).");
        }
        if (Boolean.FALSE.equals(request.delaiCarenceRespecte())) {
            messages.add("Délai de carence non respecté (art. L.1244-3) : motif autonome de requalification.");
        }
        if ("AUTRE".equals(request.motifCddInvoque()) || "EMPLOI_USAGE".equals(request.motifCddInvoque())) {
            messages.add("Motif " + request.motifCddInvoque()
                    + " : motif fréquemment contesté en jurisprudence — exiger la preuve du caractère temporaire.");
        }
        messages.add("L'indemnité de précarité (art. L.1243-8) reste due en sus de l'indemnité de requalification "
                + "(Cass. soc. 30 mars 2005, n° 03-42.667). Outil dédié : F-DT-17.");
        messages.add("L'indemnité de requalification (art. L.1245-2) est cumulable avec les indemnités de rupture "
                + "si la requalification s'accompagne d'un licenciement sans cause réelle et sérieuse.");

        return new RequalificationCddCdiResult(
                request.motifCddInvoque(),
                Boolean.TRUE.equals(request.motifInterdit()),
                request.motifInterditType(),
                List.copyOf(request.successionCdd()),
                Boolean.TRUE.equals(request.delaiCarenceRespecte()),
                request.dureeContratMois(),
                salaire,
                request.dateFinDernierContrat(),
                score,
                verdict,
                indemniteRequalif,
                indemnitePrecarite,
                total,
                BASE_JURIDIQUE,
                formule,
                List.copyOf(messages)
        );
    }

    private static String formatMontant(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
