import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  HarcelementNulliteRequest,
  HarcelementNulliteResponse,
} from '../models/harcelement-nullite.model';

/**
 * SF-DT-11-02 : wrapper HttpClient pour l'outil décisionnel
 * "Indemnité minimum licenciement nul — harcèlement" (F-DT-11).
 * Consomme l'API figée dans SF-DT-11-01 (backend).
 */
@Injectable({ providedIn: 'root' })
export class HarcelementNulliteService {
  /** F-163 SF-163-02b — `toolId` du dispatcher backend. */
  static readonly STANDALONE_TOOL_ID = 'F-DT-11-harcelement-licenciement-nul';

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: HarcelementNulliteRequest):
      Observable<HarcelementNulliteResponse> {
    return this.http.post<HarcelementNulliteResponse>(
      `/api/v1/case-files/${caseFileId}/harcelement-licenciement-nul`, request);
  }

  get(caseFileId: string): Observable<HarcelementNulliteResponse> {
    return this.http.get<HarcelementNulliteResponse>(
      `/api/v1/case-files/${caseFileId}/harcelement-licenciement-nul`);
  }

  /** F-163 SF-163-02b — POST sur le dispatcher générique des simulateurs. */
  calculateStandalone(request: HarcelementNulliteRequest):
      Observable<HarcelementNulliteResponse> {
    return this.http.post<HarcelementNulliteResponse>(
      `/api/v1/simulators/${HarcelementNulliteService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
