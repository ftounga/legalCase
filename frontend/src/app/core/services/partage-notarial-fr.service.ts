import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PartageNotarialRequest,
  PartageNotarialResponse,
} from '../models/partage-notarial-fr.model';

/**
 * SF-216-28 : wrapper HttpClient pour l'outil décisionnel "Partage
 * successoral notarié" (F-FA-PARTAGE-NOTARIAL, FR — art. 816 et s. Cciv).
 * Consomme l'API figée dans SF-216-27.
 *
 * Endpoints :
 *   POST /api/v1/case-files/{caseFileId}/partage-notarial
 *   GET  /api/v1/case-files/{caseFileId}/partage-notarial
 */
@Injectable({ providedIn: 'root' })
export class PartageNotarialFrService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse Partage successoral notarié. */
  calculate(
    caseFileId: string,
    request: PartageNotarialRequest,
  ): Observable<PartageNotarialResponse> {
    return this.http.post<PartageNotarialResponse>(
      `/api/v1/case-files/${caseFileId}/partage-notarial`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<PartageNotarialResponse> {
    return this.http.get<PartageNotarialResponse>(
      `/api/v1/case-files/${caseFileId}/partage-notarial`,
    );
  }
}
