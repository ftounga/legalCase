import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  NonConcurrenceRequest,
  NonConcurrenceResponse,
} from '../models/non-concurrence.model';

/**
 * SF-DT-24-02 : wrapper HttpClient pour l'outil décisionnel
 * "Clause de non-concurrence" (F-DT-24). Consomme l'API figée dans
 * SF-DT-24-01 (backend).
 */
@Injectable({ providedIn: 'root' })
export class NonConcurrenceService {
  /** F-163 SF-163-02b — `toolId` du dispatcher backend. */
  static readonly STANDALONE_TOOL_ID = 'F-DT-24-non-concurrence';

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: NonConcurrenceRequest):
      Observable<NonConcurrenceResponse> {
    return this.http.post<NonConcurrenceResponse>(
      `/api/v1/case-files/${caseFileId}/non-concurrence`, request);
  }

  get(caseFileId: string): Observable<NonConcurrenceResponse> {
    return this.http.get<NonConcurrenceResponse>(
      `/api/v1/case-files/${caseFileId}/non-concurrence`);
  }
  /** F-163 SF-163-02b — POST sur le dispatcher générique des simulateurs. */
  calculateStandalone(request: NonConcurrenceRequest): Observable<NonConcurrenceResponse> {
    return this.http.post<NonConcurrenceResponse>(
      `/api/v1/simulators/${NonConcurrenceService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
