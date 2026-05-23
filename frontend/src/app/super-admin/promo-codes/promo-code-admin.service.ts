import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  PromoCodeCreateRequest,
  PromoCodeDto,
} from './promo-code.model';

/**
 * F-255 SF-255-02 — Service Angular qui consomme les endpoints super-admin
 * de gestion des codes promo (figés par SF-255-01, PR #1253).
 *
 * - `createCode`     → `POST   /api/v1/super-admin/promo-codes`
 * - `listCodes`      → `GET    /api/v1/super-admin/promo-codes`
 * - `deactivateCode` → `POST   /api/v1/super-admin/promo-codes/{id}/deactivate`
 *
 * Sécurité : le filtrage super-admin est centralisé côté backend
 * ({@code PromoCodeService.assertSuperAdmin}). Le frontend se contente de
 * relayer les codes d'erreur HTTP au composant appelant.
 */
@Injectable({ providedIn: 'root' })
export class PromoCodeAdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/super-admin/promo-codes';

  /** Crée un nouveau code promo. */
  createCode(req: PromoCodeCreateRequest): Observable<PromoCodeDto> {
    return this.http.post<PromoCodeDto>(this.baseUrl, req);
  }

  /** Liste l'ensemble des codes promo triés par `createdAt DESC` côté backend. */
  listCodes(): Observable<PromoCodeDto[]> {
    return this.http.get<PromoCodeDto[]>(this.baseUrl);
  }

  /** Désactive un code promo existant (active passe à false). */
  deactivateCode(id: string): Observable<PromoCodeDto> {
    return this.http.post<PromoCodeDto>(`${this.baseUrl}/${id}/deactivate`, {});
  }
}
