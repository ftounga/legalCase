import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  NaturalisationRecoursTaRequest,
  NaturalisationRecoursTaResponse,
} from '../models/naturalisation-recours-ta.model';

/**
 * SF-214-32 : wrapper HttpClient pour l'outil décisionnel
 * "Recours TA Nantes naturalisation" (F-IM-40-naturalisation-recours-ta-fr). FR uniquement.
 * Consomme l'API figée dans SF-214-31 (backend).
 *
 * Pattern miroir de {@link NaturalisationRecoursTjService} (F-IM-39).
 */
@Injectable({ providedIn: 'root' })
export class NaturalisationRecoursTaService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: NaturalisationRecoursTaRequest):
      Observable<NaturalisationRecoursTaResponse> {
    return this.http.post<NaturalisationRecoursTaResponse>(
      `/api/v1/case-files/${caseFileId}/naturalisation-recours-ta-analysis`, request);
  }

  get(caseFileId: string): Observable<NaturalisationRecoursTaResponse> {
    return this.http.get<NaturalisationRecoursTaResponse>(
      `/api/v1/case-files/${caseFileId}/naturalisation-recours-ta-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-40-naturalisation-recours-ta-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-40-naturalisation-recours-ta-fr';
}
