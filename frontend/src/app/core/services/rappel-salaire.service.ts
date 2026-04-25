import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RappelSalaireRequest,
  RappelSalaireResponse,
} from '../models/rappel-salaire.model';

/**
 * SF-DT-20-02 : wrapper HttpClient pour l'outil décisionnel
 * "Rappel de salaire FR" (F-DT-20, FR uniquement).
 * Consomme l'API figée dans SF-DT-20-01 (backend, PR #584).
 */
@Injectable({ providedIn: 'root' })
export class RappelSalaireService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: RappelSalaireRequest):
      Observable<RappelSalaireResponse> {
    return this.http.post<RappelSalaireResponse>(
      `/api/v1/case-files/${caseFileId}/rappel-salaire`, request);
  }

  get(caseFileId: string): Observable<RappelSalaireResponse> {
    return this.http.get<RappelSalaireResponse>(
      `/api/v1/case-files/${caseFileId}/rappel-salaire`);
  }
}
