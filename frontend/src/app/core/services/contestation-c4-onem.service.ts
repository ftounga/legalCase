import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ContestationC4OnemRequest,
  ContestationC4OnemResponse,
} from '../models/contestation-c4-onem.model';

/**
 * SF-207-03b : wrapper HttpClient pour l'outil décisionnel "Contestation
 * C4 ONEM" (F-207, BE uniquement). Consomme l'API figée dans SF-207-03
 * backend (PR #1137).
 */
@Injectable({ providedIn: 'root' })
export class ContestationC4OnemService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/contestation-c4-onem`;
  }

  calculate(caseFileId: string, request: ContestationC4OnemRequest):
      Observable<ContestationC4OnemResponse> {
    return this.http.post<ContestationC4OnemResponse>(
      this.endpoint(caseFileId), request);
  }

  get(caseFileId: string): Observable<ContestationC4OnemResponse> {
    return this.http.get<ContestationC4OnemResponse>(this.endpoint(caseFileId));
  }
}
