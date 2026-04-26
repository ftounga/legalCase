import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RecherchePaterniteRequest,
  RecherchePaterniteResponse,
} from '../models/recherche-paternite.model';

/**
 * SF-FA-18-06 : wrapper HttpClient pour l'outil décisionnel
 * "Action en recherche de paternité" (F-FA-18). FR uniquement.
 *
 * Consomme l'API figée dans SF-FA-18-05 (backend, mergé PR #664).
 */
@Injectable({ providedIn: 'root' })
export class RecherchePaterniteService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: RecherchePaterniteRequest):
      Observable<RecherchePaterniteResponse> {
    return this.http.post<RecherchePaterniteResponse>(
      `/api/v1/case-files/${caseFileId}/recherche-paternite-analysis`, request);
  }

  get(caseFileId: string): Observable<RecherchePaterniteResponse> {
    return this.http.get<RecherchePaterniteResponse>(
      `/api/v1/case-files/${caseFileId}/recherche-paternite-analysis`);
  }
}
