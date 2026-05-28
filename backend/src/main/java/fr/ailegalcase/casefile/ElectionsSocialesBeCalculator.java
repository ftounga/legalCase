package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.Month;

/**
 * SF-219-09 : calculateur des élections sociales BE — Loi du
 * 04/12/2007 (relative aux élections sociales) + AR du 25/05/2012
 * (modalités d'organisation) + Loi du 04/08/1996 art. 49 (CPPT) +
 * Loi du 19/03/1991 (protection des candidats / délégués) — fonction
 * <b>pure</b> (sans dépendance HTTP / persistance / horloge).
 *
 * <p>L'outil détermine :</p>
 * <ol>
 *   <li>L'obligation d'organiser des élections (CE, CPPT, ou aucune)
 *       selon l'effectif moyen ETP (seuils 50 / 100 Loi 04/12/2007
 *       art. 9 § 1er + Loi 04/08/1996 art. 49).</li>
 *   <li>Le calendrier complet à rebours du jour Y (date du scrutin
 *       imposée par AR dans une fenêtre nationale de 14 jours).</li>
 *   <li>La fenêtre de protection des candidats contre le licenciement
 *       (Loi 19/03/1991 art. 2 §1 et 3).</li>
 * </ol>
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Loi du 04/12/2007</b> relative aux élections sociales —
 *       organisation des scrutins CE / CPPT (4 ans).</li>
 *   <li><b>AR du 25/05/2012</b> portant exécution de la Loi du
 *       04/12/2007 — modalités d'organisation (calendrier jour X à
 *       jour Y, listes électeurs, candidatures, recours).</li>
 *   <li><b>Loi du 04/08/1996</b> art. 49 (bien-être au travail) —
 *       obligation d'instituer un CPPT à partir de 50 ETP.</li>
 *   <li><b>Loi du 19/03/1991</b> portant un régime de licenciement
 *       particulier pour les délégués du personnel + candidats —
 *       protection occulte (30 j avant affichage candidats) jusqu'à
 *       installation suivante (élus) / 2 ans (non-élus 1re fois).</li>
 *   <li><b>Directive 94/45/CE</b> (comité européen d'entreprise —
 *       hors scope V1).</li>
 * </ul>
 *
 * <h2>Hiérarchie des verdicts</h2>
 * <ol>
 *   <li>{@code EFFECTIF_A_RECALCULER} — effectif aux limites
 *       (49-51 ou 99-101 ETP) : recalcul ETP sur 4 trimestres
 *       indispensable, sanction pénale art. 32 si manquement.</li>
 *   <li>{@code OBLIGATION_CE_ET_CPPT} — ≥ 100 ETP : CE + CPPT.</li>
 *   <li>{@code OBLIGATION_CPPT_SEUL} — 50 à 99 ETP : CPPT seul.</li>
 *   <li>{@code NON_APPLICABLE_SEUIL} — &lt; 50 ETP : pas d'obligation.</li>
 * </ol>
 */
public final class ElectionsSocialesBeCalculator {

    static final String BASE_JURIDIQUE =
            "Loi du 04/12/2007 relative aux élections sociales (art. 9 §"
                    + " 1er : seuils CE ≥ 100 ETP, CPPT ≥ 50 ETP) ; AR"
                    + " du 25/05/2012 portant exécution (calendrier jour X"
                    + " = Y-90, jour X-60 = procédure UTE, jour X-35 ="
                    + " transmission listes électorales, jour X+35 ="
                    + " dépôt listes candidats, jour X+40 = affichage"
                    + " candidats, jour Y = scrutin, jour Y+6 ="
                    + " proclamation, jour Y+45 = 1re réunion) ; Loi du"
                    + " 04/08/1996 art. 49 (CPPT à partir de 50 ETP) ;"
                    + " Loi du 19/03/1991 (protection des candidats /"
                    + " délégués — occulte rétroactive 30 j avant"
                    + " affichage, fin = installation suivante / 2 ans"
                    + " pour non-élus 1re fois). Sanction art. 32 Loi"
                    + " 04/12/2007 : amende pénale + délit d'entrave.";

