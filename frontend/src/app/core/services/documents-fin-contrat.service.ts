import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DocumentsFinContratRequest,
  DocumentsFinContratResponse,
} from '../models/documents-fin-contrat.model';

/**
 * SF-DT-32-02 : wrapper HttpClient pour l'outil décisionnel
 * "Documents de fin de contrat" (F-DT-32, FR uniquement).
 *
 * Consomme l'API figée dans SF-DT-32-01 (backend, PR #595 mergée) :
 *   POST/GET /api/v1/case-files/{caseFileId}/documents-fin-contrat
 */
@Injectable({ providedIn: 'root' })
export class DocumentsFinContratService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: DocumentsFinContratRequest):
      Observable<DocumentsFinContratResponse> {
    return this.http.post<DocumentsFinContratResponse>(
      `/api/v1/case-files/${caseFileId}/documents-fin-contrat`, request);
  }

  get(caseFileId: string): Observable<DocumentsFinContratResponse> {
    return this.http.get<DocumentsFinContratResponse>(
      `/api/v1/case-files/${caseFileId}/documents-fin-contrat`);
  }
}
