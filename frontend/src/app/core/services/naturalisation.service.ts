import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { NaturalisationRequest, NaturalisationResponse } from '../models/naturalisation.model';

/**
 * SF-IM-13-02 : wrapper HttpClient pour l'outil décisionnel "Naturalisation
 * française" (F-IM-13). FR uniquement (Code civil art. 21+).
 * Consomme l'API figée dans SF-IM-13-01 (backend, mergé PR #639).
 */
@Injectable({ providedIn: 'root' })
export class NaturalisationService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: NaturalisationRequest): Observable<NaturalisationResponse> {
    return this.http.post<NaturalisationResponse>(
      `/api/v1/case-files/${caseFileId}/naturalisation-analysis`, request);
  }

  get(caseFileId: string): Observable<NaturalisationResponse> {
    return this.http.get<NaturalisationResponse>(
      `/api/v1/case-files/${caseFileId}/naturalisation-analysis`);
  }
  /**
   * F-163 SF-163-02d — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-IM-13-naturalisation')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-13-naturalisation';

  /**
   * F-163 SF-163-02d — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  calculateStandalone(request: NaturalisationRequest): Observable<NaturalisationResponse> {
    return this.http.post<NaturalisationResponse>(
      `/api/v1/simulators/${NaturalisationService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }

}
