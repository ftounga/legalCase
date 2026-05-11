import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  VictimeViolencesL4256Request,
  VictimeViolencesL4256Response,
} from '../models/victime-violences-l4256.model';

/**
 * SF-208-08 : wrapper HttpClient pour l'outil decisionnel
 * "Victime de violences L.425-6 FR" (F-IM-24). FR uniquement.
 * Consomme l'API figee dans SF-208-04 (backend, PR #915).
 */
@Injectable({ providedIn: 'root' })
export class VictimeViolencesL4256Service {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: VictimeViolencesL4256Request):
      Observable<VictimeViolencesL4256Response> {
    return this.http.post<VictimeViolencesL4256Response>(
      `/api/v1/case-files/${caseFileId}/victime-violences-l4256-analysis`, request);
  }

  get(caseFileId: string): Observable<VictimeViolencesL4256Response> {
    return this.http.get<VictimeViolencesL4256Response>(
      `/api/v1/case-files/${caseFileId}/victime-violences-l4256-analysis`);
  }
  /**
   * F-163 SF-163-02d — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-IM-24-victime-violences-l4256-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-24-victime-violences-l4256-fr';

  /**
   * F-163 SF-163-02d — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  analyzeStandalone(request: VictimeViolencesL4256Request): Observable<VictimeViolencesL4256Response> {
    return this.http.post<VictimeViolencesL4256Response>(
      `/api/v1/simulators/${VictimeViolencesL4256Service.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }

}
