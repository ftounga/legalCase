import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AbandonPostePresomptionDemissionRequest,
  AbandonPostePresomptionDemissionResponse,
} from '../models/abandon-poste-presomption-demission.model';

/**
 * SF-206-02 : wrapper HttpClient pour l'outil décisionnel
 * "Abandon de poste / présomption de démission" (F-DT-42, FR — solidité de
 * la contestation de la présomption de démission, art. L.1237-1-1 CT).
 * Consomme l'API figée dans SF-206-01 (backend).
 */
@Injectable({ providedIn: 'root' })
export class AbandonPostePresomptionDemissionService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse de la contestation pour le dossier. */
  calculate(
    caseFileId: string,
    request: AbandonPostePresomptionDemissionRequest,
  ): Observable<AbandonPostePresomptionDemissionResponse> {
    return this.http.post<AbandonPostePresomptionDemissionResponse>(
      `/api/v1/case-files/${caseFileId}/abandon-poste-presomption-demission`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<AbandonPostePresomptionDemissionResponse> {
    return this.http.get<AbandonPostePresomptionDemissionResponse>(
      `/api/v1/case-files/${caseFileId}/abandon-poste-presomption-demission`,
    );
  }
}
