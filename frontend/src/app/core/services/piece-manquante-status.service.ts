import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  PieceManquanteStatus,
  PieceManquanteStatusPayload,
  PieceManquanteStatutValue,
} from '../models/piece-manquante-status.model';

/**
 * F-194 SF-194-02 — Persistance du statut décidé par l'avocat sur une pièce
 * manquante listée par la synthèse.
 *
 * Endpoint backend (importé de SF-194-01) :
 *   {@code PUT /api/v1/case-files/{id}/pieces-manquantes/status}
 *   body : <code>{ pieceLibelleOriginal, statut, raisonNonApp?, destinataire? }</code>
 *
 * <p>Cohérence F-176 stricte : le PUT est un acte pur côté backend (pas de
 * recompute, pas de side-effect). La matérialisation pièce → outil
 * (`PieceManquanteAlignment`) ne se fait qu'au prochain run de Synthèse
 * enrichie via l'event SSE {@code ENRICHED_ANALYSIS DONE}. Aucun refresh
 * côté frontend après PUT.</p>
 *
 * <p>Fail-fast : 400 / 401 / 403 / 404 / 5xx remontent au composant appelant
 * qui doit afficher un snackbar et rollback son optimistic update (CA-14).</p>
 */
@Injectable({ providedIn: 'root' })
export class PieceManquanteStatusService {
  private readonly baseUrl = '/api/v1/case-files';

  constructor(private http: HttpClient) {}

  update(
    caseFileId: string,
    payload: PieceManquanteStatusPayload,
  ): Observable<PieceManquanteStatus> {
    return this.http.put<PieceManquanteStatus>(
      `${this.baseUrl}/${caseFileId}/pieces-manquantes/status`,
      payload,
    );
  }

  /**
   * Helper de convenance : permet aux composants d'appeler `updateStatus(id, libelle, 'OBTENUE')`
   * sans avoir à construire le payload à chaque fois.
   */
  updateStatus(
    caseFileId: string,
    pieceLibelleOriginal: string,
    statut: PieceManquanteStatutValue,
    options?: { raisonNonApp?: string | null; destinataire?: string | null; documentId?: string | null },
  ): Observable<PieceManquanteStatus> {
    return this.update(caseFileId, {
      pieceLibelleOriginal,
      statut,
      raisonNonApp: options?.raisonNonApp ?? null,
      destinataire: options?.destinataire ?? null,
      // SF-194-04 — document reçu lié (optionnel). Backend l'ignore si statut ≠ OBTENUE.
      documentId: options?.documentId ?? null,
    });
  }
}
