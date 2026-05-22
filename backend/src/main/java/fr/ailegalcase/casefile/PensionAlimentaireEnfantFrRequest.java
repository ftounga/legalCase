package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-216-03 : body POST /api/v1/case-files/{id}/pension-alimentaire-enfant-fr.
 * <p>
 * Restauration / extension F-FA-02 (DELETE migration 191 + wrapper SF-198-02
 * présentationnel). Outil single-country FRANCE — art. 371-2 Cciv + barème
 * indicatif Cour de cassation (2010, révisé).
 * </p>
 */
public record PensionAlimentaireEnfantFrRequest(
        Integer revenusNetsParent1Eur,
        Integer revenusNetsParent2Eur,
        Integer nombreEnfants,
        List<Integer> agesEnfants,
        ModeResidenceEnfantEnum modeResidence
) {}
