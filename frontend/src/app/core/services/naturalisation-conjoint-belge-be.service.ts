import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  NaturalisationConjointBelgeBeRequest,
  NaturalisationConjointBelgeBeResponse,
} from '../models/naturalisation-conjoint-belge-be.model';

/**
 * SF-215-10 : wrapper HttpClient pour l'outil décisionnel
 * « Naturalisation conjoint Belge — art. 16 »
 * (F-IM-29-naturalisation-conjoint-belge-be).
 *
 * BELGIQUE uniquement — scoring d'éligibilité d'une déclaration de
 * nationalité belge par voie article 16 CNB (conjoint étranger d'un(e)
 * Belge). Verdict `eligible` (bool) + `dureeManquante` en mois si la
 * cohabitation 5 ans n'est pas encore atteinte.
 *
 * Consomme l'API figée dans SF-215-09 (backend) :
 *  - POST /api/v1/case-files/{caseFileId}/naturalisation-conjoint-belge-be-analysis
 *  - GET  /api/v1/case-files/{caseFileId}/naturalisation-conjoint-belge-be-analysis
 *
 * Pattern miroir de {@link Naturalisation12bisBeService} (SF-215-08).
 */
@Injectable({ providedIn: 'root' })
export class NaturalisationConjointBelgeBeService {
  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: NaturalisationConjointBelgeBeRequest):
      Observable<NaturalisationConjointBelgeBeResponse> {
    return this.http.post<NaturalisationConjointBelgeBeResponse>(
      `/api/v1/case-files/${caseFileId}/naturalisation-conjoint-belge-be-analysis`, request);
  }

  get(caseFileId: string): Observable<NaturalisationConjointBelgeBeResponse> {
    return this.http.get<NaturalisationConjointBelgeBeResponse>(
      `/api/v1/case-files/${caseFileId}/naturalisation-conjoint-belge-be-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-29-naturalisation-conjoint-belge-be')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-29-naturalisation-conjoint-belge-be';
}
