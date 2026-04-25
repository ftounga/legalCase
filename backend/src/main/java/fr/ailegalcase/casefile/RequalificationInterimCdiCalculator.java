package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SF-DT-23-01 : analyseur de requalification intérim → CDI
 * (art. L.1251-40, L.1251-41, L.1251-32, L.1251-36 Code du travail).
 *
 * Score additif (0-100, plafonné) sur 4 critères :
 * - motif structurellement interdit (art. L.1251-6, L.1251-9, L.1251-10) → +50
 * - succession ≥ 3 missions chez la même entreprise utilisatrice (Cass. soc. veille) → +25
 * - délai de carence non respecté (art. L.1251-36) → +20
 * - motif invoqué AUTRE → +10
 *
 * Verdict : ELEVEE ≥ 60, MOYENNE 30-59, FAIBLE < 30.
 *
 * Indemnité de requalification : plancher 1 mois de salaire (art. L.1251-41 al. 2).
 * Rappel fin de mission (art. L.1251-32) : 10 % × salaires totaux.
 *
 * Pattern jumeau direct de RequalificationCddCdiCalculator (F-DT-22), adapté à
 * la relation triangulaire propre à l'intérim.
 */
public final class RequalificationInterimCdiCalculator {

    static final Set<String> MOTIFS_INTERIM_INVOQUES = Set.of(
            "ACCROISSEMENT_TEMPORAIRE",
            "REMPLACEMENT_SALARIE",
            "EMPLOI_SAISONNIER",
            "EMPLOI_USAGE",
            "MISSION_PEPINIERE",
            "AUTRE"
    );

    static final Set<String> MOTIFS_INTERDITS_TYPES = Set.of(
            "EMPLOI_PERMANENT",
            "REMPLACEMENT_GREVISTE",
            "TRAVAUX_DANGEREUX",
            "AUTRE"
    );

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal FIN_MISSION_RATE = new BigDecimal("10");
    private static final String BASE_JURIDIQUE =
            "Art. L.1251-40+L.1251-41 (requalif) + L.1251-32 (fin mission)";

    private RequalificationInterimCdiCalculator() {}

