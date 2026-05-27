import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

/**
 * F-JU-01 / SF-JU-01-05 — client Angular pour le dashboard
 * `/super-admin/jurisprudence-watch`.
 */

export interface JurisprudenceWatchFlag {
  id: string;
  toolId: string;
  brancheCalculId: string;
  arretEntrantRef: string;
  mappingActuelId: string | null;
  source: 'CRON' | 'USER_SIGNAL';
  confidenceScore: number | null;
  explication: string | null;
  statut: 'PENDING' | 'REVIEWED' | 'IGNORED';
  createdAt: string;
  reviewedAt: string | null;
  decision: 'REPLACE' | 'ADD' | 'IGNORE' | null;
  commentUser: string | null;
}

export interface JurisprudenceAuditLog {
  id: string;
  mappingId: string;
  action: string;
  actor: 'CRON' | 'SUPER_ADMIN';
  actorUserId: string | null;
  claudeConfidence: number | null;
  claudeReason: string | null;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export type ArbitrateDecision = 'REPLACE' | 'ADD' | 'IGNORE';

export interface JurisprudenceBootstrapEntry {
  toolId: string;
  brancheCalculId: string;
  motCleRecherche: string;
  juridictionFiltre?: string;
  dateMin?: string;
}

export interface JurisprudenceBootstrapResponse {
  entriesProcessed: number;
  mappingsCreated: number;
  entriesSkipped: number;
  durationMs: number;
}

/** SF-JU-01-10 — payload 202 Accepted du POST /bootstrap (lancement async). */
export interface JurisprudenceBootstrapJobStarted {
  jobId: string;
  entriesTotal: number;
  startedAt: string;
}

/** SF-JU-01-10 — payload du GET /bootstrap/jobs/{id} pour polling. */
export interface JurisprudenceBootstrapJobStatusResponse {
  jobId: string;
  status: 'RUNNING' | 'DONE' | 'FAILED';
  entriesTotal: number;
  entriesProcessed: number;
  mappingsCreated: number;
  entriesSkipped: number;
  durationMs: number | null;
  errorMessage: string | null;
  startedAt: string;
  completedAt: string | null;
}

@Injectable({ providedIn: 'root' })
export class JurisprudenceWatchAdminClientService {

  constructor(private http: HttpClient) {}

  private readonly base = '/api/v1/super-admin/jurisprudence-watch';

  listFlags(statut: 'PENDING' | 'REVIEWED' | 'IGNORED' = 'PENDING', page = 0, size = 20):
      Observable<Page<JurisprudenceWatchFlag>> {
    const params = new HttpParams()
      .set('statut', statut)
      .set('page', page)
      .set('size', size);
    return this.http.get<Page<JurisprudenceWatchFlag>>(`${this.base}/flags`, { params });
  }

  arbitrate(flagId: string, decision: ArbitrateDecision, comment?: string):
      Observable<JurisprudenceWatchFlag> {
    return this.http.post<JurisprudenceWatchFlag>(
      `${this.base}/flags/${flagId}/arbitrate`,
      { decision, comment });
  }

  listAuditLog(page = 0, size = 50): Observable<Page<JurisprudenceAuditLog>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<JurisprudenceAuditLog>>(`${this.base}/audit-log`, { params });
  }

  /**
   * SF-JU-01-10 — démarre un bootstrap async. Retourne immédiatement le jobId
   * à utiliser avec {@link getBootstrapJobStatus} pour le polling.
   */
  triggerBootstrap(entries: JurisprudenceBootstrapEntry[]):
      Observable<JurisprudenceBootstrapJobStarted> {
    return this.http.post<JurisprudenceBootstrapJobStarted>(
      `${this.base}/bootstrap`,
      { entries });
  }

  /** SF-JU-01-10 — récupère l'état courant d'un job de bootstrap async. */
  getBootstrapJobStatus(jobId: string):
      Observable<JurisprudenceBootstrapJobStatusResponse> {
    return this.http.get<JurisprudenceBootstrapJobStatusResponse>(
      `${this.base}/bootstrap/jobs/${jobId}`);
  }
}
