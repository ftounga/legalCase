import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AdoptionRequest,
  AdoptionResponse,
} from '../models/adoption.model';

/**
 * SF-FA-18-10 : wrapper HttpClient pour l'outil décisionnel
 * "Adoption" (F-FA-18). FR uniquement.
 *
 * Consomme l'API figée dans SF-FA-18-09 (backend, mergé PR #677).
 */
@Injectable({ providedIn: 'root' })
export class AdoptionService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: AdoptionRequest):
      Observable<AdoptionResponse> {
    return this.http.post<AdoptionResponse>(
      `/api/v1/case-files/${caseFileId}/adoption-analysis`, request);
  }

  get(caseFileId: string): Observable<AdoptionResponse> {
    return this.http.get<AdoptionResponse>(
      `/api/v1/case-files/${caseFileId}/adoption-analysis`);
  }
}
