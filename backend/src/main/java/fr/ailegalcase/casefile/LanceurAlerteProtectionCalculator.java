package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-212-25 : moteur d'analyse de la protection du lanceur d'alerte
 * (FRANCE) — art. L. 1132-3-3 CT ; loi Sapin II n° 2016-1691 du
 * 09/12/2016 ; loi Waserman n° 2022-401 du 21/03/2022 (transposition
 * directive UE 2019/1937).
 *
 * <p>Évalue :
 * <ul>
 *   <li>La <b>qualité de lanceur d'alerte</b> (loi Waserman 2022) :
 *       personne physique signalant <i>sans contrepartie financière
 *       directe</i> des informations portant sur un crime, un délit,
 *       une violation du droit de l'UE ou une menace pour l'intérêt
 *       général.</li>
 *   <li>La <b>conformité de la procédure</b> de signalement (interne /
 *       externe / divulgation publique subsidiaire).</li>
 *   <li>Les <b>mesures de représailles</b> interdites par L. 1132-3-3 CT
 *       — nullité de plein droit de la mesure ; possibilité de
 *       réintégration.</li>
 *   <li>Le <b>montant minimum de dommages et intérêts</b> : 10 000 €
 *       depuis la loi Waserman 2022 (sanction civile pour procédure
 *       abusive contre le lanceur d'alerte).</li>
 * </ul>
 *
 * <p><b>Pays</b> : FRANCE uniquement — la loi Waserman et son régime
 * spécifique d'indemnisation minimum sont français. La BE dispose
 * d'un régime distinct (loi du 28/11/2022 transposant la même directive
 * UE mais avec des modalités propres).</p>
 */
public final class LanceurAlerteProtectionCalculator {

    /** Verdict d'évaluation de la protection. */
    public enum AnalyseLanceurAlerte {
        PROTECTION_FORTE,
        PROTECTION_PARTIELLE,
        HORS_CHAMP
    }

    /** Code structuré d'un point d'analyse F-IA-03. */
    public enum CodeCritere {
        DT61_NATURE_SIGNALEMENT,
        DT61_CONTREPARTIE,
        DT61_PROCEDURE,
        DT61_MESURE_REPRESAILLE
    }

    /** Point d'analyse F-IA-03 — exposé dans la réponse API. */
    public record PointAnalyse(
            CodeCritere code,
            String libelle,
            String fondement,
            String conclusion
    ) {}

    /** Montant minimum DI loi Waserman 2022 si statut protégé reconnu. */
    public static final double MONTANT_MIN_DOMMAGES_INTERETS = 10_000.0;

    /** Résultat calculé par {@link #compute}. */
    public record Result(
            AnalyseLanceurAlerte analyseLanceurAlerte,
            int scoreProtection,
            List<PointAnalyse> pointsAnalyse,
            boolean nulliteMesureRepresaille,
            double montantMinDommagesInterets,
            List<String> basesJuridiques,
            List<String> messages,
            String country
    ) {}

    private LanceurAlerteProtectionCalculator() {}

