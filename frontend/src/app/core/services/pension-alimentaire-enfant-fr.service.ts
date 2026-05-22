import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PensionAlimentaireEnfantFrRequest,
  PensionAlimentaireEnfantFrResponse,
} from '../models/pension-alimentaire-enfant-fr.model';

/**
 * SF-216-04 : wrapper HttpClient pour l'outil décisionnel "Pension alimentaire
 * enfant FR" (F-FA-02, FR — art. 371-2 Cciv). Consomme l'API figée dans SF-216-03.
 *
 * Endpoint unique :
 *   POST /api/v1/case-files/{caseFileId}/pension-alimentaire-enfant-fr
 *   GET  /api/v1/case-files/{caseFileId}/pension-alimentaire-enfant-fr
 */
@Injectable({ providedIn: 'root' })
export class PensionAlimentaireEnfantFrService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse de pension alimentaire enfant. */
  calculate(
    caseFileId: string,
    request: PensionAlimentaireEnfantFrRequest,
  ): Observable<PensionAlimentaireEnfantFrResponse> {
    return this.http.post<PensionAlimentaireEnfantFrResponse>(
      `/api/v1/case-files/${caseFileId}/pension-alimentaire-enfant-fr`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<PensionAlimentaireEnfantFrResponse> {
    return this.http.get<PensionAlimentaireEnfantFrResponse>(
      `/api/v1/case-files/${caseFileId}/pension-alimentaire-enfant-fr`,
    );
  }
}
