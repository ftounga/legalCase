import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CongeEducationPayeRegionRequest,
  CongeEducationPayeRegionResponse,
} from '../models/conge-education-paye-region.model';

/**
 * SF-219-11b : wrapper HttpClient pour l'outil décisionnel
 * « Congé-éducation payé régionalisé » (F-219, BE uniquement —
 * Loi du 22/01/1985 contenant des dispositions sociales, Section 6 —
 * régionalisée 2014 — WBR / FLA VOV / BXL).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/conge-education-paye-region}.</p>
 *
 * <p>Outil <b>BE-only</b> — pas d'équivalent strict FR (financement de
 * la formation salariée FR : CPF / Pro-A / Plan de développement des
 * compétences, art. L. 6111-1 et s. C. trav. FR — régime distinct).
 * Le gate {@code workspaceCountry} est porté côté composant ; le
 * backend renvoie 404 pour FR par cohérence.</p>
 */
@Injectable({ providedIn: 'root' })
export class CongeEducationPayeRegionService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/conge-education-paye-region`;
  }

  calculate(
    caseFileId: string,
    request: CongeEducationPayeRegionRequest,
  ): Observable<CongeEducationPayeRegionResponse> {
    return this.http.post<CongeEducationPayeRegionResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<CongeEducationPayeRegionResponse> {
    return this.http.get<CongeEducationPayeRegionResponse>(
      this.endpoint(caseFileId),
    );
  }
}
