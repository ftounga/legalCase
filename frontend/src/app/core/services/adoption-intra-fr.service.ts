import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AdoptionIntraFrRequest,
  AdoptionIntraFrResponse,
} from '../models/adoption-intra-fr.model';

/**
 * SF-216-16 : wrapper HttpClient pour l'outil décisionnel "Adoption de
 * l'enfant du conjoint (adoption intra-familiale)" (F-FA-ADOPTION-INTRA,
 * FR — art. 345-1 Cciv). Consomme l'API figée dans SF-216-15 (backend).
 *
 * Endpoint unique :
 *   POST /api/v1/case-files/{caseFileId}/adoption-intra-fr
 *   GET  /api/v1/case-files/{caseFileId}/adoption-intra-fr
 */
@Injectable({ providedIn: 'root' })
export class AdoptionIntraFrService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse Adoption intra-familiale. */
  calculate(
    caseFileId: string,
    request: AdoptionIntraFrRequest,
  ): Observable<AdoptionIntraFrResponse> {
    return this.http.post<AdoptionIntraFrResponse>(
      `/api/v1/case-files/${caseFileId}/adoption-intra-fr`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<AdoptionIntraFrResponse> {
    return this.http.get<AdoptionIntraFrResponse>(
      `/api/v1/case-files/${caseFileId}/adoption-intra-fr`,
    );
  }
}
