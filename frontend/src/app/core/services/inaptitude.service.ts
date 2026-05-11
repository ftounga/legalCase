import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  InaptitudeRequest,
  InaptitudeResponse,
} from '../models/inaptitude.model';

/**
 * SF-DT-15-02 : wrapper HttpClient pour l'outil décisionnel
 * "Licenciement pour inaptitude" (F-DT-15). FR + BE.
 * Consomme l'API figée dans SF-DT-15-01 (backend).
 */
@Injectable({ providedIn: 'root' })
export class InaptitudeService {
  /** F-163 SF-163-02b — `toolId` du dispatcher backend. */
  static readonly STANDALONE_TOOL_ID = 'F-DT-15-inaptitude';

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: InaptitudeRequest):
      Observable<InaptitudeResponse> {
    return this.http.post<InaptitudeResponse>(
      `/api/v1/case-files/${caseFileId}/inaptitude`, request);
  }

  get(caseFileId: string): Observable<InaptitudeResponse> {
    return this.http.get<InaptitudeResponse>(
      `/api/v1/case-files/${caseFileId}/inaptitude`);
  }
  /** F-163 SF-163-02b — POST sur le dispatcher générique des simulateurs. */
  calculateStandalone(request: InaptitudeRequest): Observable<InaptitudeResponse> {
    return this.http.post<InaptitudeResponse>(
      `/api/v1/simulators/${InaptitudeService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
