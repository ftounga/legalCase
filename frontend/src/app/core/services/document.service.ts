import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpHeaders, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Document } from '../models/document.model';
import { DocumentPreview } from '../models/document-preview.model';

/** SF-231-02 : options additionnelles pour l'upload (durée vidéo en secondes). */
export interface UploadOptions {
  /** Durée vidéo arrondie en secondes (header `X-Video-Duration-Seconds`), uniquement pour MP4/MOV. */
  videoDurationSeconds?: number;
}

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private apiUrl(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/documents`;
  }

  constructor(private http: HttpClient) {}

  list(caseFileId: string): Observable<Document[]> {
    return this.http.get<Document[]>(this.apiUrl(caseFileId));
  }

  upload(caseFileId: string, file: File, ocrFormsMode = false, ocrEnabled = true, options: UploadOptions = {}): Observable<Document> {
    const formData = new FormData();
    formData.append('file', file);
    if (ocrFormsMode) formData.append('ocrFormsMode', 'true');
    if (!ocrEnabled) formData.append('ocrEnabled', 'false');
    let headers = new HttpHeaders();
    if (options.videoDurationSeconds !== undefined && options.videoDurationSeconds !== null) {
      headers = headers.set('X-Video-Duration-Seconds', String(options.videoDurationSeconds));
    }
    return this.http.post<Document>(this.apiUrl(caseFileId), formData, { headers });
  }

  uploadWithProgress(caseFileId: string, file: File, ocrFormsMode = false, ocrEnabled = true, options: UploadOptions = {}): Observable<HttpEvent<Document>> {
    const formData = new FormData();
    formData.append('file', file);
    if (ocrFormsMode) formData.append('ocrFormsMode', 'true');
    if (!ocrEnabled) formData.append('ocrEnabled', 'false');
    let headers = new HttpHeaders();
    if (options.videoDurationSeconds !== undefined && options.videoDurationSeconds !== null) {
      headers = headers.set('X-Video-Duration-Seconds', String(options.videoDurationSeconds));
    }
    const req = new HttpRequest('POST', this.apiUrl(caseFileId), formData, { reportProgress: true, headers });
    return this.http.request<Document>(req);
  }

  delete(caseFileId: string, documentId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl(caseFileId)}/${documentId}`);
  }

  downloadUrl(caseFileId: string, documentId: string): string {
    return `${this.apiUrl(caseFileId)}/${documentId}/download`;
  }

  preview(caseFileId: string, documentId: string): Observable<DocumentPreview> {
    return this.http.get<DocumentPreview>(`${this.apiUrl(caseFileId)}/${documentId}/preview`);
  }

  /** SF-145-11 : reclassification manuelle d'une pièce (type + label). */
  updatePiece(caseFileId: string, documentId: string, pieceId: string,
              type: string, label: string | null): Observable<import('../models/document.model').DocumentPieceSummary> {
    return this.http.put<import('../models/document.model').DocumentPieceSummary>(
      `${this.apiUrl(caseFileId)}/${documentId}/pieces/${pieceId}`,
      { type, label }
    );
  }

  /** SF-149-01 : édition manuelle de l'extrait OCR. */
  editExtraction(caseFileId: string, documentId: string, extractedText: string): Observable<void> {
    return this.http.put<void>(
      `${this.apiUrl(caseFileId)}/${documentId}/extraction`,
      { extractedText }
    );
  }

  /** SF-149-01 : réinitialise l'extrait à la version OCR d'origine. */
  resetExtraction(caseFileId: string, documentId: string): Observable<void> {
    return this.http.post<void>(
      `${this.apiUrl(caseFileId)}/${documentId}/extraction/reset`,
      {}
    );
  }
}
