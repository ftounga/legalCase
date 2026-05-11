import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RequalificationCddCdiRequest,
  RequalificationCddCdiResponse,
} from '../models/requalification-cdd-cdi.model';

/**
 * SF-DT-22-02 : wrapper HttpClient pour l'outil décisionnel
 * "Requalification CDD → CDI" (F-DT-22). Consomme l'API figée dans
 * SF-DT-22-01 (backend).
 */
@Injectable({ providedIn: 'root' })
export class RequalificationCddCdiService {
  /** F-163 SF-163-02b — `toolId` du dispatcher backend. */
  static readonly STANDALONE_TOOL_ID = 'F-DT-22-requalification-cdd-cdi';

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: RequalificationCddCdiRequest):
      Observable<RequalificationCddCdiResponse> {
    return this.http.post<RequalificationCddCdiResponse>(
      `/api/v1/case-files/${caseFileId}/requalification-cdd-cdi`, request);
  }

  get(caseFileId: string): Observable<RequalificationCddCdiResponse> {
    return this.http.get<RequalificationCddCdiResponse>(
      `/api/v1/case-files/${caseFileId}/requalification-cdd-cdi`);
  }
  /** F-163 SF-163-02b — POST sur le dispatcher générique des simulateurs. */
  calculateStandalone(request: RequalificationCddCdiRequest): Observable<RequalificationCddCdiResponse> {
    return this.http.post<RequalificationCddCdiResponse>(
      `/api/v1/simulators/${RequalificationCddCdiService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
