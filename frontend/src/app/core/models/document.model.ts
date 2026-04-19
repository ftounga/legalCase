export type ExtractionStatus = 'PENDING' | 'PROCESSING' | 'DONE' | 'FAILED';

export type ExtractionFailureReason =
  | 'EMPTY_TEXT'
  | 'UNSUPPORTED_FORMAT'
  | 'CORRUPTED'
  | 'EXTRACTION_EXCEPTION'
  | 'OCR_FAILED'
  | 'OCR_UNSUPPORTED_SIZE'
  | 'OCR_QUOTA_EXCEEDED';

export interface Document {
  id: string;
  caseFileId: string;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  createdAt: string;
  /** SF-121-01 : statut de l'extraction si elle existe (null sinon). */
  extractionStatus?: ExtractionStatus | null;
  /** SF-121-01 : motif d'échec si extractionStatus === 'FAILED'. */
  failureReason?: ExtractionFailureReason | null;
}

/** SF-121-02 + SF-122-01 : libellé humain court pour le badge UI. */
export function extractionFailureLabel(reason: ExtractionFailureReason | null | undefined): string {
  switch (reason) {
    case 'EMPTY_TEXT':
      return 'Document illisible (scan sans texte ou image non-textuelle)';
    case 'UNSUPPORTED_FORMAT':
      return 'Format non pris en charge';
    case 'CORRUPTED':
      return 'Document corrompu';
    case 'EXTRACTION_EXCEPTION':
      return 'Erreur technique';
    case 'OCR_FAILED':
      return 'Reconnaissance OCR impossible — réessayer ou envoyer un autre document';
    case 'OCR_UNSUPPORTED_SIZE':
      return 'Document trop volumineux pour l\'OCR (max 5 Mo / 11 pages)';
    case 'OCR_QUOTA_EXCEEDED':
      return 'Quota OCR atteint — voir votre espace Abonnement pour acheter des pages supplémentaires';
    default:
      return 'Extraction impossible';
  }
}
