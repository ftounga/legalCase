import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IndemniteComparatifRequest, IndemniteComparatifResponse } from '../models/indemnite-comparatif.model';

@Injectable({ providedIn: 'root' })
export class IndemniteComparatifService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: IndemniteComparatifRequest): Observable<IndemniteComparatifResponse> {
    return this.http.post<IndemniteComparatifResponse>(
      `/api/v1/case-files/${caseFileId}/indemnite-comparatif`, request);
  }

  get(caseFileId: string): Observable<IndemniteComparatifResponse> {
    return this.http.get<IndemniteComparatifResponse>(
      `/api/v1/case-files/${caseFileId}/indemnite-comparatif`);
  }
}
