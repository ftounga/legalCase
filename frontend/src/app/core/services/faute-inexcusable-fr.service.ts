import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  FauteInexcusableFrRequest,
  FauteInexcusableFrResponse,
} from '../models/faute-inexcusable-fr.model';

/**
 * SF-212-10 : wrapper HttpClient pour l'outil décisionnel "Faute inexcusable
 * de l'employeur" (F-DT-91, FR — L. 452-1 à L. 452-5 CSS ; Cass. ass.
 * plén. 24/06/2005 ; L. 4121-1 CT). Consomme l'API figée dans SF-212-09
 * (backend).
 */
@Injectable({ providedIn: 'root' })
export class FauteInexcusableFrService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'évaluation de la faute inexcusable pour le dossier. */
  calculate(
    caseFileId: string,
    request: FauteInexcusableFrRequest,
  ): Observable<FauteInexcusableFrResponse> {
    return this.http.post<FauteInexcusableFrResponse>(
      `/api/v1/case-files/${caseFileId}/faute-inexcusable-employeur`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 204 No Content si jamais calculé. */
  get(caseFileId: string): Observable<FauteInexcusableFrResponse> {
    return this.http.get<FauteInexcusableFrResponse>(
      `/api/v1/case-files/${caseFileId}/faute-inexcusable-employeur`,
    );
  }
}
