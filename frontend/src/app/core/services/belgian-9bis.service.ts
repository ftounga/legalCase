import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Belgian9bisRequest,
  Belgian9bisResponse,
} from '../models/belgian-9bis.model';

/**
 * SF-IM-14-05 : wrapper HttpClient pour l'outil "9bis humanitaire BE"
 * (F-IM-14, Loi 15/12/1980 art. 9bis). BELGIQUE uniquement.
 * Contrat importé de SF-IM-14-01 (backend Belgian9bisController).
 */
@Injectable({ providedIn: 'root' })
export class Belgian9bisService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: Belgian9bisRequest):
      Observable<Belgian9bisResponse> {
    return this.http.post<Belgian9bisResponse>(
      `/api/v1/case-files/${caseFileId}/belgian-9bis`, request);
  }

  get(caseFileId: string): Observable<Belgian9bisResponse> {
    return this.http.get<Belgian9bisResponse>(
      `/api/v1/case-files/${caseFileId}/belgian-9bis`);
  }
}
