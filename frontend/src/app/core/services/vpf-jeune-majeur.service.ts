import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  VpfJeuneMajeurRequest,
  VpfJeuneMajeurResponse,
} from '../models/vpf-jeune-majeur.model';

/**
 * SF-220-03 : wrapper HttpClient pour l'outil décisionnel
 * "VPF jeune majeur L.423-22" (F-IM-49). FR uniquement.
 * Consomme l'API figée dans SF-220-03 (backend).
 *
 * Pattern miroir de {@link RegimeMayotteService}.
 */
@Injectable({ providedIn: 'root' })
export class VpfJeuneMajeurService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: VpfJeuneMajeurRequest):
      Observable<VpfJeuneMajeurResponse> {
    return this.http.post<VpfJeuneMajeurResponse>(
      `/api/v1/case-files/${caseFileId}/vpf-jeune-majeur-analysis`, request);
  }

  get(caseFileId: string): Observable<VpfJeuneMajeurResponse> {
    return this.http.get<VpfJeuneMajeurResponse>(
      `/api/v1/case-files/${caseFileId}/vpf-jeune-majeur-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-49-vpf-jeune-majeur-l42322-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-49-vpf-jeune-majeur-l42322-fr';
}
