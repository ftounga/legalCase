package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * F-185 SF-185-01 — réponse de l'endpoint
 * {@code GET /api/v1/case-files/{id}/case-analysis/partial}.
 *
 * <p>Retourne l'état partiel courant de l'analyse en cours de streaming :
 * les sections JSON top-level déjà détectées comme complètes par
 * {@link PartialJsonSectionExtractor}, sous forme de
 * {@link JsonNode} (objet racine {@code {"timeline": [...], "faits": [...], ...}}).
 *
 * <p>Le frontend peut décoder chaque section connue (faits, points_juridiques,
 * timeline, risques, etc.) et afficher uniquement celles arrivées, avec un
 * skeleton sur les sections non encore présentes.
 *
 * <p>Si {@code sections} est {@code null}, l'analyse n'a pas encore produit de
 * section complète (réponse 200 avec sections=null pour distinguer "en cours
 * mais rien encore" de "404 introuvable").
 *
 * <p>F-190 SF-190-02 — {@code analysisType} permet au frontend de savoir si
 * l'état partiel correspond à une analyse standard ou à une re-analyse enrichie
 * (les deux empruntent désormais le streaming SSE via le même endpoint).
 */
public record CaseAnalysisPartialResponse(
        UUID analysisId,
        int version,
        AnalysisType analysisType,
        AnalysisStatus status,
        JsonNode sections,
        Instant updatedAt
) {}
