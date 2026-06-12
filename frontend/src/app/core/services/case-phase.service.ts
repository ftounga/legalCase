import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CasePhase,
  CasePhaseInput,
  CasePhaseTimeline,
} from '../models/case-phase.model';

/** F-283 / SF-283-01 — accès aux phases procédurales (progression datée) d'un dossier. */
@Injectable({ providedIn: 'root' })
export class CasePhaseService {
  constructor(private http: HttpClient) {}

  private url(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/phases`;
  }

  timeline(caseFileId: string): Observable<CasePhaseTimeline> {
    return this.http.get<CasePhaseTimeline>(this.url(caseFileId));
  }

  create(caseFileId: string, input: CasePhaseInput): Observable<CasePhase> {
    return this.http.post<CasePhase>(this.url(caseFileId), input);
  }

  update(caseFileId: string, phaseId: string, input: CasePhaseInput): Observable<CasePhase> {
    return this.http.put<CasePhase>(`${this.url(caseFileId)}/${phaseId}`, input);
  }

  delete(caseFileId: string, phaseId: string): Observable<void> {
    return this.http.delete<void>(`${this.url(caseFileId)}/${phaseId}`);
  }
}
