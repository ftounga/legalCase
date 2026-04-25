import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AvantagesConventionnelsBeRequest,
  AvantagesConventionnelsBeResponse,
} from '../models/avantages-conventionnels-be.model';

/**
 * SF-DT-28-02 : wrapper HttpClient pour l'outil décisionnel
 * "Avantages conventionnels BE" (F-DT-28). BE uniquement.
 * Consomme l'API figée dans SF-DT-28-01 (backend).
 */
@Injectable({ providedIn: 'root' })
export class AvantagesConventionnelsBeService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: AvantagesConventionnelsBeRequest):
      Observable<AvantagesConventionnelsBeResponse> {
    return this.http.post<AvantagesConventionnelsBeResponse>(
      `/api/v1/case-files/${caseFileId}/avantages-conventionnels-be`, request);
  }

  get(caseFileId: string): Observable<AvantagesConventionnelsBeResponse> {
    return this.http.get<AvantagesConventionnelsBeResponse>(
      `/api/v1/case-files/${caseFileId}/avantages-conventionnels-be`);
  }
}
