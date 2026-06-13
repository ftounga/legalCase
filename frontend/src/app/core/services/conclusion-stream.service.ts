import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/**
 * SF-287-01 — Événement `progress` du flux SSE de génération des conclusions.
 * Contrat figé (mini-spec) : pourcentage RÉEL (tokens / maxTokens), section en
 * cours détectée (dernier titre `##`/`###`) et markdown partiel pour l'aperçu live.
 */
export interface ConclusionProgressEvent {
  type: 'progress';
  tokens: number;
  maxTokens: number;
  percent: number;
  currentSection: string | null;
  sectionIndex: number | null;
  partialContent: string;
}

/** SF-287-01 — Événement terminal `done` (le content final est relu via GET). */
export interface ConclusionDoneEvent {
  type: 'done';
  status: 'DONE';
  versionNumber: number;
}

/** SF-287-01 — Événement terminal `failed`. */
export interface ConclusionFailedEvent {
  type: 'failed';
  status: 'FAILED';
  message: string;
}

export type ConclusionStreamEvent =
  | ConclusionProgressEvent
  | ConclusionDoneEvent
  | ConclusionFailedEvent;

/**
 * SF-287-01 — Wrapper `EventSource` pour le flux de génération des conclusions
 * (`GET /api/v1/case-files/{id}/conclusions/stream`, `text/event-stream`).
 *
 * Réutilise le patron de `AnalysisSseService` (F-185). L'Observable :
 *  - `next` à chaque événement `progress` / `done` / `failed` ;
 *  - `complete` sur `done` / `failed` (états terminaux — l'EventSource est fermé) ;
 *  - `error` si la connexion est définitivement perdue (`readyState === CLOSED`),
 *    pour permettre au composant de retomber sur le polling existant (fallback).
 *
 * Contrairement au flux d'analyse, on ferme nous-mêmes la connexion sur l'état
 * terminal (le backend ne complète pas l'emitter côté serveur pendant la
 * génération). La désinscription (`unsubscribe`) ferme également l'EventSource.
 */
@Injectable({ providedIn: 'root' })
export class ConclusionStreamService {
  stream(caseFileId: string): Observable<ConclusionStreamEvent> {
    return new Observable<ConclusionStreamEvent>((observer) => {
      const url = `/api/v1/case-files/${caseFileId}/conclusions/stream`;
      const source = new EventSource(url, { withCredentials: true });

      source.addEventListener('progress', (e: MessageEvent) => {
        try {
          const data = JSON.parse(e.data) as Omit<ConclusionProgressEvent, 'type'>;
          observer.next({ type: 'progress', ...data });
        } catch {
          // Fragment malformé — on ignore, le prochain progress corrigera l'aperçu.
        }
      });

      source.addEventListener('done', (e: MessageEvent) => {
        try {
          const data = JSON.parse(e.data) as Omit<ConclusionDoneEvent, 'type'>;
          observer.next({ type: 'done', ...data });
        } catch {
          observer.next({ type: 'done', status: 'DONE', versionNumber: 0 });
        }
        source.close();
        observer.complete();
      });

      source.addEventListener('failed', (e: MessageEvent) => {
        try {
          const data = JSON.parse(e.data) as Omit<ConclusionFailedEvent, 'type'>;
          observer.next({ type: 'failed', ...data });
        } catch {
          observer.next({ type: 'failed', status: 'FAILED', message: '' });
        }
        source.close();
        observer.complete();
      });

      // Connexion perdue. L'EventSource auto-reconnecte (CONNECTING/OPEN) ; on ne
      // signale une erreur (→ fallback polling côté composant) que si la connexion
      // est définitivement fermée (CLOSED), pour ne pas casser un blip réseau.
      source.onerror = () => {
        if (source.readyState === EventSource.CLOSED) {
          observer.error(new Error('conclusion stream closed'));
        }
      };

      return () => source.close();
    });
  }
}
