package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-219-25 : résultat brut produit par
 * {@link AuditoratTravailBeChecker} — fonction <b>pure</b> (sans
 * dépendance HTTP / persistance / horloge).
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link AuditoratTravailBeService} dans
 * {@link AuditoratTravailBeResponse}).</p>
 *
 * <p>Outputs dérivés : verdict de pertinence de saisine, mode de
 * saisine recommandé, drapeaux conditionnels (constitution partie
 * civile recommandée, inspection préalable requise, voie civile
 * concurrente, prescription bloquante, urgence à signaler à
 * l'auditeur).</p>
 */
public record AuditoratTravailBeResult(
        // Inputs (snapshot)
        AuditoratTravailBeNatureFait natureFait,
        AuditoratTravailBeModeSaisine modeSaisineEnvisage,
        LocalDate dateFaits,
        boolean faitsPrescrits,
        boolean inspectionDejaSaisie,
        boolean plainteCivileDeposee,
        boolean urgenceSecuritePersonnes,
        boolean recoursPenalEnvisage,
        boolean employeurPersonneMorale,

        // Outputs analytiques
        AuditoratTravailBeVerdict verdict,
        AuditoratTravailBeModeSaisine modeSaisineRecommande,
        boolean constitutionPartieCivileRecommandee,
        boolean inspectionPrealableRequise,
        boolean voieCivileConcurrente,
        boolean prescriptionBloquante,
        boolean urgenceSignalee,
        boolean unaViaApplicable,
        boolean avisAuditeurObligatoireCivil,

        // Synthèse + base juridique
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
