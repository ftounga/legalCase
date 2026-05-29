import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  UeEeeSuisseSejourRequest,
  UeEeeSuisseSejourResponse,
} from '../models/ue-eee-suisse-sejour.model';

/**
 * SF-214-40 : wrapper HttpClient pour l'outil décisionnel
 * "Séjour UE/EEE/Suisse" (F-IM-44-ue-eee-suisse-sejour-fr). FR uniquement.
 * Consomme l'API figée dans SF-214-39 (backend).
 *
 * Pattern miroir de {@link ItfJudiciaireService} (F-IM-43).
 */
@Injectable({ providedIn: 'root' })
export class UeEeeSuisseSejourService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: UeEeeSuisseSejourRequest):
      Observable<UeEeeSuisseSejourResponse> {
    return this.http.post<UeEeeSuisseSejourResponse>(
      `/api/v1/case-files/${caseFileId}/ue-eee-suisse-sejour-analysis`, request);
  }

  get(caseFileId: string): Observable<UeEeeSuisseSejourResponse> {
    return this.http.get<UeEeeSuisseSejourResponse>(
      `/api/v1/case-files/${caseFileId}/ue-eee-suisse-sejour-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-44-ue-eee-suisse-sejour-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-44-ue-eee-suisse-sejour-fr';
}
