import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DivorceDesunionBeRequest,
  DivorceDesunionBeResponse,
} from '../models/divorce-desunion-be.model';

/**
 * SF-FA-11-02 : wrapper HttpClient pour l'outil décisionnel
 * "Divorce pour désunion irrémédiable" (Code civil belge, art. 229).
 * Consomme l'API figée dans SF-FA-11-01 (backend).
 *
 * BELGIQUE uniquement.
 */
@Injectable({ providedIn: 'root' })
export class DivorceDesunionBeService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: DivorceDesunionBeRequest):
      Observable<DivorceDesunionBeResponse> {
    return this.http.post<DivorceDesunionBeResponse>(
      `/api/v1/case-files/${caseFileId}/desunion-irremediable-be`, request);
  }

  get(caseFileId: string): Observable<DivorceDesunionBeResponse> {
    return this.http.get<DivorceDesunionBeResponse>(
      `/api/v1/case-files/${caseFileId}/desunion-irremediable-be`);
  }
}
