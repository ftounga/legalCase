import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ContradictoireRound,
  ContradictoireRoundInput,
  ContradictoireTimeline,
} from '../models/contradictoire.model';

/** F-282 — accès au cycle contradictoire (rounds) d'un dossier. */
@Injectable({ providedIn: 'root' })
export class ContradictoireService {
  constructor(private http: HttpClient) {}

  private url(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/contradictoire-rounds`;
  }

  timeline(caseFileId: string): Observable<ContradictoireTimeline> {
    return this.http.get<ContradictoireTimeline>(this.url(caseFileId));
  }

  create(caseFileId: string, input: ContradictoireRoundInput): Observable<ContradictoireRound> {
    return this.http.post<ContradictoireRound>(this.url(caseFileId), input);
  }

  update(
    caseFileId: string,
    roundId: string,
    input: ContradictoireRoundInput,
  ): Observable<ContradictoireRound> {
    return this.http.put<ContradictoireRound>(`${this.url(caseFileId)}/${roundId}`, input);
  }

  delete(caseFileId: string, roundId: string): Observable<void> {
    return this.http.delete<void>(`${this.url(caseFileId)}/${roundId}`);
  }
}
