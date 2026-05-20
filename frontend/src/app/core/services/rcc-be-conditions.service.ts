import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RccBeConditionsRequest,
  RccBeConditionsResponse,
} from '../models/rcc-be-conditions.model';

/**
 * SF-207-06b : wrapper HttpClient pour l'outil décisionnel "RCC BE — conditions
 * d'éligibilité" (F-207, BE uniquement). Consomme l'API figée dans SF-207-06
 * backend (PR #1151).
 */
@Injectable({ providedIn: 'root' })
export class RccBeConditionsService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/rcc-be-conditions`;
  }

  calculate(caseFileId: string, request: RccBeConditionsRequest):
      Observable<RccBeConditionsResponse> {
    return this.http.post<RccBeConditionsResponse>(
      this.endpoint(caseFileId), request);
  }

  get(caseFileId: string): Observable<RccBeConditionsResponse> {
    return this.http.get<RccBeConditionsResponse>(this.endpoint(caseFileId));
  }
}
