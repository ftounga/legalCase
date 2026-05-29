import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  VlsTsValidationRequest,
  VlsTsValidationResponse,
} from '../models/vls-ts-validation.model';

/**
 * SF-214-08 : wrapper HttpClient pour l'outil décisionnel
 * "Validation VLS-TS OFII" (F-IM-28-vls-ts-validation-ofii-fr). FR uniquement.
 * Consomme l'API figée dans SF-214-07 (backend).
 *
 * Pattern miroir de {@link RegroupementFamilialService}.
 */
@Injectable({ providedIn: 'root' })
export class VlsTsValidationService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: VlsTsValidationRequest):
      Observable<VlsTsValidationResponse> {
    return this.http.post<VlsTsValidationResponse>(
      `/api/v1/case-files/${caseFileId}/vls-ts-validation-analysis`, request);
  }

  get(caseFileId: string): Observable<VlsTsValidationResponse> {
    return this.http.get<VlsTsValidationResponse>(
      `/api/v1/case-files/${caseFileId}/vls-ts-validation-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-28-vls-ts-validation-ofii-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-28-vls-ts-validation-ofii-fr';
}
