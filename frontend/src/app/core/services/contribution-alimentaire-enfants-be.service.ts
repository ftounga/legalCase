import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ContributionAlimentaireEnfantsBeRequest,
  ContributionAlimentaireEnfantsBeResponse,
} from '../models/contribution-alimentaire-enfants-be.model';

/**
 * SF-217-07 : wrapper HttpClient pour l'outil décisionnel "Contribution
 * alimentaire des enfants (Belgique)" (Vague 2 Famille BE — méthode Renard).
 * Consomme l'API figée dans SF-217-06 (backend).
 */
@Injectable({ providedIn: 'root' })
export class ContributionAlimentaireEnfantsBeService {
  constructor(private http: HttpClient) {}

  /** POST — estime et persiste la contribution alimentaire du dossier. */
  calculate(
    caseFileId: string,
    request: ContributionAlimentaireEnfantsBeRequest,
  ): Observable<ContributionAlimentaireEnfantsBeResponse> {
    return this.http.post<ContributionAlimentaireEnfantsBeResponse>(
      `/api/v1/case-files/${caseFileId}/contribution-alimentaire-enfants-be`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 404 si jamais calculé. */
  get(caseFileId: string): Observable<ContributionAlimentaireEnfantsBeResponse> {
    return this.http.get<ContributionAlimentaireEnfantsBeResponse>(
      `/api/v1/case-files/${caseFileId}/contribution-alimentaire-enfants-be`,
    );
  }
}
