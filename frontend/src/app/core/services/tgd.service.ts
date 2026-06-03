import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TgdRequest, TgdResponse } from '../models/tgd.model';

/**
 * SF-222-02 : wrapper HttpClient pour l'outil décisionnel "Téléphone Grave
 * Danger — éligibilité" (F-FA-TGD, FR — art. 41-3-1 CPP). Consomme l'API figée
 * backend.
 *
 * Endpoint unique :
 *   POST /api/v1/case-files/{caseFileId}/tgd-analysis
 *   GET  /api/v1/case-files/{caseFileId}/tgd-analysis
 */
@Injectable({ providedIn: 'root' })
export class TgdService {
  constructor(private http: HttpClient) {}

  /** POST — analyse et persiste l'éligibilité TGD. */
  calculate(caseFileId: string, request: TgdRequest): Observable<TgdResponse> {
    return this.http.post<TgdResponse>(
      `/api/v1/case-files/${caseFileId}/tgd-analysis`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<TgdResponse> {
    return this.http.get<TgdResponse>(
      `/api/v1/case-files/${caseFileId}/tgd-analysis`,
    );
  }
}
