import { CaseAnalysisPartialSections } from '../../core/models/case-analysis.model';

/**
 * F-190 SF-190-01 — descripteur d'une section attendue dans le streaming.
 * `id` correspond à la clé snake_case côté backend ; `anchor` cible le panel
 * de la pile inférieure pour le scroll-to-block (utilisé sur la page synthèse).
 *
 * F-190 SF-190-03 — extrait dans ce fichier partagé pour réutilisation par la
 * page détail du dossier (`case-file-detail`) qui affiche désormais aussi le
 * compteur "X/7 sections reçues" dans son bandeau de progression.
 */
export interface StreamingSection {
  id: keyof CaseAnalysisPartialSections;
  label: string;
  anchor: string | null;
}

/**
 * Liste fixe des 7 sections visibles dans la synthèse.
 * Les champs `*_extracted_data` ne sont pas comptés (consommés par F-IA-04,
 * pas affichés dans la synthèse).
 */
export const STREAMING_EXPECTED_SECTIONS: readonly StreamingSection[] = [
  { id: 'timeline',           label: 'Chronologie',       anchor: 'section-timeline' },
  { id: 'faits',              label: 'Faits',             anchor: 'section-faits' },
  { id: 'points_juridiques',  label: 'Points juridiques', anchor: 'section-points-juridiques' },
  { id: 'risques',            label: 'Risques',           anchor: 'section-risques' },
  { id: 'questions_ouvertes', label: 'Questions ouvertes', anchor: 'section-questions-ouvertes' },
  { id: 'pieces_manquantes',  label: 'Pièces manquantes', anchor: 'section-pieces' },
  { id: 'risk_level',         label: 'Niveau de risque',  anchor: null },
] as const;
