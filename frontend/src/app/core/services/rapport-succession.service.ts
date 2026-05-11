import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RapportSuccessionRequest,
  RapportSuccessionResponse,
} from '../models/rapport-succession.model';

/**
 * SF-FA-24-14 : wrapper HttpClient pour l'outil décisionnel "Rapport à
 * succession" (F-FA-24). FR uniquement — art. 843-863 + 919 Cciv.
 *
 * Consomme l'API figée dans SF-FA-24-13 (backend, mergé PR #679).
 */
@Injectable({ providedIn: 'root' })
export class RapportSuccessionService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: RapportSuccessionRequest): Observable<RapportSuccessionResponse> {
    return this.http.post<RapportSuccessionResponse>(
      `/api/v1/case-files/${caseFileId}/rapport-succession-analysis`, request);
  }

  get(caseFileId: string): Observable<RapportSuccessionResponse> {
    return this.http.get<RapportSuccessionResponse>(
      `/api/v1/case-files/${caseFileId}/rapport-succession-analysis`);
  }

  /**
   * F-163 SF-163-02c — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-FA-24-rapport-succession')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-FA-24-rapport-succession';

  /**
   * F-163 SF-163-02c — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  calculateStandalone(request: RapportSuccessionRequest): Observable<RapportSuccessionResponse> {
    return this.http.post<RapportSuccessionResponse>(
      `/api/v1/simulators/${RapportSuccessionService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
