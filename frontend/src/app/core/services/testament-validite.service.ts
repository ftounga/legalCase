import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  TestamentValiditeRequest,
  TestamentValiditeResponse,
} from '../models/testament-validite.model';

/**
 * SF-FA-24-04 : wrapper HttpClient pour l'outil décisionnel "Validité
 * testament" (F-FA-24). FR uniquement — art. 967-1035 + 901-911 + 920+ Cciv.
 * Consomme l'API figée dans SF-FA-24-03 (backend, mergé PR #661).
 */
@Injectable({ providedIn: 'root' })
export class TestamentValiditeService {

  constructor(private http: HttpClient) {}

  calculate(
    caseFileId: string,
    request: TestamentValiditeRequest,
  ): Observable<TestamentValiditeResponse> {
    return this.http.post<TestamentValiditeResponse>(
      `/api/v1/case-files/${caseFileId}/testament-validite-analysis`, request);
  }

  get(caseFileId: string): Observable<TestamentValiditeResponse> {
    return this.http.get<TestamentValiditeResponse>(
      `/api/v1/case-files/${caseFileId}/testament-validite-analysis`);
  }
}
