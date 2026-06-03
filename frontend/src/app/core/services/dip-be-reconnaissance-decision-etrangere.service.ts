import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DipBeReconnaissanceDecisionEtrangereRequest,
  DipBeReconnaissanceDecisionEtrangereResponse,
} from '../models/dip-be-reconnaissance-decision-etrangere.model';

/**
 * SF-223-08 : wrapper HttpClient pour l'outil décisionnel "Reconnaissance /
 * exequatur d'une décision familiale étrangère (Belgique — DIP)"
 * (`dip-be-reconnaissance-decision-etrangere`). Consomme l'API figée dans
 * SF-223-08 (backend).
 */
@Injectable({ providedIn: 'root' })
export class DipBeReconnaissanceDecisionEtrangereService {
  constructor(private http: HttpClient) {}

  /** POST — qualifie la reconnaissance / exequatur et persiste le résultat. */
  calculate(
    caseFileId: string,
    request: DipBeReconnaissanceDecisionEtrangereRequest,
  ): Observable<DipBeReconnaissanceDecisionEtrangereResponse> {
    return this.http.post<DipBeReconnaissanceDecisionEtrangereResponse>(
      `/api/v1/case-files/${caseFileId}/dip-be-reconnaissance-decision-etrangere-analysis`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<DipBeReconnaissanceDecisionEtrangereResponse> {
    return this.http.get<DipBeReconnaissanceDecisionEtrangereResponse>(
      `/api/v1/case-files/${caseFileId}/dip-be-reconnaissance-decision-etrangere-analysis`,
    );
  }
}
