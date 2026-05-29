import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ProtectionTemporaireUkraineBeRequest,
  ProtectionTemporaireUkraineBeResponse,
} from '../models/protection-temporaire-ukraine-be.model';

/**
 * SF-215-20 : wrapper HttpClient pour l'outil décisionnel
 * « Protection temporaire Ukraine (BE) »
 * (F-IM-34-protection-temporaire-ukraine-be).
 *
 * BELGIQUE uniquement — régime de protection temporaire (décision d'exécution
 * (UE) 2022/382 activant la directive 2001/55/CE) au bénéfice des personnes
 * déplacées d'Ukraine. L'outil vérifie l'éligibilité, calcule la durée de
 * protection restante, expose les droits (travail sans single permit, aides),
 * le prochain renouvellement et le chemin procédural.
 *
 * Consomme l'API figée dans SF-215-19 (backend) :
 *  - POST /api/v1/case-files/{caseFileId}/protection-temporaire-ukraine-be-analysis
 *  - GET  /api/v1/case-files/{caseFileId}/protection-temporaire-ukraine-be-analysis
 *
 * Pattern miroir de {@link Annexe13quinquiesBeService}.
 */
@Injectable({ providedIn: 'root' })
export class ProtectionTemporaireUkraineBeService {
  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: ProtectionTemporaireUkraineBeRequest):
      Observable<ProtectionTemporaireUkraineBeResponse> {
    return this.http.post<ProtectionTemporaireUkraineBeResponse>(
      `/api/v1/case-files/${caseFileId}/protection-temporaire-ukraine-be-analysis`, request);
  }

  get(caseFileId: string): Observable<ProtectionTemporaireUkraineBeResponse> {
    return this.http.get<ProtectionTemporaireUkraineBeResponse>(
      `/api/v1/case-files/${caseFileId}/protection-temporaire-ukraine-be-analysis`);
  }

  /**
   * `toolId` du dispatcher backend pour cet outil.
   * Aligné sur la clé `TOOL_REGISTRY.get('F-IM-34-protection-temporaire-ukraine-be')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-34-protection-temporaire-ukraine-be';
}
