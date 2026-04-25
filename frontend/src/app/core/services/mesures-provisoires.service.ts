import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  MesuresProvisoiresRequest,
  MesuresProvisoiresResponse,
} from '../models/mesures-provisoires.model';

/**
 * SF-FA-12-02 : wrapper HttpClient pour l'outil décisionnel
 * "Mesures provisoires" (F-FA-12, art. 254-256 Cciv).
 * Consomme l'API figée dans SF-FA-12-01 (backend mergé PR #573).
 */
@Injectable({ providedIn: 'root' })
export class MesuresProvisoiresService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: MesuresProvisoiresRequest):
      Observable<MesuresProvisoiresResponse> {
    return this.http.post<MesuresProvisoiresResponse>(
      `/api/v1/case-files/${caseFileId}/mesures-provisoires`, request);
  }

  get(caseFileId: string): Observable<MesuresProvisoiresResponse> {
    return this.http.get<MesuresProvisoiresResponse>(
      `/api/v1/case-files/${caseFileId}/mesures-provisoires`);
  }
}
