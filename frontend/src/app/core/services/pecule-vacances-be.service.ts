import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PeculeVacancesBeRequest,
  PeculeVacancesBeResponse,
} from '../models/pecule-vacances-be.model';

/**
 * SF-219-20b : wrapper HttpClient pour l'outil décisionnel
 * « pécule de vacances BE » (F-219, BE uniquement — Lois coordonnées
 * du 28/06/1971 relatives aux vacances annuelles des travailleurs
 * salariés + AR du 30/03/1967 déterminant les modalités générales
 * d'exécution).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/pecule-vacances-be}.</p>
 *
 * <p>Outil <b>BE-only</b>. L'indemnité compensatrice de congés payés
 * française (C. trav. art. L. 3141-24 et s. — 10 % de la rémunération
 * brute totale ou maintien du salaire) est un régime juridiquement
 * distinct (pas de double pécule, pas de débiteur tiers de type
 * ONVA / Caisse de vacances sectorielle). Aucun mapping mécanique. Le
 * gate {@code workspaceCountry} est porté côté composant ; le backend
 * renvoie 404 pour FR par cohérence.</p>
 */
@Injectable({ providedIn: 'root' })
export class PeculeVacancesBeService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/pecule-vacances-be`;
  }

  analyze(
    caseFileId: string,
    request: PeculeVacancesBeRequest,
  ): Observable<PeculeVacancesBeResponse> {
    return this.http.post<PeculeVacancesBeResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<PeculeVacancesBeResponse> {
    return this.http.get<PeculeVacancesBeResponse>(
      this.endpoint(caseFileId),
    );
  }
}