    public static RequalificationInterimCdiResult compute(RequalificationInterimCdiRequest request) {
        // -- Validations ---------------------------------------------------
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.motifInterimInvoque() == null || request.motifInterimInvoque().isBlank()) {
            throw new IllegalArgumentException("Motif intérim invoqué requis");
        }
        if (!MOTIFS_INTERIM_INVOQUES.contains(request.motifInterimInvoque())) {
            throw new IllegalArgumentException("Motif intérim invoqué inconnu : "
                    + request.motifInterimInvoque() + ". Valeurs acceptées : " + MOTIFS_INTERIM_INVOQUES);
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
        if (request.successionMissions() == null || request.successionMissions().isEmpty()) {
            throw new IllegalArgumentException("Au moins une mission requise dans la succession");
        }
        for (int i = 0; i < request.successionMissions().size(); i++) {
            RequalificationInterimCdiRequest.MissionInterim m = request.successionMissions().get(i);
            if (m == null || m.dateDebut() == null || m.dateFin() == null) {
                throw new IllegalArgumentException("Dates requises pour la mission #" + (i + 1));
            }
            if (m.dateDebut().isAfter(m.dateFin())) {
                throw new IllegalArgumentException("Dates incohérentes pour la mission #" + (i + 1)
                        + " : dateDebut=" + m.dateDebut() + " > dateFin=" + m.dateFin());
            }
        }
        if (request.delaiCarenceRespecte() == null) {
            throw new IllegalArgumentException("delaiCarenceRespecte requis (true/false)");
        }
        if (request.dureeMissionsTotaleMois() == null || request.dureeMissionsTotaleMois() <= 0) {
            throw new IllegalArgumentException("Durée missions (mois) requise et > 0");
        }
        if (request.salaireMensuelBrutEur() == null || request.salaireMensuelBrutEur().signum() <= 0) {
            throw new IllegalArgumentException("Salaire mensuel brut requis et > 0");
        }
        if (request.dateFinDerniereMission() == null) {
            throw new IllegalArgumentException("Date de fin de la dernière mission requise");
        }
        if (request.memeEntrepriseUtilisatrice() == null) {
            throw new IllegalArgumentException("memeEntrepriseUtilisatrice requis (true/false)");
        }

        // -- Scoring -------------------------------------------------------
        int score = 0;
        if (Boolean.TRUE.equals(request.motifInterdit())) {
            score += 50;
        }
        if (request.successionMissions().size() >= 3
                && Boolean.TRUE.equals(request.memeEntrepriseUtilisatrice())) {
            score += 25;
        }
        if (Boolean.FALSE.equals(request.delaiCarenceRespecte())) {
            score += 20;
        }
        if ("AUTRE".equals(request.motifInterimInvoque())) {
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
        // Plancher art. L.1251-41 al. 2 : 1 mois de salaire
        BigDecimal indemniteRequalif = salaire.setScale(2, RoundingMode.HALF_UP);

        // Rappel fin de mission art. L.1251-32 : 10 % × total rémunérations
        BigDecimal totalRemunerations = salaire.multiply(BigDecimal.valueOf(request.dureeMissionsTotaleMois()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal indemniteFinMission = totalRemunerations.multiply(FIN_MISSION_RATE)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

        BigDecimal total = indemniteRequalif.add(indemniteFinMission).setScale(2, RoundingMode.HALF_UP);

        // -- Formule -------------------------------------------------------
        String formule = String.format(
                "Indemnité requalif = 1 × %s € (plancher art. L.1251-41 al. 2). "
                        + "Fin mission 10 %% × %s € = %s €.",
                formatMontant(salaire),
                formatMontant(totalRemunerations),
                formatMontant(indemniteFinMission));

        // -- Messages ------------------------------------------------------
        List<String> messages = new ArrayList<>();
        // Prescription
        LocalDate prescriptionLimit = request.dateFinDerniereMission().plusYears(1);
        messages.add("Action en requalification prescrite par 12 mois suivant le terme de la dernière mission "
                + "(art. L.1471-1 + Cass. soc. 29 janv. 2020) — date limite : " + prescriptionLimit + ".");

        messages.add("Requalification possible contre l'entreprise utilisatrice (art. L.1251-40 + Cass. soc. "
                + "7 mars 2018, n° 16-26.616) ou contre l'agence d'intérim (art. L.1251-41) — choix tactique.");

        if (Boolean.TRUE.equals(request.motifInterdit())) {
            messages.add("Motif interdit identifié (" + request.motifInterditType()
                    + ") : forte présomption de requalification (art. L.1251-6 / L.1251-9 / L.1251-10).");
        }
        if (request.successionMissions().size() >= 3
                && Boolean.TRUE.equals(request.memeEntrepriseUtilisatrice())) {
            messages.add("Succession de " + request.successionMissions().size()
                    + " missions chez la même entreprise utilisatrice : risque accru de requalification "
                    + "(art. L.1251-12 + jurisprudence constante).");
        }
        if (Boolean.FALSE.equals(request.delaiCarenceRespecte())) {
            messages.add("Délai de carence non respecté (art. L.1251-36) : motif autonome de requalification.");
        }
        if ("AUTRE".equals(request.motifInterimInvoque())) {
            messages.add("Motif AUTRE : motif fréquemment contesté en jurisprudence — exiger la preuve "
                    + "du caractère temporaire et du recours autorisé.");
        }
        messages.add("L'indemnité de fin de mission (art. L.1251-32) reste due en sus de l'indemnité de "
                + "requalification (Cass. soc. 13 juin 2007, n° 06-43.073). Outil dédié : F-DT-18.");
        messages.add("L'indemnité de requalification (art. L.1251-41 al. 2) est cumulable avec les indemnités "
                + "de rupture si la requalification s'accompagne d'un licenciement sans cause réelle et sérieuse.");

        return new RequalificationInterimCdiResult(
                request.motifInterimInvoque(),
                Boolean.TRUE.equals(request.motifInterdit()),
                request.motifInterditType(),
                List.copyOf(request.successionMissions()),
                Boolean.TRUE.equals(request.delaiCarenceRespecte()),
                request.dureeMissionsTotaleMois(),
                salaire,
                request.dateFinDerniereMission(),
                Boolean.TRUE.equals(request.memeEntrepriseUtilisatrice()),
                score,
                verdict,
                indemniteRequalif,
                indemniteFinMission,
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
