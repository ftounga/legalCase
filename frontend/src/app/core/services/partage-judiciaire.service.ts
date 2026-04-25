import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PartageJudiciaireRequest,
  PartageJudiciaireResponse,
} from '../models/partage-judiciaire.model';

/**
 * SF-FA-17-02 : wrapper HttpClient pour l'outil décisionnel "Partage judiciaire"
 * (F-FA-17). FR uniquement.
 * Consomme l'API figée dans SF-FA-17-01 (backend, mergé PR #636).
 */
@Injectable({ providedIn: 'root' })
export class PartageJudiciaireService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: PartageJudiciaireRequest): Observable<PartageJudiciaireResponse> {
    return this.http.post<PartageJudiciaireResponse>(
      `/api/v1/case-files/${caseFileId}/partage-judiciaire-analysis`, request);
  }

  get(caseFileId: string): Observable<PartageJudiciaireResponse> {
    return this.http.get<PartageJudiciaireResponse>(
      `/api/v1/case-files/${caseFileId}/partage-judiciaire-analysis`);
  }
}
