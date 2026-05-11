import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Belgian9terRequest,
  Belgian9terResponse,
} from '../models/belgian-9ter.model';

/**
 * SF-IM-14-06 : wrapper HttpClient pour l'outil décisionnel
 * "Régularisation 9ter médical" (F-IM-14). BE uniquement.
 * Consomme l'API figée dans SF-IM-14-02 (backend, PR #509).
 */
@Injectable({ providedIn: 'root' })
export class Belgian9terService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: Belgian9terRequest):
      Observable<Belgian9terResponse> {
    return this.http.post<Belgian9terResponse>(
      `/api/v1/case-files/${caseFileId}/belgian-9ter`, request);
  }

  get(caseFileId: string): Observable<Belgian9terResponse> {
    return this.http.get<Belgian9terResponse>(
      `/api/v1/case-files/${caseFileId}/belgian-9ter`);
  }
  /**
   * F-163 SF-163-02d — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-IM-14-9ter-medical-be')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-14-9ter-medical-be';

  /**
   * F-163 SF-163-02d — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  analyzeStandalone(request: Belgian9terRequest): Observable<Belgian9terResponse> {
    return this.http.post<Belgian9terResponse>(
      `/api/v1/simulators/${Belgian9terService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }

}
