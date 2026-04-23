package fr.ailegalcase.analysis;

/**
 * F-153 SF-153-01 : fourchette jurisprudentielle p25/p50/p75 pour situer une
 * valeur calculée (pension alimentaire, prestation compensatoire) vs ce qui
 * est habituellement accordé par les juges pour un profil similaire.
 *
 * @param p25       25e percentile observé
 * @param p50       médiane observée
 * @param p75       75e percentile observé
 * @param label     libellé pour l'UI (ex. "Fourchette observée JAF France")
 * @param sourceRef référence de la source des données
 */
public record JurisprudenceRange(
        int p25,
        int p50,
        int p75,
        String label,
        String sourceRef
) {}
