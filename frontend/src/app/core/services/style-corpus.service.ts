import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  StyleCorpusDocumentStatus,
  StyleCorpusDocumentSummary,
} from '../models/style-corpus.model';

/**
 * Corps de la réponse `POST .../style-corpus/documents` (`202`).
 *
 * Le backend (SF-98-46) ne renvoie au déclenchement que l'identifiant et le
 * statut initial — le résumé complet est obtenu via un `GET` ultérieur.
 */
export interface StyleCorpusUploadResponse {
  id: string;
  status: StyleCorpusDocumentStatus;
}

/**
 * F-98 / SF-98-48 — Accès HTTP au corpus de style du cabinet.
 *
 * Consomme le contrat API figé de SF-98-46 :
 * `/api/v1/workspaces/{workspaceId}/style-corpus/documents[/{id}]`.
 * Le `workspaceId` est résolu par l'appelant (écran cabinet) depuis le
 * workspace courant.
 */
@Injectable({ providedIn: 'root' })
export class StyleCorpusService {
  private readonly http = inject(HttpClient);

  private baseUrl(workspaceId: string): string {
    return `/api/v1/workspaces/${workspaceId}/style-corpus/documents`;
  }

  /**
   * Téléverse une conclusion de référence (multipart `file`).
   * Le traitement (extraction + signature de style) est asynchrone côté
   * backend ; la réponse `202` ne porte que l'`id` et le statut `PENDING`.
   */
  upload(workspaceId: string, file: File): Observable<StyleCorpusUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<StyleCorpusUploadResponse>(
      this.baseUrl(workspaceId),
      formData,
    );
  }

  /** Liste les documents du corpus de style du workspace. */
  list(workspaceId: string): Observable<StyleCorpusDocumentSummary[]> {
    return this.http.get<StyleCorpusDocumentSummary[]>(
      this.baseUrl(workspaceId),
    );
  }

  /** Active ou désactive un document du corpus de style. */
  setActive(
    workspaceId: string,
    id: string,
    active: boolean,
  ): Observable<StyleCorpusDocumentSummary> {
    return this.http.patch<StyleCorpusDocumentSummary>(
      `${this.baseUrl(workspaceId)}/${id}`,
      { active },
    );
  }

  /** Supprime un document du corpus de style. */
  remove(workspaceId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl(workspaceId)}/${id}`);
  }
}
