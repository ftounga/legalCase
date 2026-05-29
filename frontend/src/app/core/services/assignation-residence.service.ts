import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AssignationResidenceRequest,
  AssignationResidenceResponse,
} from '../models/assignation-residence.model';

/**
 * SF-214-36 : wrapper HttpClient pour l'outil décisionnel
 * "Assignation à résidence" (F-IM-42-assignation-residence-fr). FR uniquement.
 * Consomme l'API figée dans SF-214-35 (backend).
 *
 * Pattern miroir de {@link AppelCaaCassationService} (F-IM-41).
 */
@Injectable({ providedIn: 'root' })
export class AssignationResidenceService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: AssignationResidenceRequest):
      Observable<AssignationResidenceResponse> {
    return this.http.post<AssignationResidenceResponse>(
      `/api/v1/case-files/${caseFileId}/assignation-residence-analysis`, request);
  }

  get(caseFileId: string): Observable<AssignationResidenceResponse> {
    return this.http.get<AssignationResidenceResponse>(
      `/api/v1/case-files/${caseFileId}/assignation-residence-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-42-assignation-residence-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-42-assignation-residence-fr';
}
