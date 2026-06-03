import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PacsVpfRequest,
  PacsVpfResponse,
} from '../models/pacs-vpf.model';

/**
 * SF-220-04 : wrapper HttpClient pour l'outil décisionnel
 * "VPF au titre d'un PACS L.423-23" (F-IM-50). FR uniquement.
 * Consomme l'API figée dans SF-220-04 (backend).
 *
 * Pattern miroir de {@link VpfJeuneMajeurService}.
 */
@Injectable({ providedIn: 'root' })
export class PacsVpfService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: PacsVpfRequest):
      Observable<PacsVpfResponse> {
    return this.http.post<PacsVpfResponse>(
      `/api/v1/case-files/${caseFileId}/pacs-vpf-analysis`, request);
  }

  get(caseFileId: string): Observable<PacsVpfResponse> {
    return this.http.get<PacsVpfResponse>(
      `/api/v1/case-files/${caseFileId}/pacs-vpf-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-50-pacs-vpf-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-50-pacs-vpf-fr';
}
