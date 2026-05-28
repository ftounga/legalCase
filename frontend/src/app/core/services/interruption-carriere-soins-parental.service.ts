import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  InterruptionCarriereSoinsParentalRequest,
  InterruptionCarriereSoinsParentalResponse,
} from '../models/interruption-carriere-soins-parental.model';

/**
 * SF-219-32b : wrapper HttpClient pour l'outil decisionnel
 * Interruption de carriere pour conge parental BE (F-219, BE
 * uniquement — Loi du 22/01/1985 art. 99 a 107quater + AR du
 * 29/10/1997 + CCT n 64 + AR du 12/08/1991 allocations ONEM).
 *
 * <p>Endpoint canonique :
 * {@code /api/v1/case-files/&#123;caseFileId&#125;/decision-tools/interruption-carriere-soins-parental}.</p>
 *
 * <p>Outil <b>BE-only</b>. Le regime francais du conge parental
 * d'education (Code du travail FR art. L. 1225-47 a L. 1225-60,
 * duree 1 an renouvelable, allocations CAF) est juridiquement
 * distinct. Aucune transposition mecanique. Le gate
 * {@code workspaceCountry} est porte cote composant ; le backend
 * renvoie 404 pour FR par coherence.</p>
 *
 * <p>Distinct de F-DT-29 {@code credit-temps-be} (CCT 103 — regime
 * universel sans motif specifique). Cet outil couvre uniquement le
 * conge parental Loi 22/01/1985 et CCT 64 (droit individuel par
 * enfant et par parent).</p>
 */
@Injectable({ providedIn: 'root' })
export class InterruptionCarriereSoinsParentalService {

  constructor(private http: HttpClient) {}

  private endpoint(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/decision-tools/interruption-carriere-soins-parental`;
  }

  analyze(
    caseFileId: string,
    request: InterruptionCarriereSoinsParentalRequest,
  ): Observable<InterruptionCarriereSoinsParentalResponse> {
    return this.http.post<InterruptionCarriereSoinsParentalResponse>(
      this.endpoint(caseFileId),
      request,
    );
  }

  get(caseFileId: string): Observable<InterruptionCarriereSoinsParentalResponse> {
    return this.http.get<InterruptionCarriereSoinsParentalResponse>(
      this.endpoint(caseFileId),
    );
  }
}
