export type ExtractionStatus = 'PENDING' | 'PROCESSING' | 'DONE' | 'FAILED';

export type ExtractionFailureReason =
  | 'EMPTY_TEXT'
  | 'UNSUPPORTED_FORMAT'
  | 'CORRUPTED'
  | 'EXTRACTION_EXCEPTION';

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

/** SF-121-02 : libellé humain court pour le badge UI. */
export function extractionFailureLabel(reason: ExtractionFailureReason | null | undefined): string {
  switch (reason) {
    case 'EMPTY_TEXT':
      return 'Document illisible (probablement un scan sans OCR)';
    case 'UNSUPPORTED_FORMAT':
      return 'Format non pris en charge';
    case 'CORRUPTED':
      return 'Document corrompu';
    case 'EXTRACTION_EXCEPTION':
      return 'Erreur technique';
    default:
      return 'Extraction impossible';
  }
}
