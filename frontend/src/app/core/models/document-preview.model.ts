export type ExtractionMethod = 'CLASSIC' | 'OCR' | 'NONE';
export type ExtractionStatus = 'PENDING' | 'PROCESSING' | 'DONE' | 'FAILED';

/** SF-127-01 : payload renvoyé par GET /documents/{id}/preview */
export interface DocumentPreview {
  fileName: string;
  mimeType: string;
  fileSize: number;
  pageCount: number | null;
  uploadedAt: string;
  extractionStatus: ExtractionStatus;
  extractionMethod: ExtractionMethod;
  extractedText: string | null;
  charCount: number;
  textTruncated: boolean;
  ocrPagesUsed: number;
  failureReason: string | null;
  /** SF-149-01 : timestamp ISO de la dernière édition manuelle, null si jamais édité */
  textEditedAt: string | null;
  /**
   * SF-231-02 : description visuelle Legal Vision (multi-frames pour vidéos),
   * affichée au-dessus du player. Pattern miroir de
   * `DocumentPieceSummary.visualDescription` (SF-148-01) pour permettre la
   * description "vidéo entière" sans pièce associée. Null si pas de vidéo
   * ou Vision pas encore exécutée.
   */
  visualDescription?: string | null;
  /**
   * SF-231-02 : URLs signées S3 des 5 frames extraites par ffmpeg (10/30/50/70/90 %).
   * Tableau vide ou non défini si extraction en cours / non vidéo.
   * Chemin canonique backend : `case-files/{id}/documents/{docId}/frames/frame-{n}.png`.
   */
  frameUrls?: string[] | null;
}
