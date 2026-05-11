import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Annexe13BeRequest,
  Annexe13BeResponse,
} from '../models/annexe13-be.model';

/**
 * SF-IM-08-06 : wrapper HttpClient pour l'outil décisionnel
 * "Annexe 13 — OQT belge" (F-IM-08). BE uniquement.
 * Consomme l'API figée dans SF-IM-08-05 (backend).
 */
@Injectable({ providedIn: 'root' })
export class Annexe13BeService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: Annexe13BeRequest):
      Observable<Annexe13BeResponse> {
    return this.http.post<Annexe13BeResponse>(
      `/api/v1/case-files/${caseFileId}/annexe13-be`, request);
  }

  get(caseFileId: string): Observable<Annexe13BeResponse> {
    return this.http.get<Annexe13BeResponse>(
      `/api/v1/case-files/${caseFileId}/annexe13-be`);
  }
  /**
   * F-163 SF-163-02d — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-IM-08-annexe13-be')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-08-annexe13-be';

  /**
   * F-163 SF-163-02d — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  analyzeStandalone(request: Annexe13BeRequest): Observable<Annexe13BeResponse> {
    return this.http.post<Annexe13BeResponse>(
      `/api/v1/simulators/${Annexe13BeService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }

}
