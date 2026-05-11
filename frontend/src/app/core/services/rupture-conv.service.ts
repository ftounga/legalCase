import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RuptureConvRequest, RuptureConvResponse } from '../models/rupture-conv.model';

/**
 * F-DT-10 — Validité de la rupture conventionnelle.
 *
 * Modes :
 *  - case-file : `analyze(caseFileId, req)` / `get(caseFileId)`.
 *  - simulateur autonome (F-163 SF-163-02b) : `analyzeStandalone(req)` POSTe
 *    sur le dispatcher `/api/v1/simulators/{toolId}/calculate` (SF-163-03).
 */
@Injectable({ providedIn: 'root' })
export class RuptureConvService {
  static readonly STANDALONE_TOOL_ID = 'F-DT-10-rupture-conv-validity';

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: RuptureConvRequest): Observable<RuptureConvResponse> {
    return this.http.post<RuptureConvResponse>(
      `/api/v1/case-files/${caseFileId}/rupture-conv`, request);
  }

  get(caseFileId: string): Observable<RuptureConvResponse> {
    return this.http.get<RuptureConvResponse>(
      `/api/v1/case-files/${caseFileId}/rupture-conv`);
  }

  /** F-163 SF-163-02b — POST sur le dispatcher générique des simulateurs. */
  analyzeStandalone(request: RuptureConvRequest): Observable<RuptureConvResponse> {
    return this.http.post<RuptureConvResponse>(
      `/api/v1/simulators/${RuptureConvService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
