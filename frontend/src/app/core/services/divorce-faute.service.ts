import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DivorceFauteRequest,
  DivorceFauteResponse,
} from '../models/divorce-faute.model';

/**
 * SF-FA-09-02 : wrapper HttpClient pour l'outil décisionnel
 * "Divorce pour faute" (F-FA-09, art. 242 Cciv).
 * Consomme l'API figée dans SF-FA-09-01 (backend PR #515).
 */
@Injectable({ providedIn: 'root' })
export class DivorceFauteService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: DivorceFauteRequest):
      Observable<DivorceFauteResponse> {
    return this.http.post<DivorceFauteResponse>(
      `/api/v1/case-files/${caseFileId}/divorce-faute`, request);
  }

  get(caseFileId: string): Observable<DivorceFauteResponse> {
    return this.http.get<DivorceFauteResponse>(
      `/api/v1/case-files/${caseFileId}/divorce-faute`);
  }
}