    static final String AVERT_GENERAL =
            "Cet outil est un calculateur de chronologie + checklist"
                    + " d'obligation. L'effectif moyen ETP doit être"
                    + " recalculé précisément sur les 4 trimestres"
                    + " précédant le 1er trimestre de l'année des"
                    + " élections (Loi 04/12/2007 art. 7) — un seuil"
                    + " franchi entre temps déclenche l'obligation. La"
                    + " procédure UTE (X-60) est obligatoire dès qu'une"
                    + " unité technique d'exploitation n'a pas été"
                    + " préalablement confirmée par les organes paritaires"
                    + " ou par décision juridictionnelle. Hors scope :"
                    + " contentieux UTE, recours individuels listes"
                    + " électorales, contentieux protection licenciement"
                    + " (calculer l'indemnité avec l'outil licenciement-"
                    + "be-protection-deleguee).";

    /** Seuil CE — Loi 04/12/2007 art. 9 § 1er. */
    static final int SEUIL_CE_ETP = 100;

    /** Seuil CPPT — Loi 04/08/1996 art. 49. */
    static final int SEUIL_CPPT_ETP = 50;

    /** Marge effectif "limite" (recalcul ETP requis) autour des seuils. */
    static final int MARGE_EFFECTIF_LIMITE = 1;

    /** Jour X = jour Y - 90 (AR 25/05/2012 art. 9). */
    static final int OFFSET_JOUR_X = -90;

    /** Jour X - 60 = début procédure UTE (AR 25/05/2012 art. 11). */
    static final int OFFSET_JOUR_X_MOINS_60 = -60;

    /** Jour X - 35 = transmission listes électorales (AR 25/05/2012 art. 14). */
    static final int OFFSET_JOUR_X_MOINS_35 = -35;

    /** Jour X + 35 = dépôt listes de candidats (AR 25/05/2012 art. 33). */
    static final int OFFSET_JOUR_X_PLUS_35 = 35;

    /** Jour X + 40 = affichage candidats (AR 25/05/2012 art. 37). */
    static final int OFFSET_JOUR_X_PLUS_40 = 40;

    /** Jour Y + 6 = proclamation des résultats. */
    static final int OFFSET_PROCLAMATION_RESULTATS = 6;

    /** Jour Y + 45 = 1re réunion CE / CPPT installé. */
    static final int OFFSET_PREMIERE_REUNION = 45;

    /** Période protection candidats : 30 j avant affichage = X + 10. */
    static final int OFFSET_DEBUT_PERIODE_PROTEGEE = 10;

    /** Durée protection des candidats non-élus présentés une 1re fois : 2 ans. */
    static final int DUREE_PROTECTION_NON_ELU_ANS = 2;

    private ElectionsSocialesBeCalculator() {
    }

    public static ElectionsSocialesBeResult analyze(
            ElectionsSocialesBeRequest request) {
        validateInputs(request);

        // 1. Validation du jour Y dans la fenêtre AR du cycle déclaré.
        validateJourYDansFenetreCycle(request.cycleElectoral(), request.dateJourY());

        // 2. Détermination du verdict (obligation).
        ElectionsSocialesBeVerdict verdict = determinerVerdict(request.effectifMoyenEtp());

        // 3. Calcul du calendrier — toujours, indépendamment du verdict
        //    (utile pour le conseil prévisionnel ; UI affichera en grisé
        //    si NON_APPLICABLE_SEUIL).
        LocalDate y = request.dateJourY();
        LocalDate jourX = y.plusDays(OFFSET_JOUR_X);
        LocalDate procUte = jourX.plusDays(OFFSET_JOUR_X_MOINS_60);
        LocalDate transmListes = jourX.plusDays(OFFSET_JOUR_X_MOINS_35);
        LocalDate depotCandidats = jourX.plusDays(OFFSET_JOUR_X_PLUS_35);
        LocalDate affichageCandidats = jourX.plusDays(OFFSET_JOUR_X_PLUS_40);
        LocalDate proclamation = y.plusDays(OFFSET_PROCLAMATION_RESULTATS);
        LocalDate premiereReunion = y.plusDays(OFFSET_PREMIERE_REUNION);

        // 4. Fenêtre de protection : remplie seulement si obligation existe.
        LocalDate debutProtection = null;
        LocalDate finProtectionNonElus = null;
        if (verdict == ElectionsSocialesBeVerdict.OBLIGATION_CE_ET_CPPT
                || verdict == ElectionsSocialesBeVerdict.OBLIGATION_CPPT_SEUL) {
            debutProtection = jourX.plusDays(OFFSET_DEBUT_PERIODE_PROTEGEE);
            finProtectionNonElus = affichageCandidats
                    .plusYears(DUREE_PROTECTION_NON_ELU_ANS);
        }

        boolean ceObligatoire =
                verdict == ElectionsSocialesBeVerdict.OBLIGATION_CE_ET_CPPT;
        boolean cpptObligatoire =
                verdict == ElectionsSocialesBeVerdict.OBLIGATION_CE_ET_CPPT
                        || verdict == ElectionsSocialesBeVerdict.OBLIGATION_CPPT_SEUL;

        return new ElectionsSocialesBeResult(
                request.cycleElectoral(),
                request.dateJourY(),
                request.effectifMoyenEtp(),
                request.uniteTechniqueExploitationConfirmee(),
                verdict,
                ceObligatoire,
                cpptObligatoire,
                SEUIL_CE_ETP,
                SEUIL_CPPT_ETP,
                jourX,
                procUte,
                transmListes,
                depotCandidats,
                affichageCandidats,
                proclamation,
                premiereReunion,
                debutProtection,
                finProtectionNonElus,
                raisonPour(verdict),
                synthesePour(verdict, request, jourX, affichageCandidats,
                        proclamation, debutProtection),
                BASE_JURIDIQUE,
                AVERT_GENERAL);
    }

