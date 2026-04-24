import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  InaptitudeRequest,
  InaptitudeResponse,
} from '../models/inaptitude.model';

/**
 * SF-DT-15-02 : wrapper HttpClient pour l'outil décisionnel
 * "Licenciement pour inaptitude" (F-DT-15). FR + BE.
 * Consomme l'API figée dans SF-DT-15-01 (backend).
 */
@Injectable({ providedIn: 'root' })
export class InaptitudeService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: InaptitudeRequest):
      Observable<InaptitudeResponse> {
    return this.http.post<InaptitudeResponse>(
      `/api/v1/case-files/${caseFileId}/inaptitude`, request);
  }

  get(caseFileId: string): Observable<InaptitudeResponse> {
    return this.http.get<InaptitudeResponse>(
      `/api/v1/case-files/${caseFileId}/inaptitude`);
  }
}
