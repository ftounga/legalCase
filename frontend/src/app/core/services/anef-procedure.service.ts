import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AnefProcedureRequest,
  AnefProcedureResponse,
} from '../models/anef-procedure.model';

/**
 * SF-214-26 : wrapper HttpClient pour l'outil décisionnel
 * « ANEF procédure / pannes » (F-IM-37). FR uniquement.
 * Consomme l'API figée dans SF-214-25 (backend).
 *
 * Pattern miroir de {@link OfpraIntroductionService}.
 */
@Injectable({ providedIn: 'root' })
export class AnefProcedureService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: AnefProcedureRequest):
      Observable<AnefProcedureResponse> {
    return this.http.post<AnefProcedureResponse>(
      `/api/v1/case-files/${caseFileId}/anef-procedure-analysis`, request);
  }

  get(caseFileId: string): Observable<AnefProcedureResponse> {
    return this.http.get<AnefProcedureResponse>(
      `/api/v1/case-files/${caseFileId}/anef-procedure-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-37-anef-procedure-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-37-anef-procedure-fr';
}
