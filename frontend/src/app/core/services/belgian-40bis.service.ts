import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Belgian40bisRequest,
  Belgian40bisResponse,
} from '../models/belgian-40bis.model';

/**
 * SF-IM-14-07 : wrapper HttpClient pour l'outil décisionnel
 * "Regroupement familial citoyen UE — art. 40bis Loi 15/12/1980"
 * (F-IM-14). BE uniquement. Consomme l'API figée en SF-IM-14-03
 * (backend, PR #510).
 */
@Injectable({ providedIn: 'root' })
export class Belgian40bisService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: Belgian40bisRequest):
      Observable<Belgian40bisResponse> {
    return this.http.post<Belgian40bisResponse>(
      `/api/v1/case-files/${caseFileId}/belgian-40bis`, request);
  }

  get(caseFileId: string): Observable<Belgian40bisResponse> {
    return this.http.get<Belgian40bisResponse>(
      `/api/v1/case-files/${caseFileId}/belgian-40bis`);
  }
}
