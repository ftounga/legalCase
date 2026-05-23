import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ForfaitJoursValiditeRequest,
  ForfaitJoursValiditeResponse,
} from '../models/forfait-jours-fr.model';

/**
 * SF-212-04 : wrapper HttpClient pour l'outil décisionnel "Forfait jours —
 * validité et rappel HS" (F-DT-50, FR — L. 3121-58 à L. 3121-66 CT,
 * Cass. soc. 29/06/2011 n°09-71.107). Consomme l'API figée dans SF-212-03
 * (backend).
 */
@Injectable({ providedIn: 'root' })
export class ForfaitJoursFrService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse de validité du forfait jours pour le dossier. */
  calculate(
    caseFileId: string,
    request: ForfaitJoursValiditeRequest,
  ): Observable<ForfaitJoursValiditeResponse> {
    return this.http.post<ForfaitJoursValiditeResponse>(
      `/api/v1/case-files/${caseFileId}/forfait-jours-validite`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 204 No Content si jamais calculé. */
  get(caseFileId: string): Observable<ForfaitJoursValiditeResponse> {
    return this.http.get<ForfaitJoursValiditeResponse>(
      `/api/v1/case-files/${caseFileId}/forfait-jours-validite`,
    );
  }
}
