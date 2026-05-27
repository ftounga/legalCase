package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-215-05 : payload de réponse HTTP pour
 * {@code /api/v1/case-files/{id}/regroupement-10bis-be-analysis}.
 *
 * <p>Différence majeure vs {@link Regroupement10terBeResponse} : champs
 * {@code dateFinCarteA} et {@code conditionTitreEnCours} — permettent au
 * frontend d'afficher la raison du verdict INELIGIBLE même si le score
 * pondéré est élevé (carte A expirée = règle dure).
 */
public record Regroupement10bisBeResponse(
        UUID caseFileId,
        Regroupement10bisBeLienFamilialEnum lienFamilial,
        Regroupement10bisBeTypeCarteEnum typeCarteRegroupant,
        int revenusMensuelsNetsRegroupant,
        int dureeSejour,
        LocalDate dateFinCarteA,
        boolean conditionTitreEnCours,
        boolean logementConforme,
        boolean assuranceMaladie,
        boolean menaceOrdrePublic,
        int seuilRessources,
        int differentielRevenus,
        int scoreEligibilite,
        Regroupement10bisBeVerdict verdict,
        List<String> criteresNonRemplis,
        String baseJuridique
) {}
