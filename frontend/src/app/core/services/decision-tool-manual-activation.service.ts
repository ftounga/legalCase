import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/**
 * SF-238-02 — service d'activation manuelle d'un outil décisionnel par
 * l'avocat. Branché sur les endpoints SF-238-03.
 *
 * Au succès du POST, le composant appelant déclenche
 * {@code CaseDashboardRefreshService.triggerRefresh()} pour que le panel
 * F-IA-04 re-fetch la visibilité — l'outil migre du catalog vers
 * contextual.
 */
@Injectable({ providedIn: 'root' })
export class DecisionToolManualActivationService {
  private readonly http = inject(HttpClient);

  /**
   * POST /api/v1/case-files/{caseFileId}/decision-tools-visibility/manual-activations
   */
  activate(caseFileId: string, toolId: string): Observable<ManualToolActivationResponse> {
    return this.http.post<ManualToolActivationResponse>(
      `/api/v1/case-files/${caseFileId}/decision-tools-visibility/manual-activations`,
      { toolId },
    );
  }

  /**
   * DELETE /api/v1/case-files/{caseFileId}/decision-tools-visibility/manual-activations/{toolId}
   * Idempotent côté backend.
   */
  deactivate(caseFileId: string, toolId: string): Observable<void> {
    return this.http.delete<void>(
      `/api/v1/case-files/${caseFileId}/decision-tools-visibility/manual-activations/${encodeURIComponent(toolId)}`,
    );
  }
}

export interface ManualToolActivationResponse {
  id: string;
  toolId: string;
  /** ISO-8601 — émis par le backend. */
  activatedAt: string;
}
