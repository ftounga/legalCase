package fr.ailegalcase.casefile.jurisprudence;

import fr.ailegalcase.jurisprudencemapping.ToolJurisprudenceCitationResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * F-JU-02 / SF-JU-02-02 — DTO API d'un arrêt mappé pour la section
 * « Jurisprudence applicable » du PDF synthèse exporté côté frontend.
 *
 * <p>Miroir public de {@link ToolJurisprudenceCitationResponse} (record interne
 * du package {@code jurisprudencemapping}) — décorrélé pour stabiliser le
 * contrat API consommé par le frontend {@code pdf-export.service.ts} et pour
 * éviter de coupler ce dernier au record interne SF-JU-01.</p>
 *
 * @param id              identifiant interne du mapping
 * @param arretRef        référence formatée ex. {@code "Cass. soc. 8 janv. 2025, n° 23-12.345"}
 * @param juridiction     juridiction émettrice ex. {@code "Cour de cassation, chambre sociale"}
 * @param dateArret       date de l'arrêt
 * @param numeroPourvoi   numéro de pourvoi / décision
 * @param lienLegifrance  URL Légifrance / source publique
 * @param chapeauOfficiel chapeau officiel cité textuellement
 * @param lastVerifiedAt  instant de la dernière confirmation du mapping
 * @param confidenceScore score de confiance entre 0.00 et 1.00
 */
public record JurisprudenceCitationDto(
        UUID id,
        String arretRef,
        String juridiction,
        LocalDate dateArret,
        String numeroPourvoi,
        String lienLegifrance,
        String chapeauOfficiel,
        Instant lastVerifiedAt,
        BigDecimal confidenceScore) {

    /** Mappe le record interne SF-JU-01 vers le DTO API public. */
    public static JurisprudenceCitationDto from(ToolJurisprudenceCitationResponse src) {
        return new JurisprudenceCitationDto(
                src.id(),
                src.arretRef(),
                src.juridiction(),
                src.dateArret(),
                src.numeroPourvoi(),
                src.lienLegifrance(),
                src.chapeauOfficiel(),
                src.lastVerifiedAt(),
                src.confidenceScore());
    }
}
