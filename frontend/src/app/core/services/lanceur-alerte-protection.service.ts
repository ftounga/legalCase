import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  LanceurAlerteProtectionRequest,
  LanceurAlerteProtectionResponse,
} from '../models/lanceur-alerte-protection.model';

/**
 * SF-212-26 : wrapper HttpClient pour l'outil décisionnel "Protection du
 * lanceur d'alerte" (F-DT-61, FR — L. 1132-3-3 CT ; loi Sapin II ; loi
 * Waserman 2022). Consomme l'API figée dans SF-212-25 (backend).
 */
@Injectable({ providedIn: 'root' })
export class LanceurAlerteProtectionService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'évaluation de la protection pour le dossier. */
  calculate(
    caseFileId: string,
    request: LanceurAlerteProtectionRequest,
  ): Observable<LanceurAlerteProtectionResponse> {
    return this.http.post<LanceurAlerteProtectionResponse>(
      `/api/v1/case-files/${caseFileId}/lanceur-alerte-protection`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 204 No Content si jamais calculé. */
  get(caseFileId: string): Observable<LanceurAlerteProtectionResponse> {
    return this.http.get<LanceurAlerteProtectionResponse>(
      `/api/v1/case-files/${caseFileId}/lanceur-alerte-protection`,
    );
  }
}
