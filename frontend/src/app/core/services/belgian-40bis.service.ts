import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Belgian40bisRequest,
  Belgian40bisResponse,
} from '../models/belgian-40bis.model';

/**
 * SF-IM-14-07 : wrapper HttpClient pour l'outil décisionnel
 * "Regroupement familial citoyen UE — art. 40bis Loi 15/12/1980"
 * (F-IM-14). BE uniquement. Consomme l'API figée en SF-IM-14-03
 * (backend, PR #510).
 */
@Injectable({ providedIn: 'root' })
export class Belgian40bisService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: Belgian40bisRequest):
      Observable<Belgian40bisResponse> {
    return this.http.post<Belgian40bisResponse>(
      `/api/v1/case-files/${caseFileId}/belgian-40bis`, request);
  }

  get(caseFileId: string): Observable<Belgian40bisResponse> {
    return this.http.get<Belgian40bisResponse>(
      `/api/v1/case-files/${caseFileId}/belgian-40bis`);
  }
  /**
   * F-163 SF-163-02d — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-IM-14-40bis-cohabitant-ue-be')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-14-40bis-cohabitant-ue-be';

  /**
   * F-163 SF-163-02d — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  calculateStandalone(request: Belgian40bisRequest): Observable<Belgian40bisResponse> {
    return this.http.post<Belgian40bisResponse>(
      `/api/v1/simulators/${Belgian40bisService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }

}
