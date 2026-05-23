import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  IndigniteSuccessoraleRequest,
  IndigniteSuccessoraleResponse,
} from '../models/indignite-successorale-fr.model';

/**
 * SF-216-20 : wrapper HttpClient pour l'outil décisionnel "Indignité
 * successorale" (F-FA-INDIGNITE-SUCCESSORALE, FR — art. 726-729-1 Cciv).
 * Consomme l'API figée dans SF-216-19.
 *
 * Endpoints :
 *   POST /api/v1/case-files/{caseFileId}/indignite-successorale
 *   GET  /api/v1/case-files/{caseFileId}/indignite-successorale
 */
@Injectable({ providedIn: 'root' })
export class IndigniteSuccessoraleFrService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse Indignité successorale. */
  calculate(
    caseFileId: string,
    request: IndigniteSuccessoraleRequest,
  ): Observable<IndigniteSuccessoraleResponse> {
    return this.http.post<IndigniteSuccessoraleResponse>(
      `/api/v1/case-files/${caseFileId}/indignite-successorale`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<IndigniteSuccessoraleResponse> {
    return this.http.get<IndigniteSuccessoraleResponse>(
      `/api/v1/case-files/${caseFileId}/indignite-successorale`,
    );
  }
}
