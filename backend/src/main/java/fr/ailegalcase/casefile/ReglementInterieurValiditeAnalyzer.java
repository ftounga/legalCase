package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-218-35 : analyseur de la <b>validité d'un règlement intérieur</b> (contenu,
 * consultation, dépôt — art. L.1311-1 à L.1322-4, L.1321-1 et s. CT, F-DT-100).
 * Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Logique métier (invariant CLAUDE.md — un outil = une situation) :
 * <ul>
 *   <li><b>Applicabilité</b> — le règlement intérieur est obligatoire dès 50
 *       salariés (art. L.1311-2). Si {@code effectif < 50} et qu'aucun RI
 *       n'existe → {@code NON_REQUIS} (RI facultatif).</li>
 *   <li><b>Checklist contenu obligatoire</b> (type OBLIGATOIRE, 4 items) :
 *       hygiène/sécurité (L.1321-1 1°), discipline et échelle des sanctions
 *       (L.1321-1 2°), droits de la défense (L.1321-1 3°), rappel des
 *       dispositions harcèlement / agissements sexistes (L.1321-2).</li>
 *   <li><b>Checklist clauses interdites</b> (type INTERDIT, 2 items ; {@code
 *       conforme=true} = absence de la clause) : atteinte aux libertés non
 *       justifiée/proportionnée (L.1321-3), sanction pécuniaire (L.1331-2).</li>
 *   <li><b>Checklist procédure</b> (type PROCEDURE, 3 items) : consultation du
 *       CSE, transmission à l'inspection du travail, dépôt au greffe du CPH
 *       (L.1321-4).</li>
 *   <li><b>Verdict</b> :
 *       <ul>
 *         <li>{@code effectif < 50} et pas de RI → {@code NON_REQUIS} ;</li>
 *         <li>défaut de procédure (consultation CSE, transmission ou dépôt
 *             manquant) → {@code INOPPOSABLE} ;</li>
 *         <li>contenu obligatoire manquant ou clause interdite présente →
 *             {@code NON_CONFORME} ;</li>
 *         <li>tout conforme → {@code CONFORME}.</li>
 *       </ul>
 *       Le défaut de procédure prime : un RI inopposable n'est pas opposable aux
 *       salariés, indépendamment de la conformité de son contenu.</li>
 *   <li><b>Opposabilité</b> : INOPPOSABLE si une formalité de procédure manque,
 *       OPPOSABLE sinon.</li>
 * </ul>
 *
 * <p>Hors périmètre : validité d'une sanction disciplinaire fondée sur le RI,
 * note de service / additif au RI, régime du lanceur d'alerte (F-DT-61). Base
 * juridique annotée « à vérifier par avocat ».
 */
public final class ReglementInterieurValiditeAnalyzer {

    /** Seuil d'effectif rendant le règlement intérieur obligatoire (art. L.1311-2). */
    static final int SEUIL_EFFECTIF_OBLIGATOIRE = 50;

    static final String BASE_JURIDIQUE =
            "art. L.1311-1 à L.1322-4 CT — régime du règlement intérieur ; "
                    + "art. L.1311-2 CT — obligation d'établir un règlement intérieur dès "
                    + "50 salariés ; art. L.1321-1 CT — contenu obligatoire (mesures "
                    + "d'hygiène et de sécurité, règles de discipline et échelle des "
                    + "sanctions, droits de la défense des salariés) ; art. L.1321-2 CT — "
                    + "rappel des dispositions relatives au harcèlement et aux agissements "
                    + "sexistes ; art. L.1321-3 CT — clauses interdites (atteintes aux "
                    + "droits et libertés non justifiées ni proportionnées) ; art. L.1331-2 "
                    + "CT — interdiction des sanctions pécuniaires ; art. L.1321-4 CT — "
                    + "procédure de mise en place (consultation du CSE, transmission à "
                    + "l'inspection du travail, dépôt au greffe du conseil de prud'hommes) "
                    + "à défaut de laquelle le règlement intérieur est inopposable "
                    + "(à vérifier par avocat)";

