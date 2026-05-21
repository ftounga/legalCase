package fr.ailegalcase.casefile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.AuteurRupture;
import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.CategorieSocioProfessionnelle;
import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.DiscriminationMotif;
import fr.ailegalcase.casefile.RupturePeriodeEssaiCalculator.TypeContrat;

/**
 * SF-DT-38-01 : réponse de l'endpoint de qualification d'une rupture pendant
 * la période d'essai (FR).
 *
 * <p>Inclut un snapshot complet des inputs (pour pré-remplissage UI) ET les
 * sorties calculées (verdict, score, anomalies, indemnité, remède réintégration,
 * bases juridiques).</p>
 */
public record RupturePeriodeEssaiResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        CategorieSocioProfessionnelle categorieSocioProfessionnelle,
        TypeContrat typeContrat,
        Integer dureeCddMois,
        LocalDate dateDebutContrat,
        LocalDate dateRupture,
        Integer dureePeriodeEssaiContractuelleMois,
        Boolean renouvellementInvoque,
        Boolean accordBrancheRenouvellement,
        Boolean accordEcritSalarieRenouvellement,
        AuteurRupture auteurRupture,
        Integer delaiPrevenanceJoursAppliques,
        String motifInvoque,
        Boolean motifLieAuxCompetencesProfessionnelles,
        Boolean motifEconomiqueOuOrganisationnel,
        DiscriminationMotif discriminationInvoquee,
        Boolean grossesseAuMomentRupture,
        Boolean arretAccidentTravailEnCours,
        String atteinteLiberteFondamentale,
        Boolean lettreRuptureMotivee,
        Boolean motifsAveresParPieces,
        Boolean conventionCollectiveApplicable,
        Boolean conventionCollectivePlusFavorableRespectee,
        Double salaireMensuelBrut,
        // --- SF-252-01 — 7 protections nullité additionnelles (latent bug fix Response) ---
        Boolean salarieProtege,
        Boolean autorisationInspectionTravailObtenue,
        Boolean lanceurAlerte,
        Boolean temoinOuVictimeHarcelement,
        Boolean droitDeRetraitExerce,
        Boolean grossesseDeclareePostRupture,
        LocalDate dateNotificationGrossesse,
        // --- SF-252b-01 — barème CDD/INTERIM précis (audit 2026-05-20) ---
        Integer dureePeriodeEssaiContractuelleJours,
        // --- Outputs calculés ---
        RupturePeriodeEssaiCalculator.Verdict verdict,
        int scoreIrregularite,
        int ancienneteJoursAuMomentRupture,
        int dureeLegaleMaximaleMois,
        int delaiPrevenanceLegalJours,
        boolean delaiPrevenanceRespecte,
        List<RupturePeriodeEssaiCalculator.Anomalie> anomaliesDetectees,
        RupturePeriodeEssaiCalculator.IndemniteEstimee indemniteEstimee,
        boolean remedeReintegration,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt,
        // --- SF-252b-01 — Outputs additionnels (audit 2026-05-20) ---
        /** Durée légale max en jours (barèmes exacts CDD L.1242-10 / INTERIM L.1251-14). */
        int dureeLegaleMaximaleJours,
        /** Indemnité compensatrice de préavis non exécuté L.1221-25 (Cass. soc. 23/01/2013). */
        Double indemnitePrevenanceEuros
) {}
