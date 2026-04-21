export type ExtractionStatus = 'PENDING' | 'PROCESSING' | 'DONE' | 'FAILED';

export type ExtractionFailureReason =
  | 'EMPTY_TEXT'
  | 'UNSUPPORTED_FORMAT'
  | 'CORRUPTED'
  | 'EXTRACTION_EXCEPTION'
  | 'OCR_FAILED'
  | 'OCR_UNSUPPORTED_SIZE'
  | 'OCR_QUOTA_EXCEEDED';

export type DocumentPieceType =
  | 'CONTRAT'
  | 'PIECE_IDENTITE'
  | 'SMS'
  | 'EMAIL'
  | 'ATTESTATION'
  | 'BULLETIN_PAIE'
  | 'LETTRE'
  | 'PHOTO'
  | 'AUTRE';

/** SF-145-01 : résumé d'une pièce identifiée dans un document composite. */
export interface DocumentPieceSummary {
  id: string;
  type: DocumentPieceType;
  label: string | null;
  pageStart: number;
  pageEnd: number;
  orderIndex: number;
}

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
  /** SF-144-01 : true pendant l'appel synchrone à Textract (feedback polling). */
  ocrRunning?: boolean;
  /** SF-144-01 : true si l'extraction a été produite via Textract (affiche chip `OCR`). */
  ocrExtracted?: boolean;
  /** SF-145-01 : pièces identifiées dans le document composite. */
  pieces?: DocumentPieceSummary[];
}

/** SF-145-02 : libellé court par type pour les chips + sidebar. */
export function documentPieceTypeLabel(type: DocumentPieceType): string {
  switch (type) {
    case 'CONTRAT':        return 'Contrat';
    case 'PIECE_IDENTITE': return 'Identité';
    case 'SMS':            return 'SMS';
    case 'EMAIL':          return 'Email';
    case 'ATTESTATION':    return 'Attestation';
    case 'BULLETIN_PAIE':  return 'Bulletin de paie';
    case 'LETTRE':         return 'Lettre';
    case 'PHOTO':          return 'Photo';
    case 'AUTRE':          return 'Pièce';
    default:               return 'Pièce';
  }
}

/** SF-145-02 : icône Material par type. */
export function documentPieceTypeIcon(type: DocumentPieceType): string {
  switch (type) {
    case 'CONTRAT':        return 'description';
    case 'PIECE_IDENTITE': return 'badge';
    case 'SMS':            return 'chat';
    case 'EMAIL':          return 'email';
    case 'ATTESTATION':    return 'edit_note';
    case 'BULLETIN_PAIE':  return 'receipt_long';
    case 'LETTRE':         return 'mail';
    case 'PHOTO':          return 'image';
    case 'AUTRE':          return 'description';
    default:               return 'description';
  }
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
