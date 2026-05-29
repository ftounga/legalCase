import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AutorisationTravailEmployeurRequest,
  AutorisationTravailEmployeurResponse,
} from '../models/autorisation-travail-employeur.model';

/**
 * SF-214-44 : wrapper HttpClient pour l'outil décisionnel
 * "Autorisation travail employeur"
 * (F-IM-46-autorisation-travail-employeur-fr). FR uniquement.
 * Consomme l'API figée dans SF-214-43 (backend).
 *
 * Pattern miroir de {@link RetraitTitreFraudeService} (F-IM-45).
 */
@Injectable({ providedIn: 'root' })
export class AutorisationTravailEmployeurService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: AutorisationTravailEmployeurRequest):
      Observable<AutorisationTravailEmployeurResponse> {
    return this.http.post<AutorisationTravailEmployeurResponse>(
      `/api/v1/case-files/${caseFileId}/autorisation-travail-employeur-analysis`, request);
  }

  get(caseFileId: string): Observable<AutorisationTravailEmployeurResponse> {
    return this.http.get<AutorisationTravailEmployeurResponse>(
      `/api/v1/case-files/${caseFileId}/autorisation-travail-employeur-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-46-autorisation-travail-employeur-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-46-autorisation-travail-employeur-fr';
}
