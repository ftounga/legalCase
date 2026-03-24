import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface AnalysisStatusEvent {
  caseFileId: string;
  status: 'DONE' | 'FAILED';
}

@Injectable({ providedIn: 'root' })
export class AnalysisSseService {

  stream(caseFileId: string): Observable<AnalysisStatusEvent> {
    return new Observable(observer => {
      const url = `/api/v1/case-files/${caseFileId}/analysis-status/stream`;
      const source = new EventSource(url, { withCredentials: true });

      source.addEventListener('ANALYSIS_DONE', (e: MessageEvent) => {
        observer.next(JSON.parse(e.data) as AnalysisStatusEvent);
        observer.complete();
        source.close();
      });

      source.addEventListener('ANALYSIS_FAILED', (e: MessageEvent) => {
        observer.next(JSON.parse(e.data) as AnalysisStatusEvent);
        observer.complete();
        source.close();
      });

      source.onerror = () => {
        source.close();
        observer.complete();
      };

      return () => source.close();
    });
  }
}
