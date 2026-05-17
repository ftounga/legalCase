import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ProcedureNulliteLicenciementRequest,
  ProcedureNulliteLicenciementResponse,
} from '../models/procedure-nullite-licenciement.model';

/**
 * SF-DT-36-02 : wrapper HttpClient pour l'outil décisionnel
 * "Nullité de procédure de licenciement" (F-DT-36, vices de forme côté
 * employeur). Consomme l'API figée dans SF-DT-36-01 (backend).
 */
@Injectable({ providedIn: 'root' })
export class ProcedureNulliteLicenciementService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse de nullité du dossier. */
  calculate(
    caseFileId: string,
    request: ProcedureNulliteLicenciementRequest,
  ): Observable<ProcedureNulliteLicenciementResponse> {
    return this.http.post<ProcedureNulliteLicenciementResponse>(
      `/api/v1/case-files/${caseFileId}/procedure-nullite-licenciement`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<ProcedureNulliteLicenciementResponse> {
    return this.http.get<ProcedureNulliteLicenciementResponse>(
      `/api/v1/case-files/${caseFileId}/procedure-nullite-licenciement`,
    );
  }
}
