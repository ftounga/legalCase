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
}
