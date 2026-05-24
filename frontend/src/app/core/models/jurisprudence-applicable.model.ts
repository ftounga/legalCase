/**
 * F-JU-02 / SF-JU-02-02 — types miroir du DTO Java
 * {@code fr.ailegalcase.casefile.jurisprudence.JurisprudenceApplicableResponse}
 * exposé par {@code GET /api/v1/case-files/{id}/jurisprudence-applicable}.
 *
 * Consommé par {@code PdfExportService} pour la section
 * « 📚 Jurisprudence applicable » du PDF synthèse exporté.
 */

export interface JurisprudenceCitation {
  id: string;
  arretRef: string;
  juridiction: string;
  /** ISO local date `YYYY-MM-DD`. */
  dateArret: string;
  numeroPourvoi: string;
  lienLegifrance: string;
  chapeauOfficiel: string;
  /** ISO instant. */
  lastVerifiedAt: string;
  /** Score de confiance 0.00 à 1.00 (transmis en string par Jackson pour BigDecimal). */
  confidenceScore: number | string;
}

export interface JurisprudenceApplicableEntry {
  toolId: string;
  brancheCalculId: string;
  citations: JurisprudenceCitation[];
}

export interface JurisprudenceApplicableResponse {
  entries: JurisprudenceApplicableEntry[];
}
