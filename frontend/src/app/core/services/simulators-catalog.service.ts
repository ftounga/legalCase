import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SimulatorsCatalogResponse } from '../models/simulators-catalog.model';

/**
 * F-163 / SF-163-01 — Service Angular qui wrappe l'endpoint
 * `GET /api/v1/simulators` (catalogue des outils décisionnels du workspace
 * courant).
 *
 * - Stateless, providedIn 'root'.
 * - Aucune transformation de la réponse — passe le DTO tel quel au composant.
 * - Les codes d'erreur (401 / 5xx) sont laissés au consommateur (la page
 *   gère bannière + MatSnackBar).
 */
@Injectable({ providedIn: 'root' })
export class SimulatorsCatalogService {
  private readonly http = inject(HttpClient);

  /**
   * Récupère le catalogue des simulateurs disponibles pour le workspace
   * courant. Le backend résout le workspace via le `CurrentUserResolver` —
   * aucun paramètre à passer.
   */
  getCatalog(): Observable<SimulatorsCatalogResponse> {
    return this.http.get<SimulatorsCatalogResponse>('/api/v1/simulators');
  }
}
