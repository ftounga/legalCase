import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ParticulierEmployeurCesuRequest,
  ParticulierEmployeurCesuResponse,
} from '../models/particulier-employeur-cesu.model';

/**
 * SF-218-14 : wrapper HttpClient pour l'outil décisionnel « Particulier
 * employeur (CESU) : préavis et indemnité de licenciement »
 * (F-DT-108-particulier-employeur-cesu). FRANCE uniquement. Consomme l'API
 * figée dans SF-218-13 (backend) :
 *   POST/GET /api/v1/case-files/{caseFileId}/particulier-employeur-cesu-analysis
 *
 * Pattern miroir de {@link SaisieRemunerationService} (F-DT-89, SF-218-08).
 */
@Injectable({ providedIn: 'root' })
export class ParticulierEmployeurCesuService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: ParticulierEmployeurCesuRequest):
      Observable<ParticulierEmployeurCesuResponse> {
    return this.http.post<ParticulierEmployeurCesuResponse>(
      `/api/v1/case-files/${caseFileId}/particulier-employeur-cesu-analysis`, request);
  }

  get(caseFileId: string): Observable<ParticulierEmployeurCesuResponse> {
    return this.http.get<ParticulierEmployeurCesuResponse>(
      `/api/v1/case-files/${caseFileId}/particulier-employeur-cesu-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-DT-108-particulier-employeur-cesu')`
   * et sur le seed `decision_tool_visibility_rules`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-108-particulier-employeur-cesu';
}
