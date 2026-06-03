import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CarteBSejourIllimiteBeRequest,
  CarteBSejourIllimiteBeResponse,
} from '../models/carte-b-sejour-illimite-be.model';

/**
 * SF-221-02 : wrapper HttpClient pour l'outil décisionnel
 * « Carte B séjour illimité (ressortissant tiers BE) » (F-IM-54-carte-b-sejour-illimite-be).
 *
 * BELGIQUE uniquement — éligibilité au passage carte A → carte B (séjour illimité)
 * après 5 ans (60 mois) de séjour régulier ininterrompu (Loi 15/12/1980 art. 14).
 *
 * Consomme l'API figée dans SF-221-02 (backend) :
 *  - POST /api/v1/case-files/{caseFileId}/carte-b-sejour-illimite-be-analysis
 *  - GET  /api/v1/case-files/{caseFileId}/carte-b-sejour-illimite-be-analysis
 *
 * Pattern miroir de {@link CarteAProrogationBeService}.
 */
@Injectable({ providedIn: 'root' })
export class CarteBSejourIllimiteBeService {
  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: CarteBSejourIllimiteBeRequest):
      Observable<CarteBSejourIllimiteBeResponse> {
    return this.http.post<CarteBSejourIllimiteBeResponse>(
      `/api/v1/case-files/${caseFileId}/carte-b-sejour-illimite-be-analysis`, request);
  }

  get(caseFileId: string): Observable<CarteBSejourIllimiteBeResponse> {
    return this.http.get<CarteBSejourIllimiteBeResponse>(
      `/api/v1/case-files/${caseFileId}/carte-b-sejour-illimite-be-analysis`);
  }

  /** `toolId` aligné sur la clé `TOOL_REGISTRY.get('F-IM-54-carte-b-sejour-illimite-be')`. */
  static readonly STANDALONE_TOOL_ID = 'F-IM-54-carte-b-sejour-illimite-be';
}
