import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  MotifGraveBeRequest,
  MotifGraveBeResponse,
} from '../models/motif-grave-be.model';

/**
 * SF-DT-27-02 : wrapper HttpClient pour l'outil décisionnel
 * "Motif grave BE" (F-DT-27). BE uniquement.
 * Consomme l'API figée dans SF-DT-27-01 (backend, PR #497).
 */
@Injectable({ providedIn: 'root' })
export class MotifGraveBeService {
  /** F-163 SF-163-02b — `toolId` du dispatcher backend. */
  static readonly STANDALONE_TOOL_ID = 'F-DT-27-motif-grave-be';

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: MotifGraveBeRequest):
      Observable<MotifGraveBeResponse> {
    return this.http.post<MotifGraveBeResponse>(
      `/api/v1/case-files/${caseFileId}/motif-grave-be`, request);
  }

  get(caseFileId: string): Observable<MotifGraveBeResponse> {
    return this.http.get<MotifGraveBeResponse>(
      `/api/v1/case-files/${caseFileId}/motif-grave-be`);
  }
  /** F-163 SF-163-02b — POST sur le dispatcher générique des simulateurs. */
  calculateStandalone(request: MotifGraveBeRequest): Observable<MotifGraveBeResponse> {
    return this.http.post<MotifGraveBeResponse>(
      `/api/v1/simulators/${MotifGraveBeService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
