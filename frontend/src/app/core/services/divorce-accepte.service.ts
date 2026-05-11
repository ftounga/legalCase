import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DivorceAccepteRequest,
  DivorceAccepteResponse,
} from '../models/divorce-accepte.model';

/**
 * SF-FA-10-02 : service HTTP pour l'outil F-FA-10 (divorce accepté FR).
 * Wrappe les endpoints `POST` / `GET /api/v1/case-files/{id}/divorce-accepte`
 * livrés par SF-FA-10-01 (PR #514).
 */
@Injectable({ providedIn: 'root' })
export class DivorceAccepteService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: DivorceAccepteRequest): Observable<DivorceAccepteResponse> {
    return this.http.post<DivorceAccepteResponse>(
      `/api/v1/case-files/${caseFileId}/divorce-accepte`,
      request,
    );
  }

  get(caseFileId: string): Observable<DivorceAccepteResponse> {
    return this.http.get<DivorceAccepteResponse>(
      `/api/v1/case-files/${caseFileId}/divorce-accepte`,
    );
  }

  /**
   * F-163 SF-163-02c — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-FA-10-divorce-accepte')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-FA-10-divorce-accepte';

  /**
   * F-163 SF-163-02c — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  calculateStandalone(request: DivorceAccepteRequest): Observable<DivorceAccepteResponse> {
    return this.http.post<DivorceAccepteResponse>(
      `/api/v1/simulators/${DivorceAccepteService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
