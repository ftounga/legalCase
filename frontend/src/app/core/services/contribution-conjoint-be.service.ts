import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ContributionConjointBeRequest,
  ContributionConjointBeResponse,
} from '../models/contribution-conjoint-be.model';

/**
 * SF-217-09 : wrapper HttpClient pour l'outil décisionnel "Pension alimentaire
 * entre ex-époux (Belgique)" (Vague 2 Famille BE — CC art. 301). Consomme
 * l'API figée dans SF-217-08 (backend).
 */
@Injectable({ providedIn: 'root' })
export class ContributionConjointBeService {
  constructor(private http: HttpClient) {}

  /** POST — analyse et persiste le droit à pension du dossier. */
  calculate(
    caseFileId: string,
    request: ContributionConjointBeRequest,
  ): Observable<ContributionConjointBeResponse> {
    return this.http.post<ContributionConjointBeResponse>(
      `/api/v1/case-files/${caseFileId}/contribution-conjoint-be`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<ContributionConjointBeResponse> {
    return this.http.get<ContributionConjointBeResponse>(
      `/api/v1/case-files/${caseFileId}/contribution-conjoint-be`,
    );
  }
}
