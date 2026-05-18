import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateJurisprudenceCitationRequest,
  JurisprudenceCitation,
  JurisprudenceCitationListResponse,
  UpdateJurisprudenceCitationRequest,
} from '../models/jurisprudence-citation.model';

/**
 * F-242 SF-242-02 — accès aux citations de jurisprudence d'appui d'un dossier.
 *
 * <p>Consomme les 4 routes figées par SF-242-01. Toutes les routes sont sous
 * le dossier et soumises à l'isolation workspace côté backend.</p>
 */
@Injectable({ providedIn: 'root' })
export class JurisprudenceCitationService {
  constructor(private http: HttpClient) {}

  private url(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/jurisprudence-citations`;
  }

  /** `GET` — toutes les citations du dossier (tous points juridiques confondus). */
  list(caseFileId: string): Observable<JurisprudenceCitationListResponse> {
    return this.http.get<JurisprudenceCitationListResponse>(this.url(caseFileId));
  }

  /** `POST` — crée une citation rattachée à un point juridique. */
  create(
    caseFileId: string,
    request: CreateJurisprudenceCitationRequest,
  ): Observable<JurisprudenceCitation> {
    return this.http.post<JurisprudenceCitation>(this.url(caseFileId), request);
  }

  /** `PUT` — met à jour la référence / portée d'une citation existante. */
  update(
    caseFileId: string,
    citationId: string,
    request: UpdateJurisprudenceCitationRequest,
  ): Observable<JurisprudenceCitation> {
    return this.http.put<JurisprudenceCitation>(
      `${this.url(caseFileId)}/${citationId}`,
      request,
    );
  }

  /** `DELETE` — supprime une citation. */
  delete(caseFileId: string, citationId: string): Observable<void> {
    return this.http.delete<void>(`${this.url(caseFileId)}/${citationId}`);
  }
}
