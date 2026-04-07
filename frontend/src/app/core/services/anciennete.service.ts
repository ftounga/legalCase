import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AncienneteRequest, AncienneteResponse } from '../models/anciennete.model';

@Injectable({ providedIn: 'root' })
export class AncienneteService {

  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: AncienneteRequest): Observable<AncienneteResponse> {
    return this.http.post<AncienneteResponse>(
      `/api/v1/case-files/${caseFileId}/anciennete`,
      request
    );
  }

  get(caseFileId: string): Observable<AncienneteResponse> {
    return this.http.get<AncienneteResponse>(
      `/api/v1/case-files/${caseFileId}/anciennete`
    );
  }
}
