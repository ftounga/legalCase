import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  ProspectBootstrapRequest,
  ProspectBootstrapResponse,
} from './prospect-bootstrap.model';

/**
 * F-251 SF-251-04 — Service Angular qui consomme l'endpoint super-admin de
 * provisionnement express d'un compte prospect (contrat figé par SF-251-03).
 *
 * <ul>
 *   <li>{@code bootstrap} → {@code POST /api/v1/super-admin/prospect-bootstrap}</li>
 * </ul>
 *
 * <p>Sécurité : le filtrage super-admin est centralisé côté backend. Le
 * frontend se contente de relayer la réponse HTTP au composant appelant
 * (gestion 400/401/403/409/réseau côté UI).</p>
 */
@Injectable({ providedIn: 'root' })
export class ProspectBootstrapService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/super-admin/prospect-bootstrap';

  /**
   * Bootstrap un compte prospect en provisionnant user + workspace + période
   * d'évaluation.
   *
   * @returns 201 — métadonnées du compte créé (userId, workspaceId, expiresAt).
   */
  bootstrap(req: ProspectBootstrapRequest): Observable<ProspectBootstrapResponse> {
    return this.http.post<ProspectBootstrapResponse>(this.baseUrl, req);
  }
}
