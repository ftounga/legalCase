import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  VictimeTraiteBeRequest,
  VictimeTraiteBeResponse,
} from '../models/victime-traite-be.model';

/**
 * SF-221-06 : wrapper HttpClient pour l'outil décisionnel « Titre de séjour victime de la
 * traite des êtres humains (BE) » (F-IM-58-victime-traite-be).
 *
 * BELGIQUE uniquement — éligibilité au titre victime de la traite (art. 61/2 et s. Loi
 * 15/12/1980, circulaire du 26/09/2008). Régime BE propre (3 phases), distinct du pendant
 * FR F-IM-35 (L. 425-1 CESEDA).
 *
 * Consomme l'API figée dans SF-221-06 (backend) :
 *  - POST /api/v1/case-files/{caseFileId}/victime-traite-be-analysis
 *  - GET  /api/v1/case-files/{caseFileId}/victime-traite-be-analysis
 */
@Injectable({ providedIn: 'root' })
export class VictimeTraiteBeService {
  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: VictimeTraiteBeRequest):
      Observable<VictimeTraiteBeResponse> {
    return this.http.post<VictimeTraiteBeResponse>(
      `/api/v1/case-files/${caseFileId}/victime-traite-be-analysis`, request);
  }

  get(caseFileId: string): Observable<VictimeTraiteBeResponse> {
    return this.http.get<VictimeTraiteBeResponse>(
      `/api/v1/case-files/${caseFileId}/victime-traite-be-analysis`);
  }

  /** `toolId` aligné sur la clé `TOOL_REGISTRY.get('F-IM-58-victime-traite-be')`. */
  static readonly STANDALONE_TOOL_ID = 'F-IM-58-victime-traite-be';
}
