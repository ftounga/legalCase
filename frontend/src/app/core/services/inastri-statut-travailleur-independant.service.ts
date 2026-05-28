import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  InastriStatutTravailleurIndependantRequest,
  InastriStatutTravailleurIndependantResponse,
} from '../models/inastri-statut-travailleur-independant.model';

/**
 * SF-219-27b : wrapper HttpClient pour l'outil décisionnel « INASTI —
 * statut travailleur indépendant et qualification salarié /
 * indépendant » (F-219, BE uniquement — Loi du 27/06/1969 + AR n° 38
 * du 27/07/1967 + AR du 19/12/1967 + Loi-programme I du 27/12/2006
 * art. 328 à 333 « doctrine Bart Buysse » + art. 337/1 à 337/3
 * critères sectoriels).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/inastri-statut-travailleur-independant}.</p>
 *
 * <p>Outil <b>BE-only</b>. Le régime français de qualification
 * salarié / indépendant (C. trav. art. L. 8221-6 présomption de
 * non-salariat des inscrits aux registres, URSSAF redressement art.
 * R. 243-18 + Loi Madelin + statut autoentrepreneur Loi 04/08/2008)
 * constitue un dispositif juridiquement distinct dans ses critères
 * et sa procédure. Aucun mapping mécanique. Le gate
 * {@code workspaceCountry} est porté côté composant ; le backend
 * renvoie 404 pour FR par cohérence.</p>
 */
@Injectable({ providedIn: 'root' })
export class InastriStatutTravailleurIndependantService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/inastri-statut-travailleur-independant`;
  }

  analyze(
    caseFileId: string,
    request: InastriStatutTravailleurIndependantRequest,
  ): Observable<InastriStatutTravailleurIndependantResponse> {
    return this.http.post<InastriStatutTravailleurIndependantResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<InastriStatutTravailleurIndependantResponse> {
    return this.http.get<InastriStatutTravailleurIndependantResponse>(
      this.endpoint(caseFileId),
    );
  }
}
