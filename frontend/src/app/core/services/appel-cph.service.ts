import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AppelCphRequest, AppelCphResponse } from '../models/appel-cph.model';

/**
 * SF-218-02 : wrapper HttpClient pour l'outil décisionnel "Appel CPH devant la
 * Cour d'appel" (F-DT-86-appel-cph-cour-appel, FR — R. 1461-1 et s. CPC ;
 * art. 538 CPC — délai d'appel 1 mois). Consomme l'API figée dans SF-218-01
 * (backend).
 *
 * <p>F-218a — Procédure CPH avancée (P3 Travail FR).</p>
 */
@Injectable({ providedIn: 'root' })
export class AppelCphService {
  /** Tool id canonique de l'outil (TOOL_REGISTRY + KNOWN_FRONTEND_TOOL_IDS). */
  static readonly STANDALONE_TOOL_ID = 'F-DT-86-appel-cph-cour-appel';

  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse d'appel CPH pour le dossier. */
  calculate(
    caseFileId: string,
    request: AppelCphRequest,
  ): Observable<AppelCphResponse> {
    return this.http.post<AppelCphResponse>(
      `/api/v1/case-files/${caseFileId}/appel-cph-analysis`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<AppelCphResponse> {
    return this.http.get<AppelCphResponse>(
      `/api/v1/case-files/${caseFileId}/appel-cph-analysis`,
    );
  }
}
