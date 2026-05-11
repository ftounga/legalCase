import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ChangementEtatCivilRequest,
  ChangementEtatCivilResponse,
} from '../models/changement-etat-civil.model';

/**
 * SF-FA-26-02 : wrapper HttpClient pour l'outil décisionnel
 * "Changement d'état civil" (F-FA-26, FRANCE uniquement, art. 60 / 61-1 et s. /
 * 61-5 et s. Cciv).
 *
 * Consomme l'API figée par SF-FA-26-01 (backend, parallèle) :
 *   - POST /api/v1/case-files/{caseFileId}/changement-etat-civil → calcul + persistance
 *   - GET  /api/v1/case-files/{caseFileId}/changement-etat-civil → récupération
 */
@Injectable({ providedIn: 'root' })
export class ChangementEtatCivilService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: ChangementEtatCivilRequest):
      Observable<ChangementEtatCivilResponse> {
    return this.http.post<ChangementEtatCivilResponse>(
      `/api/v1/case-files/${caseFileId}/changement-etat-civil`, request);
  }

  get(caseFileId: string): Observable<ChangementEtatCivilResponse> {
    return this.http.get<ChangementEtatCivilResponse>(
      `/api/v1/case-files/${caseFileId}/changement-etat-civil`);
  }

  /**
   * F-163 SF-163-02c — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-FA-26-changement-etat-civil')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-FA-26-changement-etat-civil';

  /**
   * F-163 SF-163-02c — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  calculateStandalone(request: ChangementEtatCivilRequest): Observable<ChangementEtatCivilResponse> {
    return this.http.post<ChangementEtatCivilResponse>(
      `/api/v1/simulators/${ChangementEtatCivilService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }
}
