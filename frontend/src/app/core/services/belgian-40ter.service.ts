import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Belgian40terRequest,
  Belgian40terResponse,
} from '../models/belgian-40ter.model';

/**
 * SF-IM-14-08 : wrapper HttpClient pour l'outil décisionnel
 * "Regroupement familial d'un Belge" (40ter — F-IM-14). BE uniquement.
 * Consomme l'API figée dans SF-IM-14-04 (backend mergée PR #511).
 */
@Injectable({ providedIn: 'root' })
export class Belgian40terService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: Belgian40terRequest):
      Observable<Belgian40terResponse> {
    return this.http.post<Belgian40terResponse>(
      `/api/v1/case-files/${caseFileId}/belgian-40ter`, request);
  }

  get(caseFileId: string): Observable<Belgian40terResponse> {
    return this.http.get<Belgian40terResponse>(
      `/api/v1/case-files/${caseFileId}/belgian-40ter`);
  }
}
