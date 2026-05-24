package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-212-19 : moteur d'analyse de la régularité d'une mise à pied disciplinaire
 * (F-DT-48-mise-a-pied-disciplinaire, FRANCE — L. 1331-1 CT ;
 * L. 1332-1 à L. 1332-3 CT ; L. 1332-4 CT — prescription des faits 2 mois ;
 * L. 1311-2 CT — durée définie dans le règlement intérieur ; Cass. soc.
 * 26/10/2010 n°09-42.740 — distinction mise à pied conservatoire vs
 * disciplinaire ; jurisprudence constante Cass. soc. — interdiction de la
 * double sanction des mêmes faits).
 *
 * <p>L'outil distingue quatre verdicts :
 * <ul>
 *   <li>{@code REGULIERE} — mise à pied disciplinaire avec procédure
 *       d'entretien préalable suivie, durée définie au règlement intérieur,
 *       prescription des faits respectée, absence de double sanction et
 *       salaire effectivement suspendu. La sanction est opposable au
 *       salarié.</li>
 *   <li>{@code IRREGULIERE_FORME} — vice de forme (procédure d'entretien
 *       préalable non suivie, durée non définie au règlement intérieur).
 *       La sanction encourt l'annulation pour non-respect du formalisme
 *       protecteur des art. L. 1332-1 et s. CT — le salaire est dû pour
 *       la période.</li>
 *   <li>{@code IRREGULIERE_FOND} — vice de fond (prescription dépassée,
 *       double sanction). La sanction est sans cause — Cass. soc.
 *       constante. Salaire dû.</li>
 *   <li>{@code CONSERVATOIRE} — la mesure est qualifiée de mise à pied
 *       conservatoire (Cass. soc. 26/10/2010), donc provisoire dans
 *       l'attente de la décision finale. Le salaire est dû pendant la
 *       période sauf si la sanction finale est un licenciement pour
 *       faute grave ou lourde — non couvert par cet outil (cf. F-DT-31
 *       faute grave/lourde).</li>
 * </ul>
 *
 * <p><b>Pays</b> : FRANCE uniquement — la procédure disciplinaire FR
 * (L. 1332-1 à L. 1332-4 CT) n'a pas d'équivalent direct côté BE. La mise à
 * pied y relève du régime de la Loi du 03/07/1978 et de la CCT applicable.
 */
public final class MiseAPiedDisciplinaireCalculator {

    /** Verdict d'analyse de la régularité de la mise à pied — 4 niveaux. */
    public enum AnalyseMiseAPied {
        REGULIERE,
        IRREGULIERE_FORME,
        IRREGULIERE_FOND,
        CONSERVATOIRE
    }

    /** Code structuré d'un point de régularité (F-IA-03). */
    public enum CodeCritere {
        DT48_NATURE_MISE_A_PIED,
        DT48_PROCEDURE_SUIVIE,
        DT48_PRESCRIPTION_FAUTE,
        DT48_DOUBLE_SANCTION
    }

    /** Point de régularité exposé dans la réponse API. */
    public record PointRegularite(
            CodeCritere code,
            String libelle,
            String fondement,
            String conclusion
    ) {}

    /** Résultat calculé par {@link #compute}. */
    public record Result(
            AnalyseMiseAPied analyseMiseAPied,
            int scoreMiseAPied,
            List<PointRegularite> pointsRegularite,
            double salaireDuPeriodeMiseAPiedEuros,
            List<String> basesJuridiques,
            List<String> messages,
            String country
    ) {}

    /** Délai légal de prescription des faits fautifs (L. 1332-4 CT, en mois). */
    private static final int PRESCRIPTION_FAUTE_MOIS = 2;

    /** Nombre approximatif de jours ouvrés par mois pour le calcul du salaire dû. */
    private static final double JOURS_OUVRES_PAR_MOIS = 21.67;

    private MiseAPiedDisciplinaireCalculator() {}

