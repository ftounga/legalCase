import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  OutplacementBeGeneral30semRequest,
  OutplacementBeGeneral30semResponse,
} from '../models/outplacement-be-general-30sem.model';

/**
 * SF-219-05b : wrapper HttpClient pour l'outil décisionnel
 * « Outplacement BE général au titre du régime préavis ≥ 30 semaines »
 * (F-219, BE uniquement — Loi 05/09/2001 art. 11 + AR 21/10/2007).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/outplacement-be-general-30sem}.</p>
 *
 * <p>Outil <b>BE-only</b> — la France a un dispositif d'outplacement
 * distinct (Cap emploi / CSP). Le gate {@code workspaceCountry} est porté
 * côté composant ; le backend renvoie 404 pour FR par cohérence.</p>
 */
@Injectable({ providedIn: 'root' })
export class OutplacementBeGeneral30semService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/outplacement-be-general-30sem`;
  }

  calculate(
    caseFileId: string,
    request: OutplacementBeGeneral30semRequest,
  ): Observable<OutplacementBeGeneral30semResponse> {
    return this.http.post<OutplacementBeGeneral30semResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<OutplacementBeGeneral30semResponse> {
    return this.http.get<OutplacementBeGeneral30semResponse>(
      this.endpoint(caseFileId),
    );
  }
}
