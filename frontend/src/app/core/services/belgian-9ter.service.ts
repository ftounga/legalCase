import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Belgian9terRequest,
  Belgian9terResponse,
} from '../models/belgian-9ter.model';

/**
 * SF-IM-14-06 : wrapper HttpClient pour l'outil décisionnel
 * "Régularisation 9ter médical" (F-IM-14). BE uniquement.
 * Consomme l'API figée dans SF-IM-14-02 (backend, PR #509).
 */
@Injectable({ providedIn: 'root' })
export class Belgian9terService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: Belgian9terRequest):
      Observable<Belgian9terResponse> {
    return this.http.post<Belgian9terResponse>(
      `/api/v1/case-files/${caseFileId}/belgian-9ter`, request);
  }

  get(caseFileId: string): Observable<Belgian9terResponse> {
    return this.http.get<Belgian9terResponse>(
      `/api/v1/case-files/${caseFileId}/belgian-9ter`);
  }
}
