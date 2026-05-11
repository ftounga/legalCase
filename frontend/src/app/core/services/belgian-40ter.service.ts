import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Belgian40terRequest,
  Belgian40terResponse,
} from '../models/belgian-40ter.model';

/**
 * SF-IM-14-08 : wrapper HttpClient pour l'outil décisionnel
 * "Regroupement familial d'un Belge" (40ter — F-IM-14). BE uniquement.
 * Consomme l'API figée dans SF-IM-14-04 (backend mergée PR #511).
 */
@Injectable({ providedIn: 'root' })
export class Belgian40terService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: Belgian40terRequest):
      Observable<Belgian40terResponse> {
    return this.http.post<Belgian40terResponse>(
      `/api/v1/case-files/${caseFileId}/belgian-40ter`, request);
  }

  get(caseFileId: string): Observable<Belgian40terResponse> {
    return this.http.get<Belgian40terResponse>(
      `/api/v1/case-files/${caseFileId}/belgian-40ter`);
  }
  /**
   * F-163 SF-163-02d — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-IM-14-40ter-familial-belge-be')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-14-40ter-familial-belge-be';

  /**
   * F-163 SF-163-02d — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  analyzeStandalone(request: Belgian40terRequest): Observable<Belgian40terResponse> {
    return this.http.post<Belgian40terResponse>(
      `/api/v1/simulators/${Belgian40terService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }

}
