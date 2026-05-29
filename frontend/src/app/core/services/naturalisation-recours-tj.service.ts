import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  NaturalisationRecoursTjRequest,
  NaturalisationRecoursTjResponse,
} from '../models/naturalisation-recours-tj.model';

/**
 * SF-214-30 : wrapper HttpClient pour l'outil décisionnel
 * "Recours TJ naturalisation" (F-IM-39-naturalisation-recours-tj-fr). FR uniquement.
 * Consomme l'API figée dans SF-214-29 (backend).
 *
 * Pattern miroir de {@link VlsTsValidationService} / {@link AjCndaService}.
 */
@Injectable({ providedIn: 'root' })
export class NaturalisationRecoursTjService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: NaturalisationRecoursTjRequest):
      Observable<NaturalisationRecoursTjResponse> {
    return this.http.post<NaturalisationRecoursTjResponse>(
      `/api/v1/case-files/${caseFileId}/naturalisation-recours-tj-analysis`, request);
  }

  get(caseFileId: string): Observable<NaturalisationRecoursTjResponse> {
    return this.http.get<NaturalisationRecoursTjResponse>(
      `/api/v1/case-files/${caseFileId}/naturalisation-recours-tj-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-39-naturalisation-recours-tj-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-39-naturalisation-recours-tj-fr';
}
