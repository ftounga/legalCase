import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AesFamilleRequest, AesFamilleResponse } from '../models/aes-famille.model';

/**
 * SF-IM-09-06 : wrapper HttpClient pour l'outil décisionnel AES voie familiale
 * (L.435-1 CESEDA + circulaire Valls 28/11/2012). Consomme l'API figée dans
 * SF-IM-09-02 (PR #506).
 */
@Injectable({ providedIn: 'root' })
export class AesFamilleService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: AesFamilleRequest): Observable<AesFamilleResponse> {
    return this.http.post<AesFamilleResponse>(
      `/api/v1/case-files/${caseFileId}/aes-famille`, request);
  }

  get(caseFileId: string): Observable<AesFamilleResponse> {
    return this.http.get<AesFamilleResponse>(
      `/api/v1/case-files/${caseFileId}/aes-famille`);
  }
  /**
   * F-163 SF-163-02d — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-IM-09-aes-famille')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-09-aes-famille';

  /**
   * F-163 SF-163-02d — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  calculateStandalone(request: AesFamilleRequest): Observable<AesFamilleResponse> {
    return this.http.post<AesFamilleResponse>(
      `/api/v1/simulators/${AesFamilleService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }

}
