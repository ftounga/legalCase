package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-218-41 : analyseur de la <b>conformité aux obligations d'épargne salariale</b>
 * (intéressement, participation, dispositif de partage de la valeur — art.
 * L.3311-1 et s., L.3321-1 et s., L.3322-2 CT + loi n° 2023-1107 du 29/11/2023,
 * F-DT-53). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Logique métier (invariant CLAUDE.md — un outil = une situation ; distinct de
 * F-DT-52 PPV qui porte sur l'exonération d'une prime versée) :
 * <ul>
 *   <li><b>Participation obligatoire (≥ 50 salariés)</b> — si {@code effectif >= 50},
 *       la mise en place d'un accord de participation est obligatoire (art.
 *       L.3322-2 CT). Item {@code OBLIGATION}, conforme si
 *       {@code accordParticipationPresent}.</li>
 *   <li><b>Dispositif de partage de la valeur (11–49 salariés rentables)</b> — si
 *       {@code entreprise11a49} ET {@code beneficeNetFiscalPositif3Ans} (bénéfice
 *       net fiscal au moins égal à 1 % du CA sur 3 exercices consécutifs),
 *       obligation de mettre en place un dispositif de partage de la valeur
 *       (participation, intéressement, PPV, abondement) à compter des exercices
 *       ouverts en 2025 (loi n° 2023-1107). Item {@code OBLIGATION}, conforme si
 *       au moins un dispositif existe (participation OU intéressement).</li>
 *   <li><b>Verdict</b> — {@code NON_REQUIS} si aucune obligation applicable ;
 *       {@code CONFORME} si toutes les obligations applicables remplies ;
 *       {@code OBLIGATION_NON_REMPLIE} si au moins une obligation applicable non
 *       remplie.</li>
 * </ul>
 *
 * <p>Base juridique annotée « à vérifier par avocat ».
 */
public final class EpargneSalarialeConformiteAnalyzer {

    /** Seuil d'effectif rendant la participation obligatoire (art. L.3322-2 CT). */
    static final int SEUIL_PARTICIPATION_OBLIGATOIRE = 50;

    static final String BASE_JURIDIQUE =
            "art. L.3311-1 et suivants CT (intéressement) ; art. L.3321-1 et "
                    + "suivants CT (participation) ; art. L.3322-2 CT — participation "
                    + "obligatoire dans les entreprises employant habituellement au "
                    + "moins 50 salariés ; loi n° 2023-1107 du 29/11/2023 sur le "
                    + "partage de la valeur — obligation, pour les exercices ouverts à "
                    + "compter de 2025, de mettre en place un dispositif de partage de "
                    + "la valeur dans les entreprises de 11 à 49 salariés dont le "
                    + "bénéfice net fiscal est au moins égal à 1 % du chiffre "
                    + "d'affaires pendant 3 exercices consécutifs (à vérifier par "
                    + "avocat)";

    private EpargneSalarialeConformiteAnalyzer() {
    }

