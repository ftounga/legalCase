import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RccBeIndemniteComplementaireRequest,
  RccBeIndemniteComplementaireResponse,
} from '../models/rcc-be-indemnite-complementaire.model';

/**
 * SF-207-07b : wrapper HttpClient pour l'outil décisionnel "RCC BE —
 * indemnité complémentaire" (F-207, BE uniquement). Consomme l'API figée
 * dans SF-207-07 backend (PR #1155).
 */
@Injectable({ providedIn: 'root' })
export class RccBeIndemniteComplementaireService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/rcc-be-indemnite-complementaire`;
  }

  calculate(caseFileId: string, request: RccBeIndemniteComplementaireRequest):
      Observable<RccBeIndemniteComplementaireResponse> {
    return this.http.post<RccBeIndemniteComplementaireResponse>(
      this.endpoint(caseFileId), request);
  }

  get(caseFileId: string): Observable<RccBeIndemniteComplementaireResponse> {
    return this.http.get<RccBeIndemniteComplementaireResponse>(this.endpoint(caseFileId));
  }
}
