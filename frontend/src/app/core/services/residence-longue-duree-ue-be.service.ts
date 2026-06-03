import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ResidenceLongueDureeUeBeRequest,
  ResidenceLongueDureeUeBeResponse,
} from '../models/residence-longue-duree-ue-be.model';

/**
 * SF-221-03 : wrapper HttpClient pour l'outil décisionnel
 * « Résident longue durée UE (BE) » (F-IM-55-residence-longue-duree-ue-be).
 *
 * BELGIQUE uniquement — éligibilité au statut de résident longue durée UE
 * (art. 15bis Loi 15/12/1980 — directive 2003/109/CE) après 5 ans (60 mois)
 * de séjour légal ininterrompu + conditions matérielles + mobilité intra-UE.
 *
 * Consomme l'API figée dans SF-221-03 (backend) :
 *  - POST /api/v1/case-files/{caseFileId}/residence-longue-duree-ue-be-analysis
 *  - GET  /api/v1/case-files/{caseFileId}/residence-longue-duree-ue-be-analysis
 *
 * Pattern miroir de {@link CarteBSejourIllimiteBeService}.
 */
@Injectable({ providedIn: 'root' })
export class ResidenceLongueDureeUeBeService {
  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: ResidenceLongueDureeUeBeRequest):
      Observable<ResidenceLongueDureeUeBeResponse> {
    return this.http.post<ResidenceLongueDureeUeBeResponse>(
      `/api/v1/case-files/${caseFileId}/residence-longue-duree-ue-be-analysis`, request);
  }

  get(caseFileId: string): Observable<ResidenceLongueDureeUeBeResponse> {
    return this.http.get<ResidenceLongueDureeUeBeResponse>(
      `/api/v1/case-files/${caseFileId}/residence-longue-duree-ue-be-analysis`);
  }

  /** `toolId` aligné sur la clé `TOOL_REGISTRY.get('F-IM-55-residence-longue-duree-ue-be')`. */
  static readonly STANDALONE_TOOL_ID = 'F-IM-55-residence-longue-duree-ue-be';
}
