import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PartageSuccessoralRequest,
  PartageSuccessoralResponse,
} from '../models/partage-successoral.model';

/**
 * SF-FA-24-10 : wrapper HttpClient pour l'outil décisionnel "Partage
 * successoral" (F-FA-24). FR uniquement — art. 815-840 Cciv + 1364 CPC.
 * Consomme l'API figée dans SF-FA-24-09 (PR #680).
 */
@Injectable({ providedIn: 'root' })
export class PartageSuccessoralService {

  constructor(private http: HttpClient) {}

  calculate(
    caseFileId: string,
    request: PartageSuccessoralRequest,
  ): Observable<PartageSuccessoralResponse> {
    return this.http.post<PartageSuccessoralResponse>(
      `/api/v1/case-files/${caseFileId}/partage-successoral-analysis`,
      request,
    );
  }

  get(caseFileId: string): Observable<PartageSuccessoralResponse> {
    return this.http.get<PartageSuccessoralResponse>(
      `/api/v1/case-files/${caseFileId}/partage-successoral-analysis`);
  }
}
