import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  TempsTrajetDeplacementRequest,
  TempsTrajetDeplacementResponse,
} from '../models/temps-trajet-deplacement.model';

/**
 * SF-218-52 : wrapper HttpClient pour l'outil décisionnel « Temps de trajet /
 * déplacement professionnel » (F-DT-81-temps-trajet-deplacement). FRANCE
 * uniquement. Consomme l'API figée dans SF-218-51 (backend) :
 *   POST/GET /api/v1/case-files/{caseFileId}/temps-trajet-deplacement-analysis
 *
 * DISTINCT du remboursement de frais de déplacement et de l'astreinte.
 */
@Injectable({ providedIn: 'root' })
export class TempsTrajetDeplacementService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: TempsTrajetDeplacementRequest):
      Observable<TempsTrajetDeplacementResponse> {
    return this.http.post<TempsTrajetDeplacementResponse>(
      `/api/v1/case-files/${caseFileId}/temps-trajet-deplacement-analysis`, request);
  }

  get(caseFileId: string): Observable<TempsTrajetDeplacementResponse> {
    return this.http.get<TempsTrajetDeplacementResponse>(
      `/api/v1/case-files/${caseFileId}/temps-trajet-deplacement-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-DT-81-temps-trajet-deplacement')`
   * et sur le seed `decision_tool_visibility_rules`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-DT-81-temps-trajet-deplacement';
}
