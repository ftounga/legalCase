import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  IndivisionSuccessoraleRequest,
  IndivisionSuccessoraleResponse,
} from '../models/indivision-successorale.model';

/**
 * SF-FA-24-12 : wrapper HttpClient pour l'outil décisionnel "Indivision
 * successorale" (F-FA-24). FR uniquement — art. 815 à 832-2 + 1873-1 et s. +
 * 815-1 et s. Cciv. Consomme l'API figée dans SF-FA-24-11 (PR #681).
 */
@Injectable({ providedIn: 'root' })
export class IndivisionSuccessoraleService {

  constructor(private http: HttpClient) {}

  calculate(
    caseFileId: string,
    request: IndivisionSuccessoraleRequest,
  ): Observable<IndivisionSuccessoraleResponse> {
    return this.http.post<IndivisionSuccessoraleResponse>(
      `/api/v1/case-files/${caseFileId}/indivision-successorale-analysis`, request);
  }

  get(caseFileId: string): Observable<IndivisionSuccessoraleResponse> {
    return this.http.get<IndivisionSuccessoraleResponse>(
      `/api/v1/case-files/${caseFileId}/indivision-successorale-analysis`);
  }
}
