package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-16 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/teletravail-be-cct-85-149}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les
 * autres outils décisionnels BE de la F-219 (miroir
 * {@link InterimBeCct322Response} SF-219-14).</p>
 */
public record TeletravailBeCct85149Response(
        UUID caseFileId,

        // Inputs (snapshot)
        LocalDate dateDebutTeletravail,
        TeletravailBeCct85149TypeTeletravail typeTeletravail,
        boolean volontariatReciproque,
        boolean conventionEcriteIndividuelle,
        boolean equipementFourniOuIndemnise,
        BigDecimal indemniteForfaitaireMensuelleEuros,
        BigDecimal plafondIndemniteMensuelleEuros,
        boolean droitsSociauxMaintenus,
        boolean deconnexionDefinie,
        int effectifEntreprise,

        // Outputs
        TeletravailBeCct85149Verdict verdict,
        boolean conventionRespectee,
        boolean equipementRespecte,
        boolean droitsRespectes,
        boolean deconnexionRespectee,
        BigDecimal indemniteExcedentaire,

        // Synthèse + métadonnées légales
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