    private ReglementInterieurValiditeAnalyzer() {
    }

    /**
     * Analyse la validité d'un règlement intérieur : apprécie l'applicabilité,
     * construit les trois checklists (contenu obligatoire, clauses interdites,
     * procédure) et rend le verdict global ainsi que l'opposabilité.
     */
    public static ReglementInterieurValiditeResult analyze(
            Integer effectif,
            Boolean reglementExiste,
            Boolean contenuHygieneSecurite,
            Boolean contenuDiscipline,
            Boolean contenuDroitsDefense,
            Boolean contenuHarcelementAgissements,
            Boolean clauseAtteinteLibertesNonJustifiee,
            Boolean clauseSanctionPecuniaire,
            Boolean consultationCseRealisee,
            Boolean transmissionInspectionTravail,
            Boolean depotGreffeCph) {

        validate(effectif, reglementExiste, contenuHygieneSecurite, contenuDiscipline,
                contenuDroitsDefense, contenuHarcelementAgissements, consultationCseRealisee,
                transmissionInspectionTravail, depotGreffeCph);

        int eff = effectif;
        boolean riExiste = reglementExiste;
        boolean hygieneSecurite = contenuHygieneSecurite;
        boolean discipline = contenuDiscipline;
        boolean droitsDefense = contenuDroitsDefense;
        boolean harcelement = contenuHarcelementAgissements;
        boolean clauseAtteinteLibertes = clauseAtteinteLibertesNonJustifiee != null
                && clauseAtteinteLibertesNonJustifiee;
        boolean clauseSanctionPecuniairePresente = clauseSanctionPecuniaire != null
                && clauseSanctionPecuniaire;
        boolean consultationCse = consultationCseRealisee;
        boolean transmissionInspection = transmissionInspectionTravail;
        boolean depotGreffe = depotGreffeCph;

        // ── Applicabilité : effectif < 50 et pas de RI → NON_REQUIS ─────────
        if (eff < SEUIL_EFFECTIF_OBLIGATOIRE && !riExiste) {
            return new ReglementInterieurValiditeResult(
                    eff,
                    false,
                    List.of(),
                    0,
                    0,
                    ReglementInterieurValiditeStatut.NON_REQUIS,
                    ReglementInterieurOpposabilite.OPPOSABLE,
                    List.of("Le règlement intérieur n'est obligatoire que dans les "
                            + "entreprises d'au moins 50 salariés (art. L.1311-2) : "
                            + "l'établissement d'un règlement intérieur est ici facultatif. "
                            + "S'il est néanmoins établi, il doit respecter le contenu, les "
                            + "interdictions et la procédure de mise en place."),
                    BASE_JURIDIQUE);
        }

        List<ReglementInterieurValiditeChecklistItem> checklist = new ArrayList<>();
        List<String> consequences = new ArrayList<>();

        // ── Contenu obligatoire (type OBLIGATOIRE) ──────────────────────────
        checklist.add(new ReglementInterieurValiditeChecklistItem(
                "Mesures d'application de la réglementation en matière d'hygiène et de sécurité",
                hygieneSecurite,
                ReglementInterieurChecklistType.OBLIGATOIRE,
                "Contenu obligatoire (art. L.1321-1 1°) : mesures d'application de la "
                        + "réglementation en matière de santé et de sécurité dans l'entreprise."));
        if (!hygieneSecurite) {
            consequences.add("Compléter le règlement intérieur par les mesures d'hygiène et "
                    + "de sécurité (art. L.1321-1 1°) : contenu obligatoire manquant.");
        }

        checklist.add(new ReglementInterieurValiditeChecklistItem(
                "Règles générales et permanentes relatives à la discipline (nature et échelle des sanctions)",
                discipline,
                ReglementInterieurChecklistType.OBLIGATOIRE,
                "Contenu obligatoire (art. L.1321-1 2°) : règles générales et permanentes "
                        + "relatives à la discipline, notamment la nature et l'échelle des "
                        + "sanctions que l'employeur peut prendre."));
        if (!discipline) {
            consequences.add("Compléter le règlement intérieur par les règles de discipline "
                    + "et l'échelle des sanctions (art. L.1321-1 2°) : contenu obligatoire "
                    + "manquant. À défaut, aucune sanction disciplinaire ne peut être prononcée "
                    + "sur le fondement du règlement intérieur.");
        }

        checklist.add(new ReglementInterieurValiditeChecklistItem(
                "Dispositions relatives aux droits de la défense des salariés",
                droitsDefense,
                ReglementInterieurChecklistType.OBLIGATOIRE,
                "Contenu obligatoire (art. L.1321-1 3°) : dispositions relatives aux droits "
                        + "de la défense des salariés (art. L.1332-1 à L.1332-3) ou résultant "
                        + "d'une convention ou d'un accord collectif."));
        if (!droitsDefense) {
            consequences.add("Compléter le règlement intérieur par les dispositions relatives "
                    + "aux droits de la défense des salariés (art. L.1321-1 3°) : contenu "
                    + "obligatoire manquant.");
        }

        checklist.add(new ReglementInterieurValiditeChecklistItem(
                "Rappel des dispositions relatives au harcèlement et aux agissements sexistes",
                harcelement,
                ReglementInterieurChecklistType.OBLIGATOIRE,
                "Contenu obligatoire (art. L.1321-2) : rappel des dispositions relatives aux "
                        + "harcèlements moral et sexuel et aux agissements sexistes prévues par "
                        + "le code du travail."));
        if (!harcelement) {
            consequences.add("Compléter le règlement intérieur par le rappel des dispositions "
                    + "relatives au harcèlement et aux agissements sexistes (art. L.1321-2) : "
                    + "contenu obligatoire manquant.");
        }

        // ── Clauses interdites (type INTERDIT ; conforme = absence) ─────────
        checklist.add(new ReglementInterieurValiditeChecklistItem(
                "Absence de clause portant atteinte aux libertés non justifiée ni proportionnée",
                !clauseAtteinteLibertes,
                ReglementInterieurChecklistType.INTERDIT,
                "Clause interdite (art. L.1321-3) : le règlement intérieur ne peut apporter "
                        + "aux droits et libertés des restrictions qui ne seraient pas justifiées "
                        + "par la nature de la tâche à accomplir ni proportionnées au but "
                        + "recherché."));
        if (clauseAtteinteLibertes) {
            consequences.add("Supprimer la clause portant atteinte aux droits et libertés non "
                    + "justifiée ni proportionnée (art. L.1321-3) : clause interdite — réputée "
                    + "non écrite, susceptible de retrait par l'inspection du travail.");
        }

        checklist.add(new ReglementInterieurValiditeChecklistItem(
                "Absence de clause de sanction pécuniaire",
                !clauseSanctionPecuniairePresente,
                ReglementInterieurChecklistType.INTERDIT,
                "Clause interdite (art. L.1331-2) : les amendes et autres sanctions "
                        + "pécuniaires sont prohibées ; toute disposition contraire est réputée "
                        + "non écrite."));
        if (clauseSanctionPecuniairePresente) {
            consequences.add("Supprimer la clause de sanction pécuniaire (art. L.1331-2) : "
                    + "clause interdite — réputée non écrite.");
        }

        // ── Procédure de mise en place (type PROCEDURE) ─────────────────────
        checklist.add(new ReglementInterieurValiditeChecklistItem(
                "Consultation du CSE avant mise en place",
                consultationCse,
                ReglementInterieurChecklistType.PROCEDURE,
                "Formalité substantielle (art. L.1321-4) : le règlement intérieur est soumis "
                        + "à l'avis du comité social et économique avant son introduction."));
        checklist.add(new ReglementInterieurValiditeChecklistItem(
                "Transmission à l'inspection du travail",
                transmissionInspection,
                ReglementInterieurChecklistType.PROCEDURE,
                "Formalité substantielle (art. L.1321-4) : le règlement intérieur est "
                        + "communiqué à l'inspecteur du travail, accompagné de l'avis du CSE."));
        checklist.add(new ReglementInterieurValiditeChecklistItem(
                "Dépôt au greffe du conseil de prud'hommes",
                depotGreffe,
                ReglementInterieurChecklistType.PROCEDURE,
                "Formalité substantielle (art. L.1321-4 et R.1321-1) : le règlement intérieur "
                        + "est déposé au greffe du conseil de prud'hommes du ressort de "
                        + "l'entreprise ou de l'établissement."));

        // ── Comptages ───────────────────────────────────────────────────────
        int itemsObligatoiresManquants = (int) checklist.stream()
                .filter(i -> i.type() == ReglementInterieurChecklistType.OBLIGATOIRE)
                .filter(i -> !i.conforme())
                .count();
        int clausesInterditesPresentes = (int) checklist.stream()
                .filter(i -> i.type() == ReglementInterieurChecklistType.INTERDIT)
                .filter(i -> !i.conforme())
                .count();

        boolean procedureRespectee = consultationCse && transmissionInspection && depotGreffe;

        ReglementInterieurOpposabilite opposabilite = procedureRespectee
                ? ReglementInterieurOpposabilite.OPPOSABLE
                : ReglementInterieurOpposabilite.INOPPOSABLE;

        // ── Verdict global ──────────────────────────────────────────────────
        ReglementInterieurValiditeStatut statut;
        if (!procedureRespectee) {
            statut = ReglementInterieurValiditeStatut.INOPPOSABLE;
            consequences.add("Le règlement intérieur est inopposable aux salariés faute de "
                    + "respect de la procédure de mise en place (art. L.1321-4) : régulariser "
                    + "les formalités manquantes (consultation du CSE, transmission à "
                    + "l'inspection du travail, dépôt au greffe du conseil de prud'hommes).");
        } else if (itemsObligatoiresManquants > 0 || clausesInterditesPresentes > 0) {
            statut = ReglementInterieurValiditeStatut.NON_CONFORME;
        } else {
            statut = ReglementInterieurValiditeStatut.CONFORME;
        }

        return new ReglementInterieurValiditeResult(
                eff,
                riExiste,
                List.copyOf(checklist),
                itemsObligatoiresManquants,
                clausesInterditesPresentes,
                statut,
                opposabilite,
                List.copyOf(consequences),
                BASE_JURIDIQUE);
    }

