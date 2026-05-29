import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  OfpraIntroductionRequest,
  OfpraIntroductionResponse,
} from '../models/ofpra-introduction.model';

/**
 * SF-214-18 : wrapper HttpClient pour l'outil décisionnel
 * « OFPRA introduction » (F-IM-33). FR uniquement.
 * Consomme l'API figée dans SF-214-17 (backend).
 *
 * Pattern miroir de {@link RecepisseAttestationService}.
 */
@Injectable({ providedIn: 'root' })
export class OfpraIntroductionService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: OfpraIntroductionRequest):
      Observable<OfpraIntroductionResponse> {
    return this.http.post<OfpraIntroductionResponse>(
      `/api/v1/case-files/${caseFileId}/ofpra-introduction-analysis`, request);
  }

  get(caseFileId: string): Observable<OfpraIntroductionResponse> {
    return this.http.get<OfpraIntroductionResponse>(
      `/api/v1/case-files/${caseFileId}/ofpra-introduction-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-33-ofpra-introduction-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-33-ofpra-introduction-fr';
}
