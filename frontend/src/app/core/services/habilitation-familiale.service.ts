import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  HabilitationFamilialeRequest,
  HabilitationFamilialeResponse,
} from '../models/habilitation-familiale.model';

/**
 * SF-222-03 : wrapper HttpClient pour l'outil décisionnel "Habilitation
 * familiale" (F-FA-HABILITATION-FAMILIALE, FR — art. 494-1 et s. Cciv). Consomme
 * l'API figée backend.
 *
 * Endpoint unique :
 *   POST /api/v1/case-files/{caseFileId}/habilitation-familiale-analysis
 *   GET  /api/v1/case-files/{caseFileId}/habilitation-familiale-analysis
 */
@Injectable({ providedIn: 'root' })
export class HabilitationFamilialeService {
  constructor(private http: HttpClient) {}

  /** POST — analyse et persiste les conditions de l'habilitation familiale. */
  calculate(
    caseFileId: string,
    request: HabilitationFamilialeRequest,
  ): Observable<HabilitationFamilialeResponse> {
    return this.http.post<HabilitationFamilialeResponse>(
      `/api/v1/case-files/${caseFileId}/habilitation-familiale-analysis`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<HabilitationFamilialeResponse> {
    return this.http.get<HabilitationFamilialeResponse>(
      `/api/v1/case-files/${caseFileId}/habilitation-familiale-analysis`,
    );
  }
}
