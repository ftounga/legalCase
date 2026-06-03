package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-223-06 : input du moteur décisionnel BE qualifiant le régime de séparation
 * de biens belge et ses variantes (Livre 3 CC ; loi du 22/07/2018).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis ici.
 * {@code varianteRegime} est requise (validation portée par le Calculator →
 * 400). Les autres champs sont nullables (informatifs / pré-remplissables) ;
 * {@code clauseParticipationPrevue} est requise quand la variante est
 * {@code SEPARATION_AVEC_PARTICIPATION_ACQUETS}.</p>
 */
public record RegimeBeSeparationBiensInput(
        RegimeBeSeparationBiensCalculator.VarianteRegime varianteRegime,
        Boolean contratMariageNotarie,
        Boolean clauseParticipationPrevue,
        Boolean disproportionPatrimonialeAllegee,
        LocalDate dateContrat,
        Double patrimoinePropreEpoux1Eur,
        Double patrimoinePropreEpoux2Eur
) {
    public RegimeBeSeparationBiensInput {
        contratMariageNotarie = Boolean.TRUE.equals(contratMariageNotarie);
        disproportionPatrimonialeAllegee = Boolean.TRUE.equals(disproportionPatrimonialeAllegee);
    }
}
