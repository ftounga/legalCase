package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-214-31 : requête POST pour l'analyse du délai de recours devant le Tribunal
 * administratif de Nantes contre un refus de naturalisation par décret
 * (CJA L. 213-1, délai 2 mois ; Cciv 21-15). Outil single-country FR.
 *
 * @param dateRefusDecret date de notification du refus de naturalisation par
 *        décret (requise).
 * @param motivationRefus extrait facultatif de la motivation du refus
 *        (≤ 500 caractères) — à titre de contexte, sans impact sur le calcul du délai.
 * @param recoursPrerequis indique si un recours préalable (gracieux / hiérarchique
 *        ministre) a été ou doit être exercé.
 */
public record NaturalisationRecoursTaNantesRequest(
        LocalDate dateRefusDecret,
        String motivationRefus,
        boolean recoursPrerequis
) {}
