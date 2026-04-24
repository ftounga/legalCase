import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  IndemnitePrecariteCddRequest,
  IndemnitePrecariteCddResponse,
} from '../models/indemnite-precarite-cdd.model';

/**
 * SF-DT-17-02 : wrapper HttpClient pour l'outil décisionnel
 * "Indemnité précarité CDD" (F-DT-17). Consomme l'API figée dans
 * SF-DT-17-01 (backend).
 */
@Injectable({ providedIn: 'root' })
export class IndemnitePrecariteCddService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: IndemnitePrecariteCddRequest):
      Observable<IndemnitePrecariteCddResponse> {
    return this.http.post<IndemnitePrecariteCddResponse>(
      `/api/v1/case-files/${caseFileId}/cdd-indemnite-precarite`, request);
  }

  get(caseFileId: string): Observable<IndemnitePrecariteCddResponse> {
    return this.http.get<IndemnitePrecariteCddResponse>(
      `/api/v1/case-files/${caseFileId}/cdd-indemnite-precarite`);
  }
}
