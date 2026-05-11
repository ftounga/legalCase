import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ReserveHereditaireRequest,
  ReserveHereditaireResponse,
} from '../models/reserve-heriditaire.model';

/**
 * SF-FA-24-08 : wrapper HttpClient pour l'outil décisionnel "Réserve
 * héréditaire et action en réduction" (F-FA-24). FR uniquement —
 * art. 913 + 914-1 + 920-928 Cciv.
 *
 * Consomme l'API figée dans SF-FA-24-07 (backend, mergé PR #672).
 */
@Injectable({ providedIn: 'root' })
export class ReserveHereditaireService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: ReserveHereditaireRequest): Observable<ReserveHereditaireResponse> {
    return this.http.post<ReserveHereditaireResponse>(
      `/api/v1/case-files/${caseFileId}/reserve-heriditaire-analysis`, request);
  }

  get(caseFileId: string): Observable<ReserveHereditaireResponse> {
    return this.http.get<ReserveHereditaireResponse>(
      `/api/v1/case-files/${caseFileId}/reserve-heriditaire-analysis`);
  }

  /**
   * F-163 SF-163-02c — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-FA-24-reserve-heriditaire')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-FA-24-reserve-heriditaire';

  /**
   * F-163 SF-163-02c — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  calculateStandalone(request: ReserveHereditaireRequest): Observable<ReserveHereditaireResponse> {
    return this.http.post<ReserveHereditaireResponse>(
      `/api/v1/simulators/${ReserveHereditaireService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
