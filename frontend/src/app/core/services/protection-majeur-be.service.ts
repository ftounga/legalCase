import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ProtectionMajeurBeRequest,
  ProtectionMajeurBeResponse,
} from '../models/protection-majeur-be.model';

/**
 * F-217 SF-217-15 : wrapper HttpClient pour l'outil décisionnel
 * "Protection du majeur (Belgique)" (`protection-majeur-be`).
 * Consomme l'API figée dans SF-217-14 (backend).
 */
@Injectable({ providedIn: 'root' })
export class ProtectionMajeurBeService {
  constructor(private http: HttpClient) {}

  /** POST — analyse la situation et persiste le résultat. */
  calculate(
    caseFileId: string,
    request: ProtectionMajeurBeRequest,
  ): Observable<ProtectionMajeurBeResponse> {
    return this.http.post<ProtectionMajeurBeResponse>(
      `/api/v1/case-files/${caseFileId}/protection-majeur-be`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<ProtectionMajeurBeResponse> {
    return this.http.get<ProtectionMajeurBeResponse>(
      `/api/v1/case-files/${caseFileId}/protection-majeur-be`,
    );
  }
}
