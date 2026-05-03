import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type AnalysisJobType = 'CASE_ANALYSIS' | 'ENRICHED_ANALYSIS' | 'DOCUMENT_ANALYSIS' | 'QUESTION_GENERATION';

/**
 * F-185 SF-185-01 — `PARTIAL` est émis pendant le streaming Sonnet à chaque section
 * JSON top-level complétée. Le frontend l'utilise pour rafraîchir la synthèse au fil de
 * l'eau via `GET /case-analysis/partial`. `DONE` / `FAILED` restent les statuts terminaux.
 */
export interface AnalysisStatusEvent {
  caseFileId: string;
  status: 'PARTIAL' | 'DONE' | 'FAILED';
  jobType: AnalysisJobType;
}

@Injectable({ providedIn: 'root' })
export class AnalysisSseService {

  stream(caseFileId: string): Observable<AnalysisStatusEvent> {
    return new Observable(observer => {
      const url = `/api/v1/case-files/${caseFileId}/analysis-status/stream`;
      const source = new EventSource(url, { withCredentials: true });

      const eventNames = [
        'CASE_ANALYSIS_PARTIAL', 'CASE_ANALYSIS_DONE', 'CASE_ANALYSIS_FAILED',
        'ENRICHED_ANALYSIS_DONE', 'ENRICHED_ANALYSIS_FAILED',
        'DOCUMENT_ANALYSIS_DONE', 'DOCUMENT_ANALYSIS_FAILED',
        // F-185 SF-185-02 — fin de la génération Q&A async post-synthèse.
        'QUESTION_GENERATION_DONE', 'QUESTION_GENERATION_FAILED'
      ];
      for (const name of eventNames) {
        source.addEventListener(name, (e: MessageEvent) => {
          observer.next(JSON.parse(e.data) as AnalysisStatusEvent);
        });
      }

      // SF-186-01 — EventSource auto-reconnecte (browser-native, ~3s) sur les
      // erreurs réseau transitoires. Ne PAS appeler `source.close()` ni
      // `observer.complete()` tant que `readyState !== CLOSED` — sinon on tue
      // le stream définitivement et l'UI rate les events suivants (DONE,
      // FAILED). Bug observé staging 2026-05-03 sur Chen 8 : navigation
      // detail ↔ synthesis ↔ detail → blip réseau → SSE mort → barres
      // restent stuck → hard refresh obligatoire pour récupérer.
      source.onerror = () => {
        if (source.readyState === EventSource.CLOSED) {
          observer.complete();
        }
        // Sinon (CONNECTING=0, OPEN=1) : on laisse le navigateur retenter.
      };

      return () => source.close();
    });
  }
}