    // ── Détermination du verdict ─────────────────────────────────────────

    static ElectionsSocialesBeVerdict determinerVerdict(int effectifMoyen) {
        // Limites avec marge ±1 ETP : recalcul ETP impératif.
        if (Math.abs(effectifMoyen - SEUIL_CPPT_ETP) <= MARGE_EFFECTIF_LIMITE
                || Math.abs(effectifMoyen - SEUIL_CE_ETP) <= MARGE_EFFECTIF_LIMITE) {
            return ElectionsSocialesBeVerdict.EFFECTIF_A_RECALCULER;
        }
        if (effectifMoyen >= SEUIL_CE_ETP) {
            return ElectionsSocialesBeVerdict.OBLIGATION_CE_ET_CPPT;
        }
        if (effectifMoyen >= SEUIL_CPPT_ETP) {
            return ElectionsSocialesBeVerdict.OBLIGATION_CPPT_SEUL;
        }
        return ElectionsSocialesBeVerdict.NON_APPLICABLE_SEUIL;
    }

    // ── Raison ──────────────────────────────────────────────────────────

    private static String raisonPour(ElectionsSocialesBeVerdict verdict) {
        return switch (verdict) {
            case OBLIGATION_CE_ET_CPPT -> "OBLIGATION_CE_CPPT_SEUIL_100";
            case OBLIGATION_CPPT_SEUL -> "OBLIGATION_CPPT_SEUIL_50";
            case NON_APPLICABLE_SEUIL -> "EFFECTIF_INFERIEUR_50";
            case EFFECTIF_A_RECALCULER -> "EFFECTIF_LIMITE_RECALCUL_REQUIS";
        };
    }

    // ── Synthèse ────────────────────────────────────────────────────────

