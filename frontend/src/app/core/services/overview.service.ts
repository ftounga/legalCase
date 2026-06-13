import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OverviewResponse } from '../models/overview.model';

/**
 * F-289 / SF-289-01 — Accès à l'agrégat « Vue d'ensemble du dossier ».
 *
 * Lecture seule : un unique GET vers l'endpoint d'agrégation backend, lui-même
 * `fail-open` (une source qui échoue est omise sans casser la réponse). Aucune
 * écriture ici — les actions (validate, generate, etc.) passent par leurs
 * services dédiés / le routage inter-onglets du `case-file-detail`.
 */
@Injectable({ providedIn: 'root' })
export class OverviewService {
  constructor(private http: HttpClient) {}

  /** `GET /api/v1/case-files/{caseFileId}/overview` — pilotage + attention + fil. */
  getOverview(caseFileId: string): Observable<OverviewResponse> {
    return this.http.get<OverviewResponse>(`/api/v1/case-files/${caseFileId}/overview`);
  }
}
