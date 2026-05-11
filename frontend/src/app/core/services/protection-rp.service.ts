import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ProtectionRpRequest, ProtectionRpResponse } from '../models/protection-rp.model';

/**
 * SF-DT-30-02 : wrapper HttpClient pour l'outil décisionnel "Protection des
 * représentants du personnel" (F-DT-30). FR uniquement.
 * Consomme l'API figée dans SF-DT-30-01 (backend, mergé PR #631).
 */
@Injectable({ providedIn: 'root' })
export class ProtectionRpService {
  /** F-163 SF-163-02b — `toolId` du dispatcher backend. */
  static readonly STANDALONE_TOOL_ID = 'F-DT-30-protection-rp';

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: ProtectionRpRequest): Observable<ProtectionRpResponse> {
    return this.http.post<ProtectionRpResponse>(
      `/api/v1/case-files/${caseFileId}/protection-rp-analysis`, request);
  }

  get(caseFileId: string): Observable<ProtectionRpResponse> {
    return this.http.get<ProtectionRpResponse>(
      `/api/v1/case-files/${caseFileId}/protection-rp-analysis`);
  }
  /** F-163 SF-163-02b — POST sur le dispatcher générique des simulateurs. */
  calculateStandalone(request: ProtectionRpRequest): Observable<ProtectionRpResponse> {
    return this.http.post<ProtectionRpResponse>(
      `/api/v1/simulators/${ProtectionRpService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
