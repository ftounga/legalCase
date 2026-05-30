import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ApprentissageRuptureRequest,
  ApprentissageRuptureResponse,
} from '../models/apprentissage-rupture.model';

/**
 * SF-218-24 : wrapper HttpClient pour l'outil décisionnel « Apprentissage :
 * validité de la rupture du contrat » (F-DT-110-apprentissage-rupture).
 * FRANCE uniquement. Consomme l'API figée dans SF-218-23 (backend) :
 *   POST/GET /api/v1/case-files/{caseFileId}/apprentissage-rupture-analysis
 *
 * Pattern miroir de {@link StagiaireGratificationService} (F-DT-109, SF-218-22).
 */
@Injectable({ providedIn: 'root' })
export class ApprentissageRuptureService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: ApprentissageRuptureRequest):
      Observable<ApprentissageRuptureResponse> {
    return this.http.post<ApprentissageRuptureResponse>(
      `/api/v1/case-files/${caseFileId}/apprentissage-rupture-analysis`, request);
  }

  get(caseFileId: string): Observable<ApprentissageRuptureResponse> {
    return this.http.get<ApprentissageRuptureResponse>(
      `/api/v1/case-files/${caseFileId}/apprentissage-rupture-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-DT-110-apprentissage-rupture')`
   * et sur le seed `decision_tool_visibility_rules`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-110-apprentissage-rupture';
}
