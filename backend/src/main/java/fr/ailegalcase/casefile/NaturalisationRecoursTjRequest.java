package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-214-29 : requête POST pour l'analyse du délai de recours devant le Tribunal
 * judiciaire contre un refus de déclaration de nationalité française (Cciv 26-3).
 * Outil single-country FR.
 */
public record NaturalisationRecoursTjRequest(
        NaturalisationRecoursTjVoieEnum voieNaturalisation,
        LocalDate dateRefusDeclaration,
        NaturalisationRecoursTjTypeRefusEnum typeRefus
) {}