    private static void validate(Integer effectif,
                                 Boolean reglementExiste,
                                 Boolean contenuHygieneSecurite,
                                 Boolean contenuDiscipline,
                                 Boolean contenuDroitsDefense,
                                 Boolean contenuHarcelementAgissements,
                                 Boolean consultationCseRealisee,
                                 Boolean transmissionInspectionTravail,
                                 Boolean depotGreffeCph) {
        if (effectif == null) {
            throw new IllegalArgumentException("effectif est requis");
        }
        if (effectif <= 0) {
            throw new IllegalArgumentException("effectif doit être strictement positif");
        }
        if (reglementExiste == null) {
            throw new IllegalArgumentException("reglementExiste est requis");
        }
        if (contenuHygieneSecurite == null) {
            throw new IllegalArgumentException("contenuHygieneSecurite est requis");
        }
        if (contenuDiscipline == null) {
            throw new IllegalArgumentException("contenuDiscipline est requis");
        }
        if (contenuDroitsDefense == null) {
            throw new IllegalArgumentException("contenuDroitsDefense est requis");
        }
        if (contenuHarcelementAgissements == null) {
            throw new IllegalArgumentException("contenuHarcelementAgissements est requis");
        }
        if (consultationCseRealisee == null) {
            throw new IllegalArgumentException("consultationCseRealisee est requis");
        }
        if (transmissionInspectionTravail == null) {
            throw new IllegalArgumentException("transmissionInspectionTravail est requis");
        }
        if (depotGreffeCph == null) {
            throw new IllegalArgumentException("depotGreffeCph est requis");
        }
    }
}
