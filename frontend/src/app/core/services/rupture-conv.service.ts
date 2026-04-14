import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RuptureConvRequest, RuptureConvResponse } from '../models/rupture-conv.model';

@Injectable({ providedIn: 'root' })
export class RuptureConvService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: RuptureConvRequest): Observable<RuptureConvResponse> {
    return this.http.post<RuptureConvResponse>(
      `/api/v1/case-files/${caseFileId}/rupture-conv`, request);
  }

  get(caseFileId: string): Observable<RuptureConvResponse> {
    return this.http.get<RuptureConvResponse>(
      `/api/v1/case-files/${caseFileId}/rupture-conv`);
  }
}
