import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  HarcelementNulliteRequest,
  HarcelementNulliteResponse,
} from '../models/harcelement-nullite.model';

/**
 * SF-DT-11-02 : wrapper HttpClient pour l'outil décisionnel
 * "Indemnité minimum licenciement nul — harcèlement" (F-DT-11).
 * Consomme l'API figée dans SF-DT-11-01 (backend).
 */
@Injectable({ providedIn: 'root' })
export class HarcelementNulliteService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: HarcelementNulliteRequest):
      Observable<HarcelementNulliteResponse> {
    return this.http.post<HarcelementNulliteResponse>(
      `/api/v1/case-files/${caseFileId}/harcelement-licenciement-nul`, request);
  }

  get(caseFileId: string): Observable<HarcelementNulliteResponse> {
    return this.http.get<HarcelementNulliteResponse>(
      `/api/v1/case-files/${caseFileId}/harcelement-licenciement-nul`);
  }
}
