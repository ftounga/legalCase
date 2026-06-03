import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DetentionCentreFermeBeRequest,
  DetentionCentreFermeBeResponse,
} from '../models/detention-centre-ferme-be.model';

/**
 * SF-221-04 : wrapper HttpClient pour l'outil décisionnel « Détention en centre fermé +
 * requête de mise en liberté (BE) » (F-IM-56-detention-centre-ferme-be).
 *
 * BELGIQUE uniquement — durée de la détention en centre fermé (art. 7/27/29/74/5
 * Loi 15/12/1980, AR 02/08/2002) + fenêtre de requête de mise en liberté devant la
 * chambre du conseil (art. 71 et s. ; 5 j indicatif).
 *
 * Consomme l'API figée dans SF-221-04 (backend) :
 *  - POST /api/v1/case-files/{caseFileId}/detention-centre-ferme-be-analysis
 *  - GET  /api/v1/case-files/{caseFileId}/detention-centre-ferme-be-analysis
 *
 * Pattern miroir de {@link ResidenceLongueDureeUeBeService}.
 */
@Injectable({ providedIn: 'root' })
export class DetentionCentreFermeBeService {
  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: DetentionCentreFermeBeRequest):
      Observable<DetentionCentreFermeBeResponse> {
    return this.http.post<DetentionCentreFermeBeResponse>(
      `/api/v1/case-files/${caseFileId}/detention-centre-ferme-be-analysis`, request);
  }

  get(caseFileId: string): Observable<DetentionCentreFermeBeResponse> {
    return this.http.get<DetentionCentreFermeBeResponse>(
      `/api/v1/case-files/${caseFileId}/detention-centre-ferme-be-analysis`);
  }

  /** `toolId` aligné sur la clé `TOOL_REGISTRY.get('F-IM-56-detention-centre-ferme-be')`. */
  static readonly STANDALONE_TOOL_ID = 'F-IM-56-detention-centre-ferme-be';
}
