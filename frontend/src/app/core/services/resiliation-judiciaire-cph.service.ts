import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ResiliationJudiciaireCphRequest,
  ResiliationJudiciaireCphResponse,
} from '../models/resiliation-judiciaire-cph.model';

/**
 * SF-206-08 : wrapper HttpClient pour l'outil décisionnel "Résiliation
 * judiciaire du contrat de travail aux torts de l'employeur" (F-DT-40, FR —
 * Cass. soc. 16/03/1989 ; Cass. soc. 20/01/1998 ; art. L.1411-1 CT). Consomme
 * l'API figée dans SF-206-07 (backend).
 */
@Injectable({ providedIn: 'root' })
export class ResiliationJudiciaireCphService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse de résiliation judiciaire pour le dossier. */
  calculate(
    caseFileId: string,
    request: ResiliationJudiciaireCphRequest,
  ): Observable<ResiliationJudiciaireCphResponse> {
    return this.http.post<ResiliationJudiciaireCphResponse>(
      `/api/v1/case-files/${caseFileId}/resiliation-judiciaire-cph`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<ResiliationJudiciaireCphResponse> {
    return this.http.get<ResiliationJudiciaireCphResponse>(
      `/api/v1/case-files/${caseFileId}/resiliation-judiciaire-cph`,
    );
  }
}
