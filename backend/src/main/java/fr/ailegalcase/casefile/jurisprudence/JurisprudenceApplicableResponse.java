package fr.ailegalcase.casefile.jurisprudence;

import java.util.List;

/**
 * F-JU-02 / SF-JU-02-02 — réponse de
 * {@code GET /api/v1/case-files/{id}/jurisprudence-applicable}.
 *
 * <p>Liste agrégée des arrêts mappés (F-JU-01) des outils décisionnels
 * effectivement utilisés sur le dossier — consommée par le frontend
 * {@code pdf-export.service.ts} pour la section « 📚 Jurisprudence
 * applicable » du PDF synthèse exporté (pattern miroir SF-192/195/196 PDF).</p>
 *
 * <p>{@code entries} peut être vide (cas nominal V1 : aucun
 * {@code ToolUsageContributor} n'est encore implémenté par les outils,
 * cf. SF-JU-02-01 hors scope). Dans ce cas le frontend omet la section
 * (fail-open).</p>
 */
public record JurisprudenceApplicableResponse(List<JurisprudenceApplicableEntry> entries) {
}
