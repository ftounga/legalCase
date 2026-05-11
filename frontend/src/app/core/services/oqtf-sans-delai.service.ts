import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  OqtfSansDelaiRequest,
  OqtfSansDelaiResponse,
} from '../models/oqtf-sans-delai.model';

/**
 * SF-IM-08-04 : wrapper HttpClient pour l'outil décisionnel
 * "OQTF SANS délai de départ volontaire FR" (F-IM-08). FR uniquement.
 * Urgence absolue 48h. Consomme l'API figée dans SF-IM-08-03 (backend).
 */
@Injectable({ providedIn: 'root' })
export class OqtfSansDelaiService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: OqtfSansDelaiRequest):
      Observable<OqtfSansDelaiResponse> {
    return this.http.post<OqtfSansDelaiResponse>(
      `/api/v1/case-files/${caseFileId}/oqtf-sans-delai`, request);
  }

  get(caseFileId: string): Observable<OqtfSansDelaiResponse> {
    return this.http.get<OqtfSansDelaiResponse>(
      `/api/v1/case-files/${caseFileId}/oqtf-sans-delai`);
  }
  /**
   * F-163 SF-163-02d — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-IM-08-oqtf-sans-delai-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-08-oqtf-sans-delai-fr';

  /**
   * F-163 SF-163-02d — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  analyzeStandalone(request: OqtfSansDelaiRequest): Observable<OqtfSansDelaiResponse> {
    return this.http.post<OqtfSansDelaiResponse>(
      `/api/v1/simulators/${OqtfSansDelaiService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }

}
