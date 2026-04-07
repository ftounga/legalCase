import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PartageImmobilierRequest, PartageImmobilierResponse } from '../models/partage-immobilier.model';

@Injectable({ providedIn: 'root' })
export class PartageImmobilierService {
  constructor(private http: HttpClient) {}

  calculate(caseFileId: string, request: PartageImmobilierRequest): Observable<PartageImmobilierResponse> {
    return this.http.post<PartageImmobilierResponse>(`/api/v1/case-files/${caseFileId}/partage-immobilier`, request);
  }

  get(caseFileId: string): Observable<PartageImmobilierResponse> {
    return this.http.get<PartageImmobilierResponse>(`/api/v1/case-files/${caseFileId}/partage-immobilier`);
  }
}
