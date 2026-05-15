import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ProcedureStage,
  ProcedureStageOptions,
  ProcedureStageUpdate
} from '../models/procedure-stage.model';

/**
 * F-243 / SF-243-02 — Accès HTTP au stade procédural du dossier.
 * Consomme le contrat API figé de SF-243-01 (endpoints A/B/C).
 */
@Injectable({ providedIn: 'root' })
export class ProcedureStageService {
  constructor(private http: HttpClient) {}

  /** Endpoint A — référentiel des valeurs pour un domaine + pays. */
  getOptions(domain: string, country: string): Observable<ProcedureStageOptions> {
    const params = new HttpParams()
      .set('domain', domain)
      .set('country', country);
    return this.http.get<ProcedureStageOptions>('/api/v1/procedure-stage/options', { params });
  }

  /** Endpoint B — stade procédural courant d'un dossier. */
  get(caseFileId: string): Observable<ProcedureStage> {
    return this.http.get<ProcedureStage>(`/api/v1/case-files/${caseFileId}/procedure-stage`);
  }

  /** Endpoint C — met à jour le stade procédural (champs nullable). */
  update(caseFileId: string, payload: ProcedureStageUpdate): Observable<ProcedureStage> {
    return this.http.patch<ProcedureStage>(
      `/api/v1/case-files/${caseFileId}/procedure-stage`,
      payload
    );
  }
}
