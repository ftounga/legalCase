import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import {
  ToolJurisprudenceCitation,
  ToolJurisprudenceSignalRequest,
} from '../models/tool-jurisprudence-citation.model';

/**
 * F-JU-01 / SF-JU-01-04 — client HTTP pour les citations jurisprudentielles
 * d'un outil décisionnel.
 *
 * Backend : `GET /api/v1/tools/{toolId}/jurisprudence-citations?branch={brancheId}`
 * (SF-JU-01-01) + `POST .../signal` (SF-JU-01-04).
 */
@Injectable({ providedIn: 'root' })
export class ToolJurisprudenceClientService {

  constructor(private http: HttpClient) {}

  /**
   * Récupère 0 à 3 arrêts mappés pour un outil + branche. Retourne `of([])`
   * si `branchActive` est vide (économise un appel HTTP).
   */
  findByToolAndBranch(toolId: string, branchActive: string | null | undefined):
      Observable<ToolJurisprudenceCitation[]> {
    if (!toolId || !branchActive) {
      return of([]);
    }
    const params = new HttpParams().set('branch', branchActive);
    return this.http.get<ToolJurisprudenceCitation[]>(
      `/api/v1/tools/${encodeURIComponent(toolId)}/jurisprudence-citations`,
      { params });
  }

  /**
   * Signale un problème sur une citation. Crée un flag PENDING côté admin.
   */
  signalProblem(toolId: string, citationId: string, comment?: string): Observable<void> {
    const body: ToolJurisprudenceSignalRequest = comment ? { comment } : {};
    return this.http.post<void>(
      `/api/v1/tools/${encodeURIComponent(toolId)}/jurisprudence-citations/${citationId}/signal`,
      body);
  }
}
