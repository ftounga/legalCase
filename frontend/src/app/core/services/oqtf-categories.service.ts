import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  OqtfCategoriesRequest,
  OqtfCategoriesResponse,
} from '../models/oqtf-categories.model';

/**
 * SF-214-10 : wrapper HttpClient pour l'outil décisionnel
 * « OQTF catégories L. 611-1 » (F-IM-29). FR uniquement.
 * Consomme l'API figée dans SF-214-09 (backend).
 *
 * Pattern miroir de {@link RegroupementFamilialService}.
 */
@Injectable({ providedIn: 'root' })
export class OqtfCategoriesService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: OqtfCategoriesRequest):
      Observable<OqtfCategoriesResponse> {
    return this.http.post<OqtfCategoriesResponse>(
      `/api/v1/case-files/${caseFileId}/oqtf-categories-analysis`, request);
  }

  get(caseFileId: string): Observable<OqtfCategoriesResponse> {
    return this.http.get<OqtfCategoriesResponse>(
      `/api/v1/case-files/${caseFileId}/oqtf-categories-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-29-oqtf-categories-l6111-fr')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-29-oqtf-categories-l6111-fr';
}
