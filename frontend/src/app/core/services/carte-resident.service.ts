import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CarteResidentRequest,
  CarteResidentResponse,
} from '../models/carte-resident.model';

/**
 * SF-214-24 : wrapper HttpClient pour l'outil décisionnel
 * « Carte de résident — article L. 426-1 du CESEDA » (F-IM-36). FR uniquement.
 * Consomme l'API figée dans SF-214-23 (backend).
 *
 * Pattern miroir de {@link VictimeTraiteService}.
 */
@Injectable({ providedIn: 'root' })
export class CarteResidentService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: CarteResidentRequest):
      Observable<CarteResidentResponse> {
    return this.http.post<CarteResidentResponse>(
      `/api/v1/case-files/${caseFileId}/carte-resident-analysis`, request);
  }

  get(caseFileId: string): Observable<CarteResidentResponse> {
    return this.http.get<CarteResidentResponse>(
      `/api/v1/case-files/${caseFileId}/carte-resident-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-36-carte-resident-l4261-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-36-carte-resident-l4261-fr';
}
