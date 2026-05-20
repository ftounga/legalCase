import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AtFedrisDeclarationRequest,
  AtFedrisDeclarationResponse,
} from '../models/at-fedris-declaration.model';

/**
 * SF-207-04b : wrapper HttpClient pour l'outil décisionnel "Déclaration AT
 * Fedris" (F-207, BE uniquement). Consomme l'API figée dans SF-207-04
 * backend (PR #1142).
 */
@Injectable({ providedIn: 'root' })
export class AtFedrisDeclarationService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/at-fedris-declaration`;
  }

  calculate(caseFileId: string, request: AtFedrisDeclarationRequest):
      Observable<AtFedrisDeclarationResponse> {
    return this.http.post<AtFedrisDeclarationResponse>(
      this.endpoint(caseFileId), request);
  }

  get(caseFileId: string): Observable<AtFedrisDeclarationResponse> {
    return this.http.get<AtFedrisDeclarationResponse>(this.endpoint(caseFileId));
  }
}
