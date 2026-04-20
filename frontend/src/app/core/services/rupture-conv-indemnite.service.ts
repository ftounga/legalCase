import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  RuptureConvIndemniteRequest,
  RuptureConvIndemniteResponse,
} from '../models/rupture-conv-indemnite.model';

@Injectable({ providedIn: 'root' })
export class RuptureConvIndemniteService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: RuptureConvIndemniteRequest):
      Observable<RuptureConvIndemniteResponse> {
    return this.http.post<RuptureConvIndemniteResponse>(
      `/api/v1/case-files/${caseFileId}/rupture-conv-indemnite`, request);
  }

  get(caseFileId: string): Observable<RuptureConvIndemniteResponse> {
    return this.http.get<RuptureConvIndemniteResponse>(
      `/api/v1/case-files/${caseFileId}/rupture-conv-indemnite`);
  }
}