    private static String synthesePour(
            ElectionsSocialesBeVerdict verdict,
            ElectionsSocialesBeRequest request,
            LocalDate jourX,
            LocalDate affichageCandidats,
            LocalDate proclamation,
            LocalDate debutProtection) {
        String entete = "Cycle " + cycleLibelle(request.cycleElectoral())
                + ", jour Y = " + request.dateJourY()
                + " (effectif " + request.effectifMoyenEtp() + " ETP). ";
        return switch (verdict) {
            case OBLIGATION_CE_ET_CPPT -> entete
                    + "Obligation d'organiser le scrutin CE + CPPT. Jour X"
                    + " = " + jourX + " (affichage avis), affichage"
                    + " candidats = " + affichageCandidats + ", proclamation"
                    + " = " + proclamation + ". Période protégée"
                    + " candidats à partir du " + debutProtection
                    + " (occulte rétroactive Loi 19/03/1991). Tout"
                    + " licenciement d'un candidat pendant la fenêtre est"
                    + " soumis à motif grave préalablement reconnu par la"
                    + " juridiction du travail ou raisons d'ordre"
                    + " économique / technique préalablement reconnues par"
                    + " la commission paritaire compétente."
                    + (Boolean.FALSE.equals(request.uniteTechniqueExploitationConfirmee())
                            ? " ⚠️ UTE non confirmée : initier la procédure"
                            + " unité technique d'exploitation au jour X-60."
                            : "");
            case OBLIGATION_CPPT_SEUL -> entete
                    + "Obligation d'organiser le scrutin CPPT seul (effectif"
                    + " entre 50 et 99 ETP — pas de CE). Jour X = " + jourX
                    + ", affichage candidats = " + affichageCandidats
                    + ", proclamation = " + proclamation + ". Période"
                    + " protégée candidats à partir du " + debutProtection
                    + " (Loi 19/03/1991). À noter : si l'effectif franchit"
                    + " 100 ETP à la date de référence du prochain cycle,"
                    + " un CE devra être institué.";
            case NON_APPLICABLE_SEUIL -> entete
                    + "Aucune obligation d'élections sociales : l'effectif"
                    + " moyen est inférieur au seuil CPPT (50 ETP). La"
                    + " délégation syndicale CCT n° 5 reste possible si"
                    + " ≥ 50 % du personnel le demande (hors scope outil).";
            case EFFECTIF_A_RECALCULER -> entete
                    + "Effectif aux limites (49-51 ou 99-101 ETP). Un"
                    + " recalcul ETP précis sur les 4 trimestres précédant"
                    + " le 1er trimestre de l'année des élections (Loi"
                    + " 04/12/2007 art. 7) est INDISPENSABLE avant tout"
                    + " déclenchement. Risque pénal art. 32 (amende + délit"
                    + " d'entrave) en cas de manquement à l'obligation"
                    + " procédurale.";
        };
    }

    private static String cycleLibelle(ElectionsSocialesBeCycle cycle) {
        return switch (cycle) {
            case CYCLE_2024 -> "2024";
            case CYCLE_2028 -> "2028";
            case CYCLE_2032 -> "2032";
        };
    }

    // ── Validation jour Y dans fenêtre cycle ────────────────────────────

    static void validateJourYDansFenetreCycle(
            ElectionsSocialesBeCycle cycle, LocalDate dateJourY) {
        LocalDate debutFenetre;
        LocalDate finFenetre;
        switch (cycle) {
            case CYCLE_2024 -> {
                debutFenetre = LocalDate.of(2024, Month.MAY, 13);
                finFenetre = LocalDate.of(2024, Month.MAY, 26);
            }
            case CYCLE_2028 -> {
                debutFenetre = LocalDate.of(2028, Month.MAY, 11);
                finFenetre = LocalDate.of(2028, Month.MAY, 24);
            }
            case CYCLE_2032 -> {
                debutFenetre = LocalDate.of(2032, Month.MAY, 1);
                finFenetre = LocalDate.of(2032, Month.MAY, 31);
            }
            default -> throw new IllegalArgumentException(
                    "Cycle inconnu : " + cycle);
        }
        if (dateJourY.isBefore(debutFenetre) || dateJourY.isAfter(finFenetre)) {
            throw new IllegalArgumentException(
                    "dateJourY (" + dateJourY + ") hors fenêtre AR du cycle "
                            + cycleLibelle(cycle) + " (" + debutFenetre
                            + " → " + finFenetre + ")");
        }
    }

    // ── Validation conditionnelle ─────────────────────────────────────────

    private static void validateInputs(ElectionsSocialesBeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.cycleElectoral() == null) {
            throw new IllegalArgumentException("cycleElectoral est requis");
        }
        if (request.dateJourY() == null) {
            throw new IllegalArgumentException("dateJourY est requise");
        }
        if (request.effectifMoyenEtp() == null
                || request.effectifMoyenEtp() < 0) {
            throw new IllegalArgumentException(
                    "effectifMoyenEtp requis et ≥ 0");
        }
        if (request.uniteTechniqueExploitationConfirmee() == null) {
            throw new IllegalArgumentException(
                    "uniteTechniqueExploitationConfirmee est requis");
        }
    }
}
