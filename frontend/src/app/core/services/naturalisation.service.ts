import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { NaturalisationRequest, NaturalisationResponse } from '../models/naturalisation.model';

/**
 * SF-IM-13-02 : wrapper HttpClient pour l'outil décisionnel "Naturalisation
 * française" (F-IM-13). FR uniquement (Code civil art. 21+).
 * Consomme l'API figée dans SF-IM-13-01 (backend, mergé PR #639).
 */
@Injectable({ providedIn: 'root' })
export class NaturalisationService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: NaturalisationRequest): Observable<NaturalisationResponse> {
    return this.http.post<NaturalisationResponse>(
      `/api/v1/case-files/${caseFileId}/naturalisation-analysis`, request);
  }

  get(caseFileId: string): Observable<NaturalisationResponse> {
    return this.http.get<NaturalisationResponse>(
      `/api/v1/case-files/${caseFileId}/naturalisation-analysis`);
  }
}
