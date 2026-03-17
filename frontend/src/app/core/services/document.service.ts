import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Document } from '../models/document.model';

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private apiUrl(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/documents`;
  }

  constructor(private http: HttpClient) {}

  list(caseFileId: string): Observable<Document[]> {
    return this.http.get<Document[]>(this.apiUrl(caseFileId));
  }

  upload(caseFileId: string, file: File): Observable<Document> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Document>(this.apiUrl(caseFileId), formData);
  }
}
