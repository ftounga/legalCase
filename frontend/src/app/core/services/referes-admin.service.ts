import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ReferesAdminRequest,
  ReferesAdminResponse,
} from '../models/referes-admin.model';

/**
 * SF-IM-08-08 : wrapper HttpClient pour l'outil décisionnel
 * "Référés administratifs L.521-1 / L.521-2 — France" (F-IM-08).
 * FR uniquement. Consomme l'API figée dans SF-IM-08-07 (backend).
 */
@Injectable({ providedIn: 'root' })
export class ReferesAdminService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: ReferesAdminRequest):
      Observable<ReferesAdminResponse> {
    return this.http.post<ReferesAdminResponse>(
      `/api/v1/case-files/${caseFileId}/referes-admin`, request);
  }

  get(caseFileId: string): Observable<ReferesAdminResponse> {
    return this.http.get<ReferesAdminResponse>(
      `/api/v1/case-files/${caseFileId}/referes-admin`);
  }
  /**
   * F-163 SF-163-02d — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-IM-08-referes-admin-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-08-referes-admin-fr';

  /**
   * F-163 SF-163-02d — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  analyzeStandalone(request: ReferesAdminRequest): Observable<ReferesAdminResponse> {
    return this.http.post<ReferesAdminResponse>(
      `/api/v1/simulators/${ReferesAdminService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }

}
