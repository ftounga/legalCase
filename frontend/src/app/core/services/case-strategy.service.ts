import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CaseStrategy } from '../models/case-strategy.model';

/** F-286 / SF-286-01 — accès à la stratégie de dossier unifiée (lecture + génération). */
@Injectable({ providedIn: 'root' })
export class CaseStrategyService {
  constructor(private http: HttpClient) {}

  private url(caseFileId: string): string {
    return `/api/v1/case-files/${caseFileId}/strategy`;
  }

  /** Lit la stratégie courante du dossier (jamais de génération). */
  get(caseFileId: string): Observable<CaseStrategy> {
    return this.http.get<CaseStrategy>(this.url(caseFileId));
  }

  /** (Re)génère la stratégie du dossier (couche LLM en lecture des verdicts calculés). */
  generate(caseFileId: string): Observable<CaseStrategy> {
    return this.http.post<CaseStrategy>(this.url(caseFileId), {});
  }
}
