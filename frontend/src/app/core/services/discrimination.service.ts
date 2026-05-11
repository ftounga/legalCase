import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DiscriminationRequest,
  DiscriminationResponse,
} from '../models/discrimination.model';

/**
 * SF-DT-12-02 : wrapper HttpClient pour l'outil décisionnel
 * "Discrimination — dommages-intérêts" (F-DT-12). Consomme l'API figée
 * dans SF-DT-12-01 (backend) — endpoint
 * `/api/v1/case-files/{id}/discrimination-dommages-interets`.
 */
@Injectable({ providedIn: 'root' })
export class DiscriminationService {
  /** F-163 SF-163-02b — `toolId` du dispatcher backend. */
  static readonly STANDALONE_TOOL_ID = 'F-DT-12-discrimination-dommages-interets';

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: DiscriminationRequest):
      Observable<DiscriminationResponse> {
    return this.http.post<DiscriminationResponse>(
      `/api/v1/case-files/${caseFileId}/discrimination-dommages-interets`, request);
  }

  get(caseFileId: string): Observable<DiscriminationResponse> {
    return this.http.get<DiscriminationResponse>(
      `/api/v1/case-files/${caseFileId}/discrimination-dommages-interets`);
  }
  /** F-163 SF-163-02b — POST sur le dispatcher générique des simulateurs. */
  calculateStandalone(request: DiscriminationRequest): Observable<DiscriminationResponse> {
    return this.http.post<DiscriminationResponse>(
      `/api/v1/simulators/${DiscriminationService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
