import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ClauseNonConcurrenceBeRequest,
  ClauseNonConcurrenceBeResponse,
} from '../models/clause-non-concurrence-be.model';

/**
 * SF-213-01b : wrapper HttpClient pour l'outil décisionnel
 * "Clause de non-concurrence BE" (F-213, BE uniquement). Consomme l'API
 * figée dans SF-213-01 backend.
 *
 * <p>Endpoint canonique (aligné dispatcher F-IA-04) :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/clause-non-concurrence-be}.</p>
 */
@Injectable({ providedIn: 'root' })
export class ClauseNonConcurrenceBeService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/clause-non-concurrence-be`;
  }

  calculate(
    caseFileId: string,
    request: ClauseNonConcurrenceBeRequest,
  ): Observable<ClauseNonConcurrenceBeResponse> {
    return this.http.post<ClauseNonConcurrenceBeResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<ClauseNonConcurrenceBeResponse> {
    return this.http.get<ClauseNonConcurrenceBeResponse>(this.endpoint(caseFileId));
  }
}
