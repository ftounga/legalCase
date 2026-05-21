package fr.ailegalcase.jurisprudencemapping;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * F-JU-01 / SF-JU-01-01 — représentation API d'un {@link ToolJurisprudenceMapping}
 * exposée par {@code GET /api/tools/&#123;toolId&#125;/jurisprudence-citations}.
 *
 * <p>Préfixe {@code Tool} explicite pour éviter toute confusion avec la classe
 * {@code JurisprudenceCitation} de F-242 (use case orthogonal — saisie manuelle
 * avocat per-point juridique d'un dossier, table {@code case_jurisprudence_citations},
 * isolation workspace).</p>
 *
 * @param id              identifiant interne du mapping
 * @param arretRef        référence formatée ex. {@code "Cass. soc. 8 janv. 2025, n° 23-12.345"}
 * @param juridiction     juridiction émettrice ex. {@code "Cour de cassation, chambre sociale"}
 * @param dateArret       date de l'arrêt
 * @param numeroPourvoi   numéro de pourvoi / décision
 * @param lienLegifrance  URL Légifrance / source publique
 * @param chapeauOfficiel chapeau officiel cité textuellement (pas de reformulation Claude)
 * @param lastVerifiedAt  instant de la dernière confirmation du mapping (cron mensuel)
 * @param confidenceScore score de confiance Claude entre 0.00 et 1.00
 */
public record ToolJurisprudenceCitationResponse(
        UUID id,
        String arretRef,
        String juridiction,
        LocalDate dateArret,
        String numeroPourvoi,
        String lienLegifrance,
        String chapeauOfficiel,
        Instant lastVerifiedAt,
        BigDecimal confidenceScore) {

    /** Mappe une entité vers sa représentation API. */
    public static ToolJurisprudenceCitationResponse from(ToolJurisprudenceMapping mapping) {
        return new ToolJurisprudenceCitationResponse(
                mapping.getId(),
                mapping.getArretRef(),
                mapping.getJuridiction(),
                mapping.getDateArret(),
                mapping.getNumeroPourvoi(),
                mapping.getLienLegifrance(),
                mapping.getChapeauOfficiel(),
                mapping.getLastVerifiedAt(),
                mapping.getConfidenceScore());
    }
}
