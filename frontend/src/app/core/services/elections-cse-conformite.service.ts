import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ElectionsCseConformiteRequest,
  ElectionsCseConformiteResponse,
} from '../models/elections-cse-conformite.model';

/**
 * SF-212-32 : wrapper HttpClient pour l'outil décisionnel "Élections CSE
 * — conformité procédure" (F-DT-65, FR — L. 2314-1 à L. 2314-37 CT ;
 * ordonnance n°2017-1386 du 22/09/2017). Consomme l'API figée dans
 * SF-212-31 (backend).
 */
@Injectable({ providedIn: 'root' })
export class ElectionsCseConformiteService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse de conformité pour le dossier. */
  calculate(
    caseFileId: string,
    request: ElectionsCseConformiteRequest,
  ): Observable<ElectionsCseConformiteResponse> {
    return this.http.post<ElectionsCseConformiteResponse>(
      `/api/v1/case-files/${caseFileId}/elections-cse-conformite`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 204 No Content si jamais calculé. */
  get(caseFileId: string): Observable<ElectionsCseConformiteResponse> {
    return this.http.get<ElectionsCseConformiteResponse>(
      `/api/v1/case-files/${caseFileId}/elections-cse-conformite`,
    );
  }
}
