import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RegimeBeSeparationBiensRequest,
  RegimeBeSeparationBiensResponse,
} from '../models/regime-be-separation-biens.model';

/**
 * SF-223-06 : wrapper HttpClient pour l'outil décisionnel "Régime de séparation
 * de biens (Belgique)" (`regime-be-separation-biens`). Consomme l'API figée
 * dans SF-223-06 (backend).
 */
@Injectable({ providedIn: 'root' })
export class RegimeBeSeparationBiensService {
  constructor(private http: HttpClient) {}

  /** POST — qualifie le régime de séparation de biens et persiste le résultat. */
  calculate(
    caseFileId: string,
    request: RegimeBeSeparationBiensRequest,
  ): Observable<RegimeBeSeparationBiensResponse> {
    return this.http.post<RegimeBeSeparationBiensResponse>(
      `/api/v1/case-files/${caseFileId}/regime-be-separation-biens-analysis`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<RegimeBeSeparationBiensResponse> {
    return this.http.get<RegimeBeSeparationBiensResponse>(
      `/api/v1/case-files/${caseFileId}/regime-be-separation-biens-analysis`,
    );
  }
}
