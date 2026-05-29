import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RecepisseAttestationRequest,
  RecepisseAttestationResponse,
} from '../models/recepisse-attestation.model';

/**
 * SF-214-16 : wrapper HttpClient pour l'outil décisionnel
 * « Récépissé vs attestation » (F-IM-32). FR uniquement.
 * Consomme l'API figée dans SF-214-15 (backend).
 *
 * Pattern miroir de {@link OqtfCategoriesService}.
 */
@Injectable({ providedIn: 'root' })
export class RecepisseAttestationService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: RecepisseAttestationRequest):
      Observable<RecepisseAttestationResponse> {
    return this.http.post<RecepisseAttestationResponse>(
      `/api/v1/case-files/${caseFileId}/recepisse-attestation-analysis`, request);
  }

  get(caseFileId: string): Observable<RecepisseAttestationResponse> {
    return this.http.get<RecepisseAttestationResponse>(
      `/api/v1/case-files/${caseFileId}/recepisse-attestation-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-32-recepisse-attestation-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-32-recepisse-attestation-fr';
}
