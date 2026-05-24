import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  TeletravailAccordRequest,
  TeletravailAccordResponse,
} from '../models/teletravail-accord.model';

/**
 * SF-212-16 : wrapper HttpClient pour l'outil décisionnel "Télétravail —
 * conformité et litige" (F-DT-82-teletravail-accord, FR — L. 1222-9 à
 * L. 1222-11 CT ; ANI télétravail 26/11/2020). Consomme l'API figée dans
 * SF-212-15 (backend).
 */
@Injectable({ providedIn: 'root' })
export class TeletravailAccordService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse pour le dossier. */
  calculate(
    caseFileId: string,
    request: TeletravailAccordRequest,
  ): Observable<TeletravailAccordResponse> {
    return this.http.post<TeletravailAccordResponse>(
      `/api/v1/case-files/${caseFileId}/teletravail-accord`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 204 No Content si jamais calculé. */
  get(caseFileId: string): Observable<TeletravailAccordResponse> {
    return this.http.get<TeletravailAccordResponse>(
      `/api/v1/case-files/${caseFileId}/teletravail-accord`,
    );
  }
}
