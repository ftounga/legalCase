import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AesMetiersTensionRequest,
  AesMetiersTensionResponse,
} from '../models/aes-metiers-tension.model';

/**
 * SF-IM-09-05 : wrapper HttpClient pour l'outil décisionnel
 * "AES Métiers en tension — France" (F-IM-09).
 * Consomme l'API figée dans SF-IM-09-01 (backend PR #504).
 */
@Injectable({ providedIn: 'root' })
export class AesMetiersTensionService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: AesMetiersTensionRequest):
      Observable<AesMetiersTensionResponse> {
    return this.http.post<AesMetiersTensionResponse>(
      `/api/v1/case-files/${caseFileId}/aes-metiers-tension`, request);
  }

  get(caseFileId: string): Observable<AesMetiersTensionResponse> {
    return this.http.get<AesMetiersTensionResponse>(
      `/api/v1/case-files/${caseFileId}/aes-metiers-tension`);
  }
}
