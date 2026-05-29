import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  VpfLiensPersonnelsRequest,
  VpfLiensPersonnelsResponse,
} from '../models/vpf-liens-personnels.model';

/**
 * SF-214-06 : wrapper HttpClient pour l'outil décisionnel
 * « Vie privée et familiale — liens personnels L.423-23 CESEDA »
 * (F-IM-27-vpf-liens-personnels-l42323-fr). FRANCE uniquement.
 * Consomme l'API figée dans SF-214-05 (backend).
 *
 * Pattern miroir de {@link RegroupementFamilialService}.
 */
@Injectable({ providedIn: 'root' })
export class VpfLiensPersonnelsService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: VpfLiensPersonnelsRequest):
      Observable<VpfLiensPersonnelsResponse> {
    return this.http.post<VpfLiensPersonnelsResponse>(
      `/api/v1/case-files/${caseFileId}/vpf-liens-personnels-analysis`, request);
  }

  get(caseFileId: string): Observable<VpfLiensPersonnelsResponse> {
    return this.http.get<VpfLiensPersonnelsResponse>(
      `/api/v1/case-files/${caseFileId}/vpf-liens-personnels-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-27-vpf-liens-personnels-l42323-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-27-vpf-liens-personnels-l42323-fr';
}
