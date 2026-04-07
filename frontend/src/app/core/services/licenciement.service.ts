import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { LicenciementRequest, LicenciementResponse } from '../models/licenciement.model';

@Injectable({ providedIn: 'root' })
export class LicenciementService {

  constructor(private http: HttpClient) {}

  analyze(caseFileId: string, request: LicenciementRequest): Observable<LicenciementResponse> {
    return this.http.post<LicenciementResponse>(
      `/api/v1/case-files/${caseFileId}/licenciement`, request);
  }

  get(caseFileId: string): Observable<LicenciementResponse> {
    return this.http.get<LicenciementResponse>(
      `/api/v1/case-files/${caseFileId}/licenciement`);
  }
}
