import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AesHumanitaireRequest,
  AesHumanitaireResponse,
} from '../models/aes-humanitaire.model';

/**
 * SF-IM-09-07 : wrapper HttpClient pour l'outil décisionnel AES voie
 * humanitaire (L.435-2 CESEDA + L.432-14 commission du titre de séjour).
 * Consomme l'API figée dans SF-IM-09-03 (PR #507).
 */
@Injectable({ providedIn: 'root' })
export class AesHumanitaireService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: AesHumanitaireRequest): Observable<AesHumanitaireResponse> {
    return this.http.post<AesHumanitaireResponse>(
      `/api/v1/case-files/${caseFileId}/aes-humanitaire`, request);
  }

  get(caseFileId: string): Observable<AesHumanitaireResponse> {
    return this.http.get<AesHumanitaireResponse>(
      `/api/v1/case-files/${caseFileId}/aes-humanitaire`);
  }
}
