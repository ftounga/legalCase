import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type AnalysisJobType = 'CASE_ANALYSIS' | 'ENRICHED_ANALYSIS' | 'DOCUMENT_ANALYSIS';

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
        'DOCUMENT_ANALYSIS_DONE', 'DOCUMENT_ANALYSIS_FAILED'
      ];
      for (const name of eventNames) {
        source.addEventListener(name, (e: MessageEvent) => {
          observer.next(JSON.parse(e.data) as AnalysisStatusEvent);
        });
      }

      source.onerror = () => {
        source.close();
        observer.complete();
      };

      return () => source.close();
    });
  }
}
