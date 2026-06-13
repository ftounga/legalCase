import { TestBed } from '@angular/core/testing';
import {
  ConclusionStreamEvent,
  ConclusionStreamService,
} from './conclusion-stream.service';

class MockEventSource {
  static instances: MockEventSource[] = [];
  static readonly CONNECTING = 0;
  static readonly OPEN = 1;
  static readonly CLOSED = 2;
  url: string;
  withCredentials: boolean;
  closed = false;
  readyState: number = MockEventSource.OPEN;
  onerror: ((this: EventSource, ev: Event) => unknown) | null = null;
  private listeners = new Map<string, ((e: MessageEvent) => void)[]>();

  constructor(url: string, init?: EventSourceInit) {
    this.url = url;
    this.withCredentials = init?.withCredentials ?? false;
    MockEventSource.instances.push(this);
  }

  addEventListener(name: string, cb: (e: MessageEvent) => void): void {
    const arr = this.listeners.get(name) ?? [];
    arr.push(cb);
    this.listeners.set(name, arr);
  }

  close(): void {
    this.closed = true;
    this.readyState = MockEventSource.CLOSED;
  }

  emit(name: string, data: unknown): void {
    const cbs = this.listeners.get(name) ?? [];
    cbs.forEach((cb) => cb({ data: JSON.stringify(data) } as MessageEvent));
  }

  triggerError(readyState: number = MockEventSource.CONNECTING): void {
    this.readyState = readyState;
    this.onerror?.call(this as unknown as EventSource, new Event('error'));
  }
}

describe('ConclusionStreamService', () => {
  let service: ConclusionStreamService;
  let originalEventSource: typeof EventSource | undefined;

  beforeEach(() => {
    originalEventSource = (globalThis as { EventSource?: typeof EventSource })
      .EventSource;
    (globalThis as { EventSource: unknown }).EventSource = MockEventSource;
    MockEventSource.instances = [];
    TestBed.configureTestingModule({});
    service = TestBed.inject(ConclusionStreamService);
  });

  afterEach(() => {
    (globalThis as { EventSource: unknown }).EventSource = originalEventSource;
  });

  it('ouvre l’EventSource sur le bon endpoint avec withCredentials', () => {
    service.stream('case-1').subscribe();
    const es = MockEventSource.instances[0];
    expect(es.url).toBe('/api/v1/case-files/case-1/conclusions/stream');
    expect(es.withCredentials).toBe(true);
  });

  it('émet les événements progress puis complète au done', () => {
    const events: ConclusionStreamEvent[] = [];
    let completed = false;
    service.stream('c').subscribe({
      next: (e) => events.push(e),
      complete: () => (completed = true),
    });
    const es = MockEventSource.instances[0];

    es.emit('progress', {
      tokens: 4000,
      maxTokens: 8000,
      percent: 50,
      currentSection: 'II. DISCUSSION',
      sectionIndex: 2,
      partialContent: '## II. DISCUSSION',
    });
    es.emit('done', { status: 'DONE', versionNumber: 5 });

    expect(events[0]).toEqual(
      expect.objectContaining({ type: 'progress', percent: 50, currentSection: 'II. DISCUSSION' }),
    );
    expect(events[1]).toEqual(
      expect.objectContaining({ type: 'done', versionNumber: 5 }),
    );
    expect(completed).toBe(true);
    expect(es.closed).toBe(true);
  });

  it('émet failed puis complète et ferme', () => {
    const events: ConclusionStreamEvent[] = [];
    let completed = false;
    service.stream('c').subscribe({
      next: (e) => events.push(e),
      complete: () => (completed = true),
    });
    const es = MockEventSource.instances[0];

    es.emit('failed', { status: 'FAILED', message: 'boom' });

    expect(events[0]).toEqual(
      expect.objectContaining({ type: 'failed', message: 'boom' }),
    );
    expect(completed).toBe(true);
    expect(es.closed).toBe(true);
  });

  it('ne signale une erreur (fallback) que si la connexion est CLOSED', () => {
    let errored = false;
    service.stream('c').subscribe({ error: () => (errored = true) });
    const es = MockEventSource.instances[0];

    // Blip réseau (CONNECTING) : pas d'erreur, le navigateur reconnecte.
    es.triggerError(MockEventSource.CONNECTING);
    expect(errored).toBe(false);

    // Connexion définitivement fermée → erreur (le composant repasse en polling).
    es.triggerError(MockEventSource.CLOSED);
    expect(errored).toBe(true);
  });

  it('ferme l’EventSource à la désinscription', () => {
    const sub = service.stream('c').subscribe();
    const es = MockEventSource.instances[0];
    sub.unsubscribe();
    expect(es.closed).toBe(true);
  });
});
