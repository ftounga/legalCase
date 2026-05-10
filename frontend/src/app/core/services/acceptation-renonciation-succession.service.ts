import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AcceptationRenonciationSuccessionRequest,
  AcceptationRenonciationSuccessionResponse,
} from '../models/acceptation-renonciation-succession.model';

/**
 * F-210 SF-210-04 : service HTTP pour l'outil "Acceptation / renonciation
 * succession". Wrappe les endpoints SF-210-03.
 */
@Injectable({ providedIn: 'root' })
export class AcceptationRenonciationSuccessionService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string,
            request: AcceptationRenonciationSuccessionRequest)
            : Observable<AcceptationRenonciationSuccessionResponse> {
    return this.http.post<AcceptationRenonciationSuccessionResponse>(
      `/api/v1/case-files/${caseFileId}/acceptation-renonciation-succession`,
      request,
    );
  }

  get(caseFileId: string): Observable<AcceptationRenonciationSuccessionResponse> {
    return this.http.get<AcceptationRenonciationSuccessionResponse>(
      `/api/v1/case-files/${caseFileId}/acceptation-renonciation-succession`,
    );
  }
}
