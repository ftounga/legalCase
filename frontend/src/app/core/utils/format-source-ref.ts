import { AnalysisItem } from '../models/case-analysis.model';
import { documentPieceTypeLabel } from '../models/document.model';

/**
 * F-146 SF-146-03 : formate la source d'un AnalysisItem en texte plat pour
 * les exports (DOCX, PDF) ou toute sortie statique non cliquable.
 *
 * <p>Priorité au `sourceRef` enrichi (doc·pièce·page) quand disponible,
 * fallback sur le `source` string legacy sinon. Ajoute l'extrait entre
 * guillemets s'il est présent.</p>
 *
 * @returns chaîne formatée (sans suffixe `[Source : ...]`) ou `null` si
 *          aucune information de source n'est disponible.
 */
export function formatSourceRef(item: Pick<AnalysisItem, 'source' | 'sourceRef' | 'extrait'>): string | null {
  const parts: string[] = [];

  if (item.sourceRef && item.sourceRef.documentName) {
    const ref = item.sourceRef;
    parts.push(ref.documentName!);

    const pieceText = buildPieceText(ref.pieceType, ref.pieceLabel);
    if (pieceText) parts.push(pieceText);

    const pagesText = buildPagesText(ref.pageStart, ref.pageEnd);
    if (pagesText) parts.push(pagesText);
  } else if (item.source && item.source.trim().length > 0) {
    parts.push(item.source);
  } else {
    return null;
  }

  let out = parts.join(' · ');
  if (item.extrait && item.extrait.trim().length > 0) {
    out += ` — « ${item.extrait} »`;
  }
  return out;
}

function buildPieceText(pieceType: string | null, pieceLabel: string | null): string | null {
  const hasLabel = !!pieceLabel && pieceLabel.trim().length > 0;
  if (pieceType) {
    const typeLabel = documentPieceTypeLabel(pieceType as any);
    return hasLabel ? `${typeLabel} « ${pieceLabel} »` : typeLabel;
  }
  if (hasLabel) return `« ${pieceLabel} »`;
  return null;
}

function buildPagesText(pageStart: number | null, pageEnd: number | null): string | null {
  if (pageStart == null) return null;
  if (pageEnd == null || pageEnd === pageStart) return `p. ${pageStart}`;
  return `p. ${pageStart}-${pageEnd}`;
}
