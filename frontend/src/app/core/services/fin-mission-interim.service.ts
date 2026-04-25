import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  FinMissionInterimRequest,
  FinMissionInterimResponse,
} from '../models/fin-mission-interim.model';

/**
 * SF-DT-18-02 : wrapper HttpClient pour l'outil décisionnel
 * "Indemnité fin mission intérim" (F-DT-18). Consomme l'API figée dans
 * SF-DT-18-01 (backend, parallèle).
 */
@Injectable({ providedIn: 'root' })
export class FinMissionInterimService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: FinMissionInterimRequest):
      Observable<FinMissionInterimResponse> {
    return this.http.post<FinMissionInterimResponse>(
      `/api/v1/case-files/${caseFileId}/fin-mission-interim`, request);
  }

  get(caseFileId: string): Observable<FinMissionInterimResponse> {
    return this.http.get<FinMissionInterimResponse>(
      `/api/v1/case-files/${caseFileId}/fin-mission-interim`);
  }
}
