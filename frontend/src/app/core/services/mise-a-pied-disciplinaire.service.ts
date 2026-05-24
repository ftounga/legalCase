import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  MiseAPiedDisciplinaireRequest,
  MiseAPiedDisciplinaireResponse,
} from '../models/mise-a-pied-disciplinaire.model';

/**
 * SF-212-20 : wrapper HttpClient pour l'outil décisionnel "Mise à pied
 * disciplinaire — régularité" (F-DT-48-mise-a-pied-disciplinaire, FR —
 * L. 1331-1 CT ; L. 1332-1 à L. 1332-4 CT ; Cass. soc. constante).
 * Consomme l'API figée dans SF-212-19 (backend).
 */
@Injectable({ providedIn: 'root' })
export class MiseAPiedDisciplinaireService {
  constructor(private http: HttpClient) {}

  /** POST — calcule et persiste l'analyse pour le dossier. */
  calculate(
    caseFileId: string,
    request: MiseAPiedDisciplinaireRequest,
  ): Observable<MiseAPiedDisciplinaireResponse> {
    return this.http.post<MiseAPiedDisciplinaireResponse>(
      `/api/v1/case-files/${caseFileId}/mise-a-pied-disciplinaire`,
      request,
    );
  }

  /** GET — dernier résultat calculé, ou 204 No Content si jamais calculé. */
  get(caseFileId: string): Observable<MiseAPiedDisciplinaireResponse> {
    return this.http.get<MiseAPiedDisciplinaireResponse>(
      `/api/v1/case-files/${caseFileId}/mise-a-pied-disciplinaire`,
    );
  }
}
