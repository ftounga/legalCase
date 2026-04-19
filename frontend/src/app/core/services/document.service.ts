import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Document } from '../models/document.model';
import { DocumentPreview } from '../models/document-preview.model';

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private apiUrl(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/documents`;
  }

  constructor(private http: HttpClient) {}

  list(caseFileId: string): Observable<Document[]> {
    return this.http.get<Document[]>(this.apiUrl(caseFileId));
  }

  upload(caseFileId: string, file: File, ocrFormsMode = false, ocrEnabled = true): Observable<Document> {
    const formData = new FormData();
    formData.append('file', file);
    if (ocrFormsMode) formData.append('ocrFormsMode', 'true');
    if (!ocrEnabled) formData.append('ocrEnabled', 'false');
    return this.http.post<Document>(this.apiUrl(caseFileId), formData);
  }

  uploadWithProgress(caseFileId: string, file: File, ocrFormsMode = false, ocrEnabled = true): Observable<HttpEvent<Document>> {
    const formData = new FormData();
    formData.append('file', file);
    if (ocrFormsMode) formData.append('ocrFormsMode', 'true');
    if (!ocrEnabled) formData.append('ocrEnabled', 'false');
    const req = new HttpRequest('POST', this.apiUrl(caseFileId), formData, { reportProgress: true });
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
}
