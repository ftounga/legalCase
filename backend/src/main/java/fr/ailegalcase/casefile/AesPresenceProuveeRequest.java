package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-214-11 : requête POST pour le calcul de présence prouvée en France et
 * l'éligibilité aux 4 voies AES (L. 435-1 / L. 435-3 CESEDA). Outil single-country FR.
 *
 * @param periodesPresentees liste non vide des périodes de présence justifiées
 *                           par une pièce admissible (circulaire Valls 28/11/2012)
 */
public record AesPresenceProuveeRequest(
        List<PeriodePresentee> periodesPresentees
) {

    /**
     * Une période de présence justifiée par une pièce.
     *
     * @param debut     date de début de la période (incluse, non future)
     * @param fin       date de fin de la période (incluse, ≥ debut)
     * @param typePiece type de pièce justificative (whitelist {@link AesPieceType})
     */
    public record PeriodePresentee(
            LocalDate debut,
            LocalDate fin,
            AesPieceType typePiece
    ) {}
}
