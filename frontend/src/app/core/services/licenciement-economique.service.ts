import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  LicenciementEconomiqueRequest,
  LicenciementEconomiqueResponse,
} from '../models/licenciement-economique.model';

/**
 * SF-DT-13-02 : wrapper HttpClient pour l'outil décisionnel
 * "Licenciement économique" (F-DT-13, FR uniquement, art. L.1233-3/4/5/45
 * Code du travail). Consomme l'API figée dans SF-DT-13-01 (backend PR #585).
 */
@Injectable({ providedIn: 'root' })
export class LicenciementEconomiqueService {
  /** F-163 SF-163-02b — `toolId` du dispatcher backend. */
  static readonly STANDALONE_TOOL_ID = 'F-DT-13-licenciement-economique';

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: LicenciementEconomiqueRequest):
      Observable<LicenciementEconomiqueResponse> {
    return this.http.post<LicenciementEconomiqueResponse>(
      `/api/v1/case-files/${caseFileId}/licenciement-economique`, request);
  }

  get(caseFileId: string): Observable<LicenciementEconomiqueResponse> {
    return this.http.get<LicenciementEconomiqueResponse>(
      `/api/v1/case-files/${caseFileId}/licenciement-economique`);
  }
  /** F-163 SF-163-02b — POST sur le dispatcher générique des simulateurs. */
  calculateStandalone(request: LicenciementEconomiqueRequest): Observable<LicenciementEconomiqueResponse> {
    return this.http.post<LicenciementEconomiqueResponse>(
      `/api/v1/simulators/${LicenciementEconomiqueService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
