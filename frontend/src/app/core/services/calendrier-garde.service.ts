import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CalendrierGardeRequest, CalendrierGardeResponse } from '../models/calendrier-garde.model';

@Injectable({ providedIn: 'root' })
export class CalendrierGardeService {
  constructor(private http: HttpClient) {}
  generate(caseFileId: string, req: CalendrierGardeRequest): Observable<CalendrierGardeResponse> {
    return this.http.post<CalendrierGardeResponse>(`/api/v1/case-files/${caseFileId}/calendrier-garde`, req);
  }
  get(caseFileId: string): Observable<CalendrierGardeResponse> {
    return this.http.get<CalendrierGardeResponse>(`/api/v1/case-files/${caseFileId}/calendrier-garde`);
  }
}
