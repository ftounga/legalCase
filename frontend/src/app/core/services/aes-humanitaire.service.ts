import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AesHumanitaireRequest,
  AesHumanitaireResponse,
} from '../models/aes-humanitaire.model';

/**
 * SF-IM-09-07 : wrapper HttpClient pour l'outil décisionnel AES voie
 * humanitaire (L.435-2 CESEDA + L.432-14 commission du titre de séjour).
 * Consomme l'API figée dans SF-IM-09-03 (PR #507).
 */
@Injectable({ providedIn: 'root' })
export class AesHumanitaireService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: AesHumanitaireRequest): Observable<AesHumanitaireResponse> {
    return this.http.post<AesHumanitaireResponse>(
      `/api/v1/case-files/${caseFileId}/aes-humanitaire`, request);
  }

  get(caseFileId: string): Observable<AesHumanitaireResponse> {
    return this.http.get<AesHumanitaireResponse>(
      `/api/v1/case-files/${caseFileId}/aes-humanitaire`);
  }
  /**
   * F-163 SF-163-02d — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-IM-09-aes-humanitaire')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-09-aes-humanitaire';

  /**
   * F-163 SF-163-02d — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  calculateStandalone(request: AesHumanitaireRequest): Observable<AesHumanitaireResponse> {
    return this.http.post<AesHumanitaireResponse>(
      `/api/v1/simulators/${AesHumanitaireService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }

}
