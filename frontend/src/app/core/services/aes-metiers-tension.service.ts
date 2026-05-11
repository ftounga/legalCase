import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AesMetiersTensionRequest,
  AesMetiersTensionResponse,
} from '../models/aes-metiers-tension.model';

/**
 * SF-IM-09-05 : wrapper HttpClient pour l'outil décisionnel
 * "AES Métiers en tension — France" (F-IM-09).
 * Consomme l'API figée dans SF-IM-09-01 (backend PR #504).
 */
@Injectable({ providedIn: 'root' })
export class AesMetiersTensionService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: AesMetiersTensionRequest):
      Observable<AesMetiersTensionResponse> {
    return this.http.post<AesMetiersTensionResponse>(
      `/api/v1/case-files/${caseFileId}/aes-metiers-tension`, request);
  }

  get(caseFileId: string): Observable<AesMetiersTensionResponse> {
    return this.http.get<AesMetiersTensionResponse>(
      `/api/v1/case-files/${caseFileId}/aes-metiers-tension`);
  }
  /**
   * F-163 SF-163-02d — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-IM-09-aes-metiers-tension')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-09-aes-metiers-tension';

  /**
   * F-163 SF-163-02d — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  calculateStandalone(request: AesMetiersTensionRequest): Observable<AesMetiersTensionResponse> {
    return this.http.post<AesMetiersTensionResponse>(
      `/api/v1/simulators/${AesMetiersTensionService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }

}
