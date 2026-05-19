import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DashboardAuditReport } from '../models/dashboard-audit.model';

/**
 * F-180 SF-180-02 — accès aux endpoints super-admin d'audit dashboard tiles F-167.
 * Service distinct de {@link BacklogAdminService} : l'audit dashboard n'a aucun
 * lien fonctionnel avec la sync backlog MD→DB.
 */
@Injectable({ providedIn: 'root' })
export class DashboardAuditService {
  private readonly base = '/api/v1/super-admin/dashboard-audit';

  constructor(private http: HttpClient) {}

  /** Dernier rapport d'audit (le backend déclenche un run si aucun n'existe). */
  getLatest(): Observable<DashboardAuditReport> {
    return this.http.get<DashboardAuditReport>(`${this.base}/latest`);
  }

  /** Force un audit immédiat (bouton « Relancer maintenant »). */
  runAudit(): Observable<DashboardAuditReport> {
    return this.http.post<DashboardAuditReport>(`${this.base}/run`, {});
  }
}
