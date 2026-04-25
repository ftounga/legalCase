import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  IndivisionRequest,
  IndivisionResponse,
} from '../models/indivision.model';

/**
 * SF-FA-22-02 : service HTTP pour l'outil décisionnel Indivision
 * post-communautaire (art. 815 et s. Cciv). Backend SF-FA-22-01.
 */
@Injectable({ providedIn: 'root' })
export class IndivisionService {
  constructor(private http: HttpClient) {}

  analyser(caseFileId: string, request: IndivisionRequest): Observable<IndivisionResponse> {
    return this.http.post<IndivisionResponse>(
      `/api/v1/case-files/${caseFileId}/indivision`,
      request,
    );
  }

  get(caseFileId: string): Observable<IndivisionResponse> {
    return this.http.get<IndivisionResponse>(
      `/api/v1/case-files/${caseFileId}/indivision`,
    );
  }
}
