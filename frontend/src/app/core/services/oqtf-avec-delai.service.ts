import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  OqtfAvecDelaiRequest,
  OqtfAvecDelaiResponse,
} from '../models/oqtf-avec-delai.model';

/**
 * SF-IM-08-02 : wrapper HttpClient pour l'outil décisionnel
 * "OQTF avec délai de départ volontaire FR" (F-IM-08). FR uniquement.
 * Consomme l'API figée dans SF-IM-08-01 (backend).
 */
@Injectable({ providedIn: 'root' })
export class OqtfAvecDelaiService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: OqtfAvecDelaiRequest):
      Observable<OqtfAvecDelaiResponse> {
    return this.http.post<OqtfAvecDelaiResponse>(
      `/api/v1/case-files/${caseFileId}/oqtf-avec-delai`, request);
  }

  get(caseFileId: string): Observable<OqtfAvecDelaiResponse> {
    return this.http.get<OqtfAvecDelaiResponse>(
      `/api/v1/case-files/${caseFileId}/oqtf-avec-delai`);
  }
}
