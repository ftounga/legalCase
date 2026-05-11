import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AesEtudiantRequest,
  AesEtudiantResponse,
} from '../models/aes-etudiant.model';

/**
 * SF-IM-09-08 : wrapper HttpClient pour l'outil décisionnel
 * "AES voie étudiante — France" (F-IM-09).
 * Consomme l'API figée dans SF-IM-09-04 (backend PR #505).
 */
@Injectable({ providedIn: 'root' })
export class AesEtudiantService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: AesEtudiantRequest):
      Observable<AesEtudiantResponse> {
    return this.http.post<AesEtudiantResponse>(
      `/api/v1/case-files/${caseFileId}/aes-etudiant`, request);
  }

  get(caseFileId: string): Observable<AesEtudiantResponse> {
    return this.http.get<AesEtudiantResponse>(
      `/api/v1/case-files/${caseFileId}/aes-etudiant`);
  }
  /**
   * F-163 SF-163-02d — `toolId` du dispatcher backend pour cet outil.
   * Aligné sur `STANDALONE_READY_TOOL_IDS` et sur la clé
   * `TOOL_REGISTRY.get('F-IM-09-aes-etudiant')`.
   */
  static readonly STANDALONE_TOOL_ID = 'F-IM-09-aes-etudiant';

  /**
   * F-163 SF-163-02d — POST sur le dispatcher générique des simulateurs
   * (contrat figé par SF-163-03). Body identique à la requête case-file ;
   * réponse identique. Aucune persistance côté backend.
   */
  calculateStandalone(request: AesEtudiantRequest): Observable<AesEtudiantResponse> {
    return this.http.post<AesEtudiantResponse>(
      `/api/v1/simulators/${AesEtudiantService.STANDALONE_TOOL_ID}/calculate`,
      request,
    );
  }

}
