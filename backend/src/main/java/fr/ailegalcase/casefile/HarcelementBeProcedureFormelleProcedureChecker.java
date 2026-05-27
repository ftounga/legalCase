package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-213-07 : analyseur de procédure interne BE de plainte pour
 * harcèlement moral / sexuel — fonction <b>pure</b>.
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Loi du 4 août 1996</b> relative au bien-être des travailleurs
 *       lors de l'exécution de leur travail, <b>art. 32bis–32sexies</b> :
 *       définition du harcèlement moral / sexuel, procédure interne
 *       (CPAP / personne de confiance / demande formelle), protection
 *       contre les représailles (art. 32sexies — 12 mois post-dépôt).</li>
 *   <li><b>AR du 10 avril 2014</b> relatif à la prévention des risques
 *       psychosociaux au travail : modalités de la procédure (demande
 *       informelle, demande formelle, délais, rôle CPP et CPAP).</li>
 * </ul>
 *
 * <h2>Logique</h2>
 * <p>Pour chaque étape ({@link HarcelementBeEtapeEnum}), une checklist
 * d'items contextualisés est construite. Les délais clés
 * (90 jours d'enquête, 12 mois de protection) sont calculés à partir de
 * {@code dateDepotPlainte}.</p>
 *
 * <h2>Protection contre les représailles (art. 32sexies)</h2>
 * <ul>
 *   <li>{@code representaillesPossibles = true} si
 *       {@code mesureDefavorableApres && dateDepotPlainte != null}.</li>
 *   <li>Fenêtre de protection : {@code dateDepotPlainte} →
 *       {@code dateDepotPlainte + 12 mois}.</li>
 * </ul>
 *
 * <h2>Avertissements</h2>
 * <ul>
 *   <li>Entreprise <b>≥ 50</b> sans CPAP → manquement employeur, recours
 *       direct tribunal possible.</li>
 *   <li>Entreprise <b>&lt; 50</b> sans CPAP → recours via SPF Emploi ou
 *       directement tribunal.</li>
 *   <li>Mesure défavorable au-delà des 365 jours de protection →
 *       présomption affaiblie.</li>
 * </ul>
 */
public final class HarcelementBeProcedureFormelleProcedureChecker {

    static final String BASE_JURIDIQUE =
            "Loi du 04/08/1996 art. 32bis-32sexies (bien-être au travail) ; "
                    + "AR du 10/04/2014 (prévention risques psychosociaux)";

    static final String AVERT_GRANDE_ENTREPRISE_SANS_CPAP =
            "Entreprise ≥ 50 travailleurs sans CPAP — manquement employeur, "
                    + "recours direct tribunal possible";

    static final String AVERT_PETITE_ENTREPRISE_SANS_CPAP =
            "Entreprise < 50 sans CPAP — recours via SPF Emploi ou "
                    + "directement tribunal";

    static final String AVERT_HORS_FENETRE_PROTECTION =
            "Mesure défavorable au-delà des 12 mois de protection — "
                    + "présomption affaiblie, à analyser cas par cas";

    /** Délai légal d'enquête CPAP en demande formelle (AR 10/04/2014). */
    private static final int DELAI_ENQUETE_JOURS = 90;

    /** Durée de la fenêtre de protection contre représailles (art. 32sexies). */
    private static final int PROTECTION_MOIS = 12;

    /** Seuil au-delà duquel la présomption de représailles est affaiblie. */
    private static final int SEUIL_PROTECTION_JOURS = 365;

    private HarcelementBeProcedureFormelleProcedureChecker() {
    }

    public static HarcelementBeProcedureFormelleResult analyze(
            HarcelementBeProcedureFormelleRequest request) {
        validateInputs(request);

        List<HarcelementBeChecklistItemRecord> checklist = buildChecklist(
                request.etapeProcedure(), request.dateDepotPlainte());

        boolean representaillesPossibles =
                request.mesureDefavorableApres() && request.dateDepotPlainte() != null;

        LocalDate dateDebutProtection = request.dateDepotPlainte();
        LocalDate dateFinProtection = request.dateDepotPlainte() == null
                ? null
                : request.dateDepotPlainte().plusMonths(PROTECTION_MOIS);

        LocalDate prochainDelaiFatal = computeProchainDelaiFatal(
                request.etapeProcedure(), request.dateDepotPlainte());

        String avertissement = buildAvertissement(
                request.entreprisePossedeCPAP(),
                request.entrepriseTaille(),
                request.mesureDefavorableApres(),
                request.delaiDepuisDepotJours());

        return new HarcelementBeProcedureFormelleResult(
                request.typeHarcelement(),
                request.etapeProcedure(),
                request.dateDepotPlainte(),
                request.entreprisePossedeCPAP(),
                request.entrepriseTaille(),
                request.mesureDefavorableApres(),
                request.delaiDepuisDepotJours(),
                checklist,
                representaillesPossibles,
                dateDebutProtection,
                dateFinProtection,
                prochainDelaiFatal,
                BASE_JURIDIQUE,
                avertissement);
    }

    // ── Checklist par étape ────────────────────────────────────────────────

    private static List<HarcelementBeChecklistItemRecord> buildChecklist(
            HarcelementBeEtapeEnum etape, LocalDate dateDepotPlainte) {
        List<HarcelementBeChecklistItemRecord> items = new ArrayList<>();
        switch (etape) {
            case AVANT_DEPOT -> {
                items.add(new HarcelementBeChecklistItemRecord(
                        "Identifier le Conseiller en Prévention Aspects "
                                + "Psychosociaux (interne ou SEPP)",
                        HarcelementBeChecklistStatutEnum.A_FAIRE, null));
                items.add(new HarcelementBeChecklistItemRecord(
                        "Choisir voie informelle ou formelle",
                        HarcelementBeChecklistStatutEnum.A_FAIRE, null));
                items.add(new HarcelementBeChecklistItemRecord(
                        "Consigner les faits avec dates précises",
                        HarcelementBeChecklistStatutEnum.A_FAIRE, null));
            }
            case DEMANDE_INFORMELLE_EN_COURS -> {
                items.add(new HarcelementBeChecklistItemRecord(
                        "Délai de réponse CPAP (pas de délai légal strict)",
                        HarcelementBeChecklistStatutEnum.EN_COURS, null));
                items.add(new HarcelementBeChecklistItemRecord(
                        "Si échec ou refus employeur → passer à la demande formelle",
                        HarcelementBeChecklistStatutEnum.A_FAIRE, null));
            }
            case DEMANDE_FORMELLE_EN_COURS -> {
                LocalDate finEnquete = dateDepotPlainte == null
                        ? null
                        : dateDepotPlainte.plusDays(DELAI_ENQUETE_JOURS);
                LocalDate finProtection = dateDepotPlainte == null
                        ? null
                        : dateDepotPlainte.plusMonths(PROTECTION_MOIS);
                items.add(new HarcelementBeChecklistItemRecord(
                        "Enquête CPAP — délai 90 jours (AR 10/04/2014)",
                        HarcelementBeChecklistStatutEnum.EN_COURS, finEnquete));
                items.add(new HarcelementBeChecklistItemRecord(
                        "Conserver copie de la plainte écrite + accusé de "
                                + "réception CPAP",
                        HarcelementBeChecklistStatutEnum.A_FAIRE, null));
                items.add(new HarcelementBeChecklistItemRecord(
                        "Protection contre représailles active dès dépôt "
                                + "(12 mois)",
                        HarcelementBeChecklistStatutEnum.ACTIF, finProtection));
            }
            case ENQUETE_TERMINEE -> {
                items.add(new HarcelementBeChecklistItemRecord(
                        "Vérifier mesures prises par l'employeur "
                                + "(avertissement, sanction, mutation harceleur)",
                        HarcelementBeChecklistStatutEnum.A_FAIRE, null));
                items.add(new HarcelementBeChecklistItemRecord(
                        "Si insuffisant → saisir Tribunal du travail "
                                + "(CJ art. 578)",
                        HarcelementBeChecklistStatutEnum.A_FAIRE, null));
            }
            case MESURE_DEFAVORABLE_APRES_PLAINTE -> {
                items.add(new HarcelementBeChecklistItemRecord(
                        "Présomption de représailles "
                                + "(Loi 04/08/1996 art. 32sexies)",
                        HarcelementBeChecklistStatutEnum.ACTIF, null));
                items.add(new HarcelementBeChecklistItemRecord(
                        "Recours via outil F-DT-11 (licenciement nul "
                                + "représailles) si rupture",
                        HarcelementBeChecklistStatutEnum.A_FAIRE, null));
                items.add(new HarcelementBeChecklistItemRecord(
                        "Dépôt SPF Emploi / Inspection sociale",
                        HarcelementBeChecklistStatutEnum.A_FAIRE, null));
            }
        }
        return items;
    }

    // ── Prochain délai fatal ───────────────────────────────────────────────

    private static LocalDate computeProchainDelaiFatal(
            HarcelementBeEtapeEnum etape, LocalDate dateDepotPlainte) {
        if (etape == HarcelementBeEtapeEnum.DEMANDE_FORMELLE_EN_COURS
                && dateDepotPlainte != null) {
            return dateDepotPlainte.plusDays(DELAI_ENQUETE_JOURS);
        }
        return null;
    }

    // ── Avertissements ─────────────────────────────────────────────────────

    private static String buildAvertissement(
            boolean entreprisePossedeCPAP,
            HarcelementBeTailleEntrepriseEnum entrepriseTaille,
            boolean mesureDefavorableApres,
            Integer delaiDepuisDepotJours) {
        // 1. Pas de CPAP — avertissement selon taille
        if (!entreprisePossedeCPAP) {
            if (entrepriseTaille == HarcelementBeTailleEntrepriseEnum.CINQUANTE_ET_PLUS) {
                return AVERT_GRANDE_ENTREPRISE_SANS_CPAP;
            }
            return AVERT_PETITE_ENTREPRISE_SANS_CPAP;
        }
        // 2. Mesure défavorable au-delà des 12 mois → présomption affaiblie
        if (mesureDefavorableApres
                && delaiDepuisDepotJours != null
                && delaiDepuisDepotJours > SEUIL_PROTECTION_JOURS) {
            return AVERT_HORS_FENETRE_PROTECTION;
        }
        return null;
    }

    // ── Validation conditionnelle ──────────────────────────────────────────

    private static void validateInputs(HarcelementBeProcedureFormelleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.typeHarcelement() == null) {
            throw new IllegalArgumentException("typeHarcelement est requis");
        }
        if (request.etapeProcedure() == null) {
            throw new IllegalArgumentException("etapeProcedure est requis");
        }
        if (request.entrepriseTaille() == null) {
            throw new IllegalArgumentException("entrepriseTaille est requis");
        }
        if (request.etapeProcedure() != HarcelementBeEtapeEnum.AVANT_DEPOT
                && request.dateDepotPlainte() == null) {
            throw new IllegalArgumentException(
                    "Date de dépôt requise pour cette étape");
        }
        if (request.delaiDepuisDepotJours() != null
                && request.delaiDepuisDepotJours() < 0) {
            throw new IllegalArgumentException(
                    "delaiDepuisDepotJours ne peut pas être négatif");
        }
    }
}
