import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  EgaliteSalarialeFhRequest,
  EgaliteSalarialeFhResponse,
} from '../models/egalite-salariale-fh.model';

/**
 * SF-212-24 : wrapper HttpClient pour l'outil décisionnel "Égalité salariale
 * femmes/hommes" (F-DT-56-egalite-salariale-femmes-hommes, FR — L. 1142-7
 * à L. 1142-10 CT ; L. 1144-1 CT ; L. 3221-2 CT ; loi 05/09/2018).
 * Consomme l'API figée dans SF-212-23 (backend).
 */
@Injectable({ providedIn: 'root' })
export class EgaliteSalarialeFhService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse pour le dossier. */
  calculate(
    caseFileId: string,
    request: EgaliteSalarialeFhRequest,
  ): Observable<EgaliteSalarialeFhResponse> {
    return this.http.post<EgaliteSalarialeFhResponse>(
      `/api/v1/case-files/${caseFileId}/egalite-salariale-fh`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 204 No Content si jamais calculé. */
  get(caseFileId: string): Observable<EgaliteSalarialeFhResponse> {
    return this.http.get<EgaliteSalarialeFhResponse>(
      `/api/v1/case-files/${caseFileId}/egalite-salariale-fh`,
    );
  }
}
