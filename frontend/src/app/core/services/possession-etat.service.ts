import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PossessionEtatRequest,
  PossessionEtatResponse,
} from '../models/possession-etat.model';

/**
 * SF-FA-18-08 : wrapper HttpClient pour l'outil décisionnel
 * "Possession d'état" (F-FA-18). FR uniquement.
 *
 * Consomme l'API figée dans SF-FA-18-07 (backend, mergé PR #670).
 */
@Injectable({ providedIn: 'root' })
export class PossessionEtatService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: PossessionEtatRequest):
      Observable<PossessionEtatResponse> {
    return this.http.post<PossessionEtatResponse>(
      `/api/v1/case-files/${caseFileId}/possession-etat-analysis`, request);
  }

  get(caseFileId: string): Observable<PossessionEtatResponse> {
    return this.http.get<PossessionEtatResponse>(
      `/api/v1/case-files/${caseFileId}/possession-etat-analysis`);
  }
}
