import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RefereTribunalTravailBeRequest,
  RefereTribunalTravailBeResponse,
} from '../models/refere-tribunal-travail-be.model';

/**
 * SF-207-05b : wrapper HttpClient pour l'outil décisionnel "Référé tribunal
 * du travail BE" (F-207, BE uniquement). Consomme l'API figée dans SF-207-05
 * backend (PR #1147).
 */
@Injectable({ providedIn: 'root' })
export class RefereTribunalTravailBeService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/refere-tribunal-travail-be`;
  }

  calculate(caseFileId: string, request: RefereTribunalTravailBeRequest):
      Observable<RefereTribunalTravailBeResponse> {
    return this.http.post<RefereTribunalTravailBeResponse>(
      this.endpoint(caseFileId), request);
  }

  get(caseFileId: string): Observable<RefereTribunalTravailBeResponse> {
    return this.http.get<RefereTribunalTravailBeResponse>(this.endpoint(caseFileId));
  }
}
