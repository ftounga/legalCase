import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CongeMaternitePaterniteRequest,
  CongeMaternitePaterniteResponse,
} from '../models/conge-maternite-paternite.model';

/**
 * SF-212-30 : wrapper HttpClient pour l'outil décisionnel "Congé maternité /
 * paternité — protection et indemnités"
 * (F-DT-77-conge-paternite-maternite, FR — L. 1225-1 à L. 1225-40 CT ;
 * L. 331-3 CSS ; loi du 16/03/2021).
 * Consomme l'API figée dans SF-212-29 (backend).
 */
@Injectable({ providedIn: 'root' })
export class CongeMaternitePaterniteService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse pour le dossier. */
  calculate(
    caseFileId: string,
    request: CongeMaternitePaterniteRequest,
  ): Observable<CongeMaternitePaterniteResponse> {
    return this.http.post<CongeMaternitePaterniteResponse>(
      `/api/v1/case-files/${caseFileId}/conge-maternite-paternite`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 204 No Content si jamais calculé. */
  get(caseFileId: string): Observable<CongeMaternitePaterniteResponse> {
    return this.http.get<CongeMaternitePaterniteResponse>(
      `/api/v1/case-files/${caseFileId}/conge-maternite-paternite`,
    );
  }
}
