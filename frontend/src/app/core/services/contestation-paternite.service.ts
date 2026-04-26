import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ContestationPaterniteRequest,
  ContestationPaterniteResponse,
} from '../models/contestation-paternite.model';

/**
 * SF-FA-18-04 : wrapper HttpClient pour l'outil décisionnel
 * "Contestation de paternité" (F-FA-18). FR uniquement.
 *
 * Consomme l'API figée dans SF-FA-18-03 (backend, mergé PR #660).
 */
@Injectable({ providedIn: 'root' })
export class ContestationPaterniteService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: ContestationPaterniteRequest):
      Observable<ContestationPaterniteResponse> {
    return this.http.post<ContestationPaterniteResponse>(
      `/api/v1/case-files/${caseFileId}/contestation-paternite-analysis`, request);
  }

  get(caseFileId: string): Observable<ContestationPaterniteResponse> {
    return this.http.get<ContestationPaterniteResponse>(
      `/api/v1/case-files/${caseFileId}/contestation-paternite-analysis`);
  }
}
