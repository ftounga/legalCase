import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { Workspace, WorkspaceCreateResponse } from '../models/workspace.model';

@Injectable({ providedIn: 'root' })
export class WorkspaceService {
  private readonly apiUrl = '/api/v1/workspaces';

  private readonly workspaceSwitchedSource = new Subject<void>();
  readonly workspaceSwitched$ = this.workspaceSwitchedSource.asObservable();

  constructor(private http: HttpClient) {}

  getCurrentWorkspace(): Observable<Workspace> {
    return this.http.get<Workspace>(`${this.apiUrl}/current`);
  }

  /**
   * Création legacy d'un workspace (onboarding initial, domaine/pays) — réutilisé par F-154.
   * NE PAS confondre avec {@link createWorkspaceWithPlan} (F-156) qui crée un workspace
   * supplémentaire payant en `PENDING_PAYMENT` + redirection Stripe Checkout.
   */
  createWorkspace(name: string, legalDomain: string, country: string): Observable<Workspace> {
    return this.http.post<Workspace>(this.apiUrl, { name, legalDomain, country });
  }

  /**
   * F-156 SF-156-02 — crée un workspace supplémentaire en `PENDING_PAYMENT`
   * et retourne l'URL Stripe Checkout vers laquelle rediriger le navigateur.
   * Contrat figé SF-156-01.
   * - 201 → { workspaceId, stripeCheckoutUrl, status: 'PENDING_PAYMENT' }
   * - 403 PLAN_REQUIRED  (OWNER FREE/SOLO)
   * - 400 PLAN_INVALID   (plan ∉ {TEAM, PRO})
   */
  createWorkspaceWithPlan(name: string, plan: 'TEAM' | 'PRO'): Observable<WorkspaceCreateResponse> {
    return this.http.post<WorkspaceCreateResponse>(this.apiUrl, { name, plan });
  }

  listWorkspaces(): Observable<Workspace[]> {
    return this.http.get<Workspace[]>(this.apiUrl);
  }

  switchWorkspace(id: string): Observable<Workspace> {
    return this.http.post<Workspace>(`${this.apiUrl}/${id}/switch`, {});
  }

  notifyWorkspaceSwitched(): void {
    this.workspaceSwitchedSource.next();
  }
}