    /**
     * Analyse la conformité aux obligations d'épargne salariale et rend un
     * verdict {@code CONFORME} / {@code OBLIGATION_NON_REMPLIE} / {@code NON_REQUIS}.
     */
    public static EpargneSalarialeConformiteResult analyze(
            Integer effectif,
            Boolean accordParticipationPresent,
            Boolean accordInteressementPresent,
            Boolean beneficeNetFiscalPositif3Ans,
            Boolean entreprise11a49) {

        validate(effectif, accordParticipationPresent, accordInteressementPresent,
                beneficeNetFiscalPositif3Ans, entreprise11a49);

        int eff = effectif;
        boolean participation = accordParticipationPresent;
        boolean interessement = accordInteressementPresent;
        boolean beneficeNet3Ans = beneficeNetFiscalPositif3Ans;
        boolean pme11a49 = entreprise11a49;

        List<EpargneSalarialeConformiteItem> checklist = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        int obligationsApplicables = 0;
        int obligationsNonRemplies = 0;

        // ── Obligation participation (≥ 50 salariés) ────────────────────────
        if (eff >= SEUIL_PARTICIPATION_OBLIGATOIRE) {
            obligationsApplicables++;
            boolean conforme = participation;
            if (!conforme) {
                obligationsNonRemplies++;
            }
            checklist.add(new EpargneSalarialeConformiteItem(
                    "Accord de participation obligatoire (effectif d'au moins 50 salariés)",
                    conforme,
                    "OBLIGATION",
                    "Art. L.3322-2 CT : la participation est obligatoire dans les "
                            + "entreprises employant habituellement au moins 50 "
                            + "salariés. " + (conforme
                            ? "Un accord de participation est en place."
                            : "Aucun accord de participation détecté — obligation "
                                    + "non remplie (à vérifier par avocat).")));
            notes.add(conforme
                    ? "Participation obligatoire satisfaite : un accord de "
                            + "participation est en place (effectif ≥ 50)."
                    : "Participation obligatoire NON satisfaite : l'entreprise "
                            + "emploie au moins 50 salariés mais aucun accord de "
                            + "participation n'est en place (art. L.3322-2 CT).");
        }

        // ── Obligation partage de la valeur (11–49 salariés rentables) ──────
        if (pme11a49 && beneficeNet3Ans) {
            obligationsApplicables++;
            boolean dispositifPresent = participation || interessement;
            if (!dispositifPresent) {
                obligationsNonRemplies++;
            }
            checklist.add(new EpargneSalarialeConformiteItem(
                    "Dispositif de partage de la valeur (entreprise de 11 à 49 "
                            + "salariés rentable)",
                    dispositifPresent,
                    "OBLIGATION",
                    "Loi n° 2023-1107 du 29/11/2023 : pour les exercices ouverts à "
                            + "compter de 2025, les entreprises de 11 à 49 salariés "
                            + "dont le bénéfice net fiscal est au moins égal à 1 % du "
                            + "CA pendant 3 exercices consécutifs doivent mettre en "
                            + "place un dispositif de partage de la valeur "
                            + "(participation, intéressement, PPV ou abondement). "
                            + (dispositifPresent
                            ? "Au moins un dispositif est en place."
                            : "Aucun dispositif détecté — obligation non remplie (à "
                                    + "vérifier par avocat).")));
            notes.add(dispositifPresent
                    ? "Obligation de partage de la valeur satisfaite : au moins un "
                            + "dispositif (participation ou intéressement) est en "
                            + "place."
                    : "Obligation de partage de la valeur NON satisfaite : "
                            + "entreprise rentable de 11 à 49 salariés sans aucun "
                            + "dispositif de partage de la valeur (loi n° 2023-1107).");
        }

        // ── Aucune obligation applicable : information ──────────────────────
        if (obligationsApplicables == 0) {
            checklist.add(new EpargneSalarialeConformiteItem(
                    "Aucune obligation d'épargne salariale applicable",
                    true,
                    "INFORMATION",
                    "L'entreprise n'atteint pas le seuil de participation "
                            + "obligatoire (50 salariés) et n'entre pas dans le champ "
                            + "de l'obligation de partage de la valeur (entreprise de "
                            + "11 à 49 salariés rentable). La mise en place d'un "
                            + "dispositif reste facultative (à vérifier par avocat)."));
            notes.add("Aucune obligation légale d'épargne salariale applicable en "
                    + "l'état (effectif inférieur à 11 salariés, ou entreprise de 11 "
                    + "à 49 salariés non rentable).");
        }

        // ── Item d'information sur l'intéressement (toujours facultatif) ────
        checklist.add(new EpargneSalarialeConformiteItem(
                "Accord d'intéressement (dispositif facultatif)",
                true,
                "INFORMATION",
                "Art. L.3311-1 et s. CT : l'intéressement est un dispositif "
                        + "facultatif d'association des salariés aux résultats. "
                        + (interessement
                        ? "Un accord d'intéressement est en place."
                        : "Aucun accord d'intéressement détecté.")));

        // ── Verdict ─────────────────────────────────────────────────────────
        EpargneSalarialeConformiteStatut statut;
        if (obligationsApplicables == 0) {
            statut = EpargneSalarialeConformiteStatut.NON_REQUIS;
        } else if (obligationsNonRemplies > 0) {
            statut = EpargneSalarialeConformiteStatut.OBLIGATION_NON_REMPLIE;
        } else {
            statut = EpargneSalarialeConformiteStatut.CONFORME;
        }

        return new EpargneSalarialeConformiteResult(
                eff,
                participation,
                interessement,
                beneficeNet3Ans,
                pme11a49,
                List.copyOf(checklist),
                obligationsApplicables,
                obligationsNonRemplies,
                statut,
                List.copyOf(notes),
                BASE_JURIDIQUE);
    }

    private static void validate(Integer effectif,
                                 Boolean accordParticipationPresent,
                                 Boolean accordInteressementPresent,
                                 Boolean beneficeNetFiscalPositif3Ans,
                                 Boolean entreprise11a49) {
        if (effectif == null) {
            throw new IllegalArgumentException("effectif est requis");
        }
        if (effectif <= 0) {
            throw new IllegalArgumentException("effectif doit être strictement positif");
        }
        if (accordParticipationPresent == null) {
            throw new IllegalArgumentException("accordParticipationPresent est requis");
        }
        if (accordInteressementPresent == null) {
            throw new IllegalArgumentException("accordInteressementPresent est requis");
        }
        if (beneficeNetFiscalPositif3Ans == null) {
            throw new IllegalArgumentException("beneficeNetFiscalPositif3Ans est requis");
        }
        if (entreprise11a49 == null) {
            throw new IllegalArgumentException("entreprise11a49 est requis");
        }
    }
}
