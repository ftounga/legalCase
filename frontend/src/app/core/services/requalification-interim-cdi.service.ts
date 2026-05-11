import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RequalificationInterimCdiRequest,
  RequalificationInterimCdiResponse,
} from '../models/requalification-interim-cdi.model';

/**
 * SF-DT-23-02 : wrapper HttpClient pour l'outil décisionnel
 * "Requalification intérim → CDI" (F-DT-23). Consomme l'API figée dans
 * SF-DT-23-01 (backend).
 */
@Injectable({ providedIn: 'root' })
export class RequalificationInterimCdiService {
  /** F-163 SF-163-02b — `toolId` du dispatcher backend. */
  static readonly STANDALONE_TOOL_ID = 'F-DT-23-requalification-interim-cdi';

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: RequalificationInterimCdiRequest):
      Observable<RequalificationInterimCdiResponse> {
    return this.http.post<RequalificationInterimCdiResponse>(
      `/api/v1/case-files/${caseFileId}/requalification-interim-cdi`, request);
  }

  get(caseFileId: string): Observable<RequalificationInterimCdiResponse> {
    return this.http.get<RequalificationInterimCdiResponse>(
      `/api/v1/case-files/${caseFileId}/requalification-interim-cdi`);
  }
  /** F-163 SF-163-02b — POST sur le dispatcher générique des simulateurs. */
  calculateStandalone(request: RequalificationInterimCdiRequest): Observable<RequalificationInterimCdiResponse> {
    return this.http.post<RequalificationInterimCdiResponse>(
      `/api/v1/simulators/${RequalificationInterimCdiService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