    /**
     * Point d'entrée principal.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace
     * @return résultat calculé
     * @throws IllegalArgumentException si {@code country != "FRANCE"} ou si
     *         {@code input} est null ou incomplet.
     */
    public static Result compute(LanceurAlerteProtectionInput input, String country) {
        if (!"FRANCE".equalsIgnoreCase(country)) {
            throw new IllegalArgumentException(
                    "Outil réservé au droit du travail FRANCE (loi Waserman 2022) — pays détecté : " + country);
        }
        if (input == null) {
            throw new IllegalArgumentException("Données d'entrée manquantes");
        }
        if (input.natureSignalement() == null) {
            throw new IllegalArgumentException("Nature du signalement requise");
        }
        if (input.procedureSignalement() == null) {
            throw new IllegalArgumentException("Procédure de signalement requise");
        }
        if (input.natureMesureRepresaille() == null) {
            throw new IllegalArgumentException("Nature de la mesure de représailles requise (AUCUNE si pas de mesure)");
        }

        List<PointAnalyse> points = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        // ── 1. Nature du signalement (loi Waserman 2022) ────────────────────
        switch (input.natureSignalement()) {
            case CRIME_DELIT -> {
                points.add(new PointAnalyse(
                        CodeCritere.DT61_NATURE_SIGNALEMENT,
                        "Signalement portant sur un crime ou délit",
                        "Loi Waserman 2022 (loi n° 2022-401 du 21/03/2022) ; L. 1132-3-3 CT",
                        "Le signalement entre dans le champ matériel de la loi Waserman 2022 — qualité de lanceur d'alerte caractérisée pour cette nature."));
                bases.add("Loi Waserman 2022 (loi n° 2022-401 du 21/03/2022) — signalement de crimes/délits");
            }
            case VIOLATION_DROIT_UE -> {
                points.add(new PointAnalyse(
                        CodeCritere.DT61_NATURE_SIGNALEMENT,
                        "Signalement portant sur une violation du droit de l'UE",
                        "Directive UE 2019/1937 ; loi Waserman 2022 (transposition)",
                        "Le signalement entre dans le champ matériel de la directive UE 2019/1937 transposée par la loi Waserman — qualité de lanceur d'alerte caractérisée."));
                bases.add("Directive UE 2019/1937 — protection des lanceurs d'alerte de violations du droit UE");
                bases.add("Loi Waserman 2022 — transposition directive 2019/1937");
            }
            case MENACE_INTERET_GENERAL -> {
                points.add(new PointAnalyse(
                        CodeCritere.DT61_NATURE_SIGNALEMENT,
                        "Signalement d'une menace pour l'intérêt général",
                        "Loi Sapin II n° 2016-1691 du 09/12/2016 ; loi Waserman 2022",
                        "Le signalement vise une menace ou un préjudice pour l'intérêt général — entre dans le champ matériel élargi par la loi Waserman 2022 (suppression du critère de gravité de la loi Sapin II)."));
                bases.add("Loi Sapin II n° 2016-1691 du 09/12/2016 — protection des lanceurs d'alerte");
                bases.add("Loi Waserman 2022 — champ matériel élargi");
            }
            case AUTRE -> {
                points.add(new PointAnalyse(
                        CodeCritere.DT61_NATURE_SIGNALEMENT,
                        "Nature du signalement non strictement caractérisée",
                        "Loi Waserman 2022 ; L. 1132-3-3 CT",
                        "La nature du signalement ne relève pas explicitement des catégories matérielles protégées par la loi Waserman 2022. La qualité de lanceur d'alerte n'est pas pleinement caractérisée — examen au cas par cas."));
                messages.add("ALERTE : nature du signalement \"AUTRE\" — vérifier que le contenu effectif ne relève pas en réalité d'un crime/délit, d'une violation du droit UE ou d'une menace pour l'intérêt général.");
            }
        }

        boolean natureProtegee = input.natureSignalement()
                != LanceurAlerteProtectionInput.NatureSignalement.AUTRE;

        // ── 2. Contrepartie financière (cause d'exclusion loi Waserman) ─────
        boolean contrepartie = Boolean.TRUE.equals(input.contrepartieFinanciere());
        if (contrepartie) {
            points.add(new PointAnalyse(
                    CodeCritere.DT61_CONTREPARTIE,
                    "Contrepartie financière directe perçue — exclusion",
                    "Loi Waserman 2022 — art. L. 1132-3-3 CT",
                    "La perception d'une contrepartie financière directe par le signaleur est une cause d'exclusion expresse du statut de lanceur d'alerte (loi Waserman 2022). Le régime de protection ne s'applique pas."));
            messages.add("EXCLUSION : contrepartie financière directe → hors champ du statut de lanceur d'alerte (loi Waserman 2022).");
            bases.add("Loi Waserman 2022 — exclusion pour contrepartie financière directe");
        } else {
            points.add(new PointAnalyse(
                    CodeCritere.DT61_CONTREPARTIE,
                    "Absence de contrepartie financière directe",
                    "Loi Waserman 2022 — art. L. 1132-3-3 CT",
                    "Aucune contrepartie financière directe perçue — la condition d'exclusion de la loi Waserman 2022 n'est pas réunie."));
        }

        // ── 3. Procédure de signalement ─────────────────────────────────────
        boolean procedureConforme = false;
        switch (input.procedureSignalement()) {
            case INTERNE -> {
                boolean referentSaisi = Boolean.TRUE.equals(input.referentInterneSaisi());
                if (referentSaisi) {
                    points.add(new PointAnalyse(
                            CodeCritere.DT61_PROCEDURE,
                            "Procédure interne suivie — référent saisi",
                            "L. 1132-3-3 CT ; loi Waserman 2022 — canaux internes",
                            "Le signalement a été transmis via le canal interne au référent désigné de l'entreprise — procédure conforme au régime Waserman 2022 (entreprises de 50 salariés et plus)."));
                    procedureConforme = true;
                    bases.add("L. 1132-3-3 CT — canal interne de signalement (référent obligatoire ≥ 50 salariés)");
                } else {
                    points.add(new PointAnalyse(
                            CodeCritere.DT61_PROCEDURE,
                            "Procédure interne déclarée — référent NON saisi",
                            "L. 1132-3-3 CT ; loi Waserman 2022",
                            "Le signalement est décrit comme interne mais le référent désigné n'a pas été effectivement saisi — protection partielle, le signalement peut être requalifié."));
                    messages.add("ALERTE : procédure interne déclarée mais référent non saisi — risque de requalification de la procédure.");
                }
            }
            case EXTERNE -> {
                points.add(new PointAnalyse(
                        CodeCritere.DT61_PROCEDURE,
                        "Procédure externe — autorité compétente saisie",
                        "Loi Waserman 2022 — canal externe (Défenseur des droits, AFA, ANSSI, etc.)",
                        "Le signalement a été transmis directement à une autorité externe compétente (Défenseur des droits, AFA, ANSSI, inspection du travail, etc.) — procédure conforme, la loi Waserman 2022 supprime la condition de saisine interne préalable."));
                procedureConforme = true;
                bases.add("Loi Waserman 2022 — canal externe direct (suppression de la subsidiarité Sapin II)");
            }
            case DIVULGATION_PUBLIQUE -> {
                points.add(new PointAnalyse(
                        CodeCritere.DT61_PROCEDURE,
                        "Divulgation publique — voie subsidiaire",
                        "Loi Waserman 2022 — divulgation publique (art. 8 directive UE 2019/1937)",
                        "La divulgation publique est ouverte à titre subsidiaire en cas de danger grave et imminent, de risque de représailles, ou d'inaction des autorités saisies — la protection dépend de la démonstration de ces conditions."));
                messages.add("ATTENTION : la divulgation publique ne bénéficie de la protection lanceur d'alerte que sous conditions strictes (danger grave imminent, risque de représailles, inaction des autorités).");
                bases.add("Directive UE 2019/1937 art. 8 — divulgation publique subsidiaire");
            }
        }

        // ── 4. Mesure de représailles (L. 1132-3-3 CT) ──────────────────────
        boolean representailles = Boolean.TRUE.equals(input.mesureRepresailleDetectee());
        boolean nulliteMesure = false;
        if (representailles) {
            String libelleMesure = "";
            String conclusion = "";
            switch (input.natureMesureRepresaille()) {
                case LICENCIEMENT -> {
                    libelleMesure = "Licenciement de représailles détecté";
                    conclusion = "Le licenciement intervenu en représailles du signalement est NUL de plein droit (L. 1132-3-3 CT). Réintégration possible, à défaut indemnisation intégrale du préjudice + sanction civile minimum 10 000 € (loi Waserman 2022).";
                }
                case SANCTION -> {
                    libelleMesure = "Sanction disciplinaire de représailles détectée";
                    conclusion = "La sanction prononcée en représailles du signalement est NULLE de plein droit (L. 1132-3-3 CT). Annulation de la sanction + indemnisation du préjudice + sanction civile minimum 10 000 € (loi Waserman 2022).";
                }
                case MESURE_DISCRIMINATOIRE -> {
                    libelleMesure = "Mesure discriminatoire de représailles détectée";
                    conclusion = "Toute mesure discriminatoire (mutation, refus de promotion, baisse de rémunération, etc.) prise en représailles est NULLE de plein droit (L. 1132-3-3 CT) — droit au rétablissement + indemnisation + sanction civile minimum 10 000 €.";
                }
                case AUTRE -> {
                    libelleMesure = "Autre mesure défavorable détectée";
                    conclusion = "Toute mesure défavorable directe ou indirecte prise à l'encontre du lanceur d'alerte est interdite et susceptible d'annulation (L. 1132-3-3 CT) — qualification précise à examiner.";
                }
                case AUCUNE -> {
                    libelleMesure = "Mesure de représailles signalée mais nature inconnue";
                    conclusion = "Incohérence : `mesureRepresailleDetectee=true` mais `natureMesureRepresaille=AUCUNE` — clarifier la nature de la mesure auprès du client.";
                }
            }
            points.add(new PointAnalyse(
                    CodeCritere.DT61_MESURE_REPRESAILLE,
                    libelleMesure,
                    "L. 1132-3-3 CT — interdiction des mesures de représailles",
                    conclusion));
            messages.add("Mesure de représailles → nullité de plein droit (L. 1132-3-3 CT).");
            bases.add("L. 1132-3-3 CT — nullité de plein droit des mesures de représailles");
            nulliteMesure = input.natureMesureRepresaille()
                    != LanceurAlerteProtectionInput.NatureMesureRepresaille.AUCUNE;
        } else {
            points.add(new PointAnalyse(
                    CodeCritere.DT61_MESURE_REPRESAILLE,
                    "Aucune mesure de représailles détectée",
                    "L. 1132-3-3 CT",
                    "Aucune mesure défavorable n'est rapportée — la question de la nullité de plein droit ne se pose pas en l'état des pièces."));
        }

        // ── 5. Verdict et score ─────────────────────────────────────────────
        AnalyseLanceurAlerte verdict;
        int score = 0;

        if (contrepartie) {
            verdict = AnalyseLanceurAlerte.HORS_CHAMP;
            score = 0;
            messages.add(0, "HORS CHAMP — contrepartie financière directe (cause d'exclusion expresse loi Waserman 2022).");
        } else if (!natureProtegee) {
            // Nature non protégée → hors champ matériel.
            verdict = AnalyseLanceurAlerte.HORS_CHAMP;
            score = 10;
            messages.add(0, "HORS CHAMP — la nature du signalement ne relève pas des catégories matérielles protégées par la loi Waserman 2022.");
        } else if (procedureConforme) {
            verdict = AnalyseLanceurAlerte.PROTECTION_FORTE;
            score = representailles ? 95 : 80;
            messages.add(0, "PROTECTION FORTE — qualité de lanceur d'alerte caractérisée, procédure conforme. Régime protecteur L. 1132-3-3 CT + loi Waserman 2022 pleinement applicable.");
        } else {
            verdict = AnalyseLanceurAlerte.PROTECTION_PARTIELLE;
            score = representailles ? 55 : 40;
            messages.add(0, "PROTECTION PARTIELLE — qualité de lanceur d'alerte caractérisée mais la procédure de signalement présente une fragilité (référent non saisi ou divulgation publique conditionnelle). Risque de requalification.");
        }

        // ── 6. Montant minimum DI loi Waserman 2022 ─────────────────────────
        double montantMinDi;
        if (verdict == AnalyseLanceurAlerte.PROTECTION_FORTE
                || verdict == AnalyseLanceurAlerte.PROTECTION_PARTIELLE) {
            montantMinDi = MONTANT_MIN_DOMMAGES_INTERETS;
            messages.add("Montant minimum de dommages et intérêts en cas de procédure abusive : 10 000 € (loi Waserman 2022).");
            bases.add("Loi Waserman 2022 — sanction civile minimum 10 000 €");
        } else {
            montantMinDi = 0.0;
        }

        // ── 7. Bases juridiques transverses ─────────────────────────────────
        if (verdict != AnalyseLanceurAlerte.HORS_CHAMP) {
            bases.add("L. 1132-3-3 CT — protection des lanceurs d'alerte (Sapin II + Waserman)");
            messages.add("Le lanceur d'alerte peut solliciter du juge des mesures provisoires (loi Waserman 2022) — protection préventive contre les représailles.");
        }

        // Borner le score à [0, 100].
        score = Math.max(0, Math.min(100, score));

        return new Result(
                verdict,
                score,
                List.copyOf(points),
                nulliteMesure,
                montantMinDi,
                List.copyOf(bases),
                List.copyOf(messages),
                "FRANCE"
        );
    }
}
