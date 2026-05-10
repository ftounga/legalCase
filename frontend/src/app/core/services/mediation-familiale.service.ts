import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  MediationFamilialePreSaisineRequest,
  MediationFamilialePreSaisineResponse,
} from '../models/mediation-familiale.model';

/**
 * F-210 SF-210-02 : service HTTP pour l'outil de médiation familiale
 * obligatoire pré-saisine JAF. Wrappe les endpoints SF-210-01.
 */
@Injectable({ providedIn: 'root' })
export class MediationFamilialePreSaisineService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string,
            request: MediationFamilialePreSaisineRequest): Observable<MediationFamilialePreSaisineResponse> {
    return this.http.post<MediationFamilialePreSaisineResponse>(
      `/api/v1/case-files/${caseFileId}/mediation-familiale-pre-saisine`,
      request,
    );
  }

  get(caseFileId: string): Observable<MediationFamilialePreSaisineResponse> {
    return this.http.get<MediationFamilialePreSaisineResponse>(
      `/api/v1/case-files/${caseFileId}/mediation-familiale-pre-saisine`,
    );
  }
}
