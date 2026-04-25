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
}