    /**
     * Point d'entrée principal.
     *
     * @param input   données saisies par l'avocat
     * @param country pays du workspace
     * @return résultat calculé
     * @throws IllegalArgumentException si {@code country != "FRANCE"} ou si
     *         {@code input} est null, ou si une valeur de durée est négative.
     */
    public static Result compute(MiseAPiedDisciplinaireInput input, String country) {
        if (!"FRANCE".equalsIgnoreCase(country)) {
            throw new IllegalArgumentException(
                    "Outil réservé au droit du travail FRANCE — pays détecté : " + country);
        }
        if (input == null) {
            throw new IllegalArgumentException("Données d'entrée manquantes");
        }
        if (input.natureMiseAPied() == null) {
            throw new IllegalArgumentException("Nature de la mise à pied requise");
        }
        if (input.dureeJours() != null && input.dureeJours() < 0) {
            throw new IllegalArgumentException(
                    "La durée de la mise à pied ne peut pas être négative");
        }
        if (input.salaireMensuelBrutEuros() < 0) {
            throw new IllegalArgumentException(
                    "Le salaire mensuel brut ne peut pas être négatif");
        }

        List<PointRegularite> points = new ArrayList<>();
        List<String> bases = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        int score = 0;

        MiseAPiedDisciplinaireInput.NatureMiseAPied nature = input.natureMiseAPied();
        boolean procedureSuivie = input.procedureEntretienSuivie();
        boolean prescriptionOk = input.prescriptionFauteVerifiee();
        boolean dureeRi = input.dureeDefiniedansRI();
        Integer dureeJours = input.dureeJours();
        boolean salaireSuspendu = input.salaireSuspendu();
        boolean doubleSanction = input.sancionsAnterieuresMemesFaits();
        double salaireMensuel = input.salaireMensuelBrutEuros();

        // ── 1. DT48_NATURE_MISE_A_PIED — qualification de la mesure ──────────
        switch (nature) {
            case DISCIPLINAIRE -> {
                points.add(new PointRegularite(
                        CodeCritere.DT48_NATURE_MISE_A_PIED,
                        "Mise à pied qualifiée de DISCIPLINAIRE",
                        "L. 1331-1 CT — la mise à pied disciplinaire est une sanction définitive privant le salarié de salaire pendant une durée fixée",
                        "DISCIPLINAIRE — sanction définitive, salaire suspendu, inscription au dossier."));
                bases.add("L. 1331-1 CT — définition de la sanction disciplinaire");
            }
            case CONSERVATOIRE -> {
                points.add(new PointRegularite(
                        CodeCritere.DT48_NATURE_MISE_A_PIED,
                        "Mise à pied qualifiée de CONSERVATOIRE",
                        "Cass. soc. 26/10/2010 n°09-42.740 — la mise à pied conservatoire est une mesure provisoire dans l'attente de la décision finale ; le salaire est dû si la sanction finale est inférieure à un licenciement",
                        "CONSERVATOIRE — mesure provisoire ; salaire dû pendant la période sauf licenciement pour faute grave/lourde."));
                bases.add("Cass. soc. 26/10/2010 n°09-42.740 — mise à pied conservatoire");
            }
            case INCONNUE -> {
                points.add(new PointRegularite(
                        CodeCritere.DT48_NATURE_MISE_A_PIED,
                        "Nature de la mise à pied non qualifiée par l'employeur",
                        "Cass. soc. 26/10/2010 n°09-42.740 — à défaut de qualification expresse, le juge requalifie en fonction de la réalité de la mesure",
                        "INCONNUE — à requalifier au regard de l'intention de l'employeur (sanction définitive vs mesure provisoire)."));
                bases.add("Cass. soc. 26/10/2010 n°09-42.740 — requalification judiciaire");
            }
        }

        // ── 2. DT48_PROCEDURE_SUIVIE — formalisme L. 1332-1 à L. 1332-3 ──────
        // Concerne uniquement la mise à pied disciplinaire ou inconnue.
        boolean procedureNonSuivie = !procedureSuivie;
        boolean dureeNonDansRi = !dureeRi;

        if (nature != MiseAPiedDisciplinaireInput.NatureMiseAPied.CONSERVATOIRE) {
            if (procedureSuivie) {
                points.add(new PointRegularite(
                        CodeCritere.DT48_PROCEDURE_SUIVIE,
                        "Procédure disciplinaire suivie (convocation + entretien préalable + notification)",
                        "L. 1332-1 à L. 1332-3 CT — la sanction au-delà de l'avertissement requiert convocation à entretien préalable, entretien tenu et notification écrite motivée",
                        "OK — formalisme L. 1332-1 et s. respecté."));
                bases.add("L. 1332-1 à L. 1332-3 CT — procédure disciplinaire");
            } else {
                points.add(new PointRegularite(
                        CodeCritere.DT48_PROCEDURE_SUIVIE,
                        "Procédure disciplinaire non suivie",
                        "L. 1332-2 CT — l'absence de convocation à entretien préalable ou d'entretien tenu vicie la sanction de la mise à pied",
                        "VICE FORME — la sanction encourt l'annulation pour non-respect du formalisme."));
                bases.add("L. 1332-2 CT — exigence de l'entretien préalable");
            }

            if (dureeRi) {
                points.add(new PointRegularite(
                        CodeCritere.DT48_PROCEDURE_SUIVIE,
                        "Durée définie dans le règlement intérieur ou l'accord collectif",
                        "L. 1311-2 CT ; Cass. soc. 26/10/2010 — la durée maximale d'une mise à pied disciplinaire doit être prévue par le règlement intérieur ou l'accord collectif applicable",
                        "OK — quantum de la sanction prévu par le règlement intérieur."));
                bases.add("L. 1311-2 CT — règlement intérieur, durée des sanctions");
            } else {
                points.add(new PointRegularite(
                        CodeCritere.DT48_PROCEDURE_SUIVIE,
                        "Durée non définie dans le règlement intérieur",
                        "L. 1311-2 CT — sans durée prévue par le règlement intérieur ou un accord collectif, la mise à pied disciplinaire est nulle",
                        "VICE FORME — absence de fondement réglementaire au quantum."));
                bases.add("L. 1311-2 CT — nullité si absence de RI prévoyant la durée");
            }
        }

        // ── 3. DT48_PRESCRIPTION_FAUTE — L. 1332-4 CT, délai 2 mois ──────────
        if (nature != MiseAPiedDisciplinaireInput.NatureMiseAPied.CONSERVATOIRE) {
            if (prescriptionOk) {
                points.add(new PointRegularite(
                        CodeCritere.DT48_PRESCRIPTION_FAUTE,
                        "Sanction prononcée dans le délai de 2 mois (prescription respectée)",
                        "L. 1332-4 CT — aucun fait fautif ne peut donner lieu à lui seul à l'engagement de poursuites disciplinaires au-delà d'un délai de 2 mois à compter du jour où l'employeur en a eu connaissance",
                        "OK — prescription des faits respectée."));
                bases.add("L. 1332-4 CT — prescription " + PRESCRIPTION_FAUTE_MOIS + " mois");
            } else {
                points.add(new PointRegularite(
                        CodeCritere.DT48_PRESCRIPTION_FAUTE,
                        "Prescription des faits dépassée (> 2 mois)",
                        "L. 1332-4 CT ; Cass. soc. constante — la sanction prononcée plus de 2 mois après la connaissance des faits est prescrite et doit être annulée",
                        "VICE FOND — sanction prescrite, à annuler."));
                bases.add("L. 1332-4 CT — prescription dépassée = nullité de la sanction");
            }
        }

        // ── 4. DT48_DOUBLE_SANCTION — non bis in idem disciplinaire ─────────
        if (nature != MiseAPiedDisciplinaireInput.NatureMiseAPied.CONSERVATOIRE) {
            if (doubleSanction) {
                points.add(new PointRegularite(
                        CodeCritere.DT48_DOUBLE_SANCTION,
                        "Sanction antérieure déjà prononcée pour les mêmes faits",
                        "Cass. soc. constante — un même fait ne peut donner lieu qu'à une seule sanction disciplinaire (principe non bis in idem disciplinaire)",
                        "VICE FOND — double sanction interdite."));
                bases.add("Cass. soc. constante — interdiction de la double sanction");
            } else {
                points.add(new PointRegularite(
                        CodeCritere.DT48_DOUBLE_SANCTION,
                        "Absence de sanction antérieure pour les mêmes faits",
                        "Cass. soc. constante — principe non bis in idem disciplinaire respecté",
                        "OK — pas de double sanction."));
            }
        }

        // ── 5. Verdict global ────────────────────────────────────────────────
        AnalyseMiseAPied verdict;
        boolean ficheVice = procedureNonSuivie || dureeNonDansRi;
        boolean fondVice = !prescriptionOk || doubleSanction;

        if (nature == MiseAPiedDisciplinaireInput.NatureMiseAPied.CONSERVATOIRE) {
            verdict = AnalyseMiseAPied.CONSERVATOIRE;
            score = 50;
            messages.add("Mesure qualifiée de mise à pied conservatoire — provisoire dans "
                    + "l'attente de la décision finale (Cass. soc. 26/10/2010 n°09-42.740). "
                    + "Le salaire est dû pendant la période sauf si la sanction finale est "
                    + "un licenciement pour faute grave ou lourde.");
        } else if (fondVice) {
            verdict = AnalyseMiseAPied.IRREGULIERE_FOND;
            score = 15;
            if (!prescriptionOk) {
                messages.add("Sanction prescrite — prononcée plus de 2 mois après la "
                        + "connaissance des faits par l'employeur (L. 1332-4 CT). "
                        + "Annulation encourue, salaire dû pour la période.");
            }
            if (doubleSanction) {
                messages.add("Double sanction interdite — un même fait ne peut donner lieu "
                        + "qu'à une seule sanction disciplinaire (Cass. soc. constante). "
                        + "Annulation encourue, salaire dû pour la période.");
            }
        } else if (ficheVice) {
            verdict = AnalyseMiseAPied.IRREGULIERE_FORME;
            score = 30;
            if (procedureNonSuivie) {
                messages.add("Procédure d'entretien préalable non suivie (L. 1332-2 CT). "
                        + "La sanction est viciée pour non-respect du formalisme protecteur "
                        + "— annulation encourue, salaire dû pour la période.");
            }
            if (dureeNonDansRi) {
                messages.add("Durée de la mise à pied non prévue par le règlement intérieur "
                        + "ou l'accord collectif (L. 1311-2 CT). La sanction encourt la "
                        + "nullité — salaire dû pour la période.");
            }
        } else {
            verdict = AnalyseMiseAPied.REGULIERE;
            score = 85;
            if (nature == MiseAPiedDisciplinaireInput.NatureMiseAPied.INCONNUE) {
                score -= 15;
                messages.add("La nature de la mise à pied n'a pas été qualifiée par "
                        + "l'employeur — à requalifier au regard de la réalité de la mesure "
                        + "(Cass. soc. 26/10/2010 n°09-42.740). À défaut de qualification, "
                        + "le juge peut requalifier en conservatoire et restituer le salaire.");
            } else if (!salaireSuspendu) {
                score -= 10;
                messages.add("Mise à pied disciplinaire annoncée mais salaire non suspendu — "
                        + "incohérence avec la nature de la sanction (L. 1331-1 CT). "
                        + "À vérifier auprès du service paie.");
            } else {
                messages.add("Mise à pied disciplinaire régulière — procédure suivie, "
                        + "durée prévue au règlement intérieur, prescription respectée, "
                        + "absence de double sanction. Salaire suspendu sur la période.");
            }
        }

        // ── 6. Salaire dû pendant la période ─────────────────────────────────
        // - REGULIERE (DISCIPLINAIRE) → 0 (suspension licite du salaire).
        // - IRREGULIERE_FORME / IRREGULIERE_FOND → salaire dû (sanction non opposable).
        // - CONSERVATOIRE → salaire dû (mesure provisoire — Cass. soc. 26/10/2010).
        double salaireDu;
        if (verdict == AnalyseMiseAPied.REGULIERE
                && nature == MiseAPiedDisciplinaireInput.NatureMiseAPied.DISCIPLINAIRE) {
            salaireDu = 0.0;
        } else if (dureeJours != null && dureeJours > 0 && salaireMensuel > 0.0) {
            double salaireParJour = salaireMensuel / JOURS_OUVRES_PAR_MOIS;
            salaireDu = Math.round(salaireParJour * dureeJours * 100.0) / 100.0;
        } else {
            salaireDu = 0.0;
        }

        // ── 7. Bases juridiques génériques ───────────────────────────────────
        bases.add(0, "Cass. soc. — jurisprudence constante sur la mise à pied disciplinaire et la double sanction");
        bases.add("L. 1331-1 CT — pouvoir disciplinaire de l'employeur");

        // Borner le score à [0, 100]
        score = Math.max(0, Math.min(100, score));

        return new Result(
                verdict,
                score,
                List.copyOf(points),
                salaireDu,
                List.copyOf(bases),
                List.copyOf(messages),
                "FRANCE"
        );
    }
}
