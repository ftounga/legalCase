import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  OutplacementBeRequest,
  OutplacementBeResponse,
} from '../models/outplacement-be-obligatoire-45.model';

/**
 * SF-207-08b : wrapper HttpClient pour l'outil décisionnel
 * "Outplacement BE obligatoire 45+" (F-207, BE uniquement). Consomme l'API
 * figée dans SF-207-08 backend (PR #1160).
 *
 * <p>Endpoint canonique aligné avec
 * {@code OutplacementBeController#@RequestMapping} :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/outplacement-be-obligatoire-45}.</p>
 */
@Injectable({ providedIn: 'root' })
export class OutplacementBeObligatoire45Service {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/outplacement-be-obligatoire-45`;
  }

  calculate(caseFileId: string, request: OutplacementBeRequest):
      Observable<OutplacementBeResponse> {
    return this.http.post<OutplacementBeResponse>(
      this.endpoint(caseFileId), request);
  }

  get(caseFileId: string): Observable<OutplacementBeResponse> {
    return this.http.get<OutplacementBeResponse>(this.endpoint(caseFileId));
  }
}
