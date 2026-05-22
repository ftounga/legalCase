import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AripaRecouvrementFrRequest,
  AripaRecouvrementFrResponse,
} from '../models/aripa-recouvrement-fr.model';

/**
 * SF-216-08 : wrapper HttpClient pour l'outil décisionnel "ARIPA recouvrement
 * pension alimentaire impayée" (F-FA-ARIPA-RECOUVREMENT, FR — art. L. 581 CSS).
 * Consomme l'API figée dans SF-216-07 (backend).
 *
 * Endpoint unique :
 *   POST /api/v1/case-files/{caseFileId}/aripa-recouvrement-fr
 *   GET  /api/v1/case-files/{caseFileId}/aripa-recouvrement-fr
 */
@Injectable({ providedIn: 'root' })
export class AripaRecouvrementFrService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse ARIPA recouvrement. */
  calculate(
    caseFileId: string,
    request: AripaRecouvrementFrRequest,
  ): Observable<AripaRecouvrementFrResponse> {
    return this.http.post<AripaRecouvrementFrResponse>(
      `/api/v1/case-files/${caseFileId}/aripa-recouvrement-fr`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<AripaRecouvrementFrResponse> {
    return this.http.get<AripaRecouvrementFrResponse>(
      `/api/v1/case-files/${caseFileId}/aripa-recouvrement-fr`,
    );
  }
}
