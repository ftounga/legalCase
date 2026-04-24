import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  TravailDissimuleRequest,
  TravailDissimuleResponse,
} from '../models/travail-dissimule.model';

/**
 * SF-DT-21-02 : wrapper HttpClient pour l'outil décisionnel
 * "Indemnité forfaitaire pour travail dissimulé" (F-DT-21).
 * Consomme l'API figée dans SF-DT-21-01 (backend).
 */
@Injectable({ providedIn: 'root' })
export class TravailDissimuleService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: TravailDissimuleRequest):
      Observable<TravailDissimuleResponse> {
    return this.http.post<TravailDissimuleResponse>(
      `/api/v1/case-files/${caseFileId}/travail-dissimule`, request);
  }

  get(caseFileId: string): Observable<TravailDissimuleResponse> {
    return this.http.get<TravailDissimuleResponse>(
      `/api/v1/case-files/${caseFileId}/travail-dissimule`);
  }
}
