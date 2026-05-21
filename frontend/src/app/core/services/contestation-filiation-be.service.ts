import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ContestationFiliationBeRequest,
  ContestationFiliationBeResponse,
} from '../models/contestation-filiation-be.model';

/**
 * F-217 SF-217-19 : wrapper HttpClient pour l'outil décisionnel
 * "Contestation de filiation BE" (`contestation-filiation-be`).
 * Consomme l'API figée dans SF-217-18 (backend).
 */
@Injectable({ providedIn: 'root' })
export class ContestationFiliationBeService {
  constructor(private http: HttpClient) {}

  /** POST — analyse la recevabilité de l'action et persiste le résultat. */
  calculate(
    caseFileId: string,
    request: ContestationFiliationBeRequest,
  ): Observable<ContestationFiliationBeResponse> {
    return this.http.post<ContestationFiliationBeResponse>(
      `/api/v1/case-files/${caseFileId}/contestation-filiation-be`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(
    caseFileId: string,
  ): Observable<ContestationFiliationBeResponse> {
    return this.http.get<ContestationFiliationBeResponse>(
      `/api/v1/case-files/${caseFileId}/contestation-filiation-be`,
    );
  }
}
