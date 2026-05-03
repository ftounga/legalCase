import { TestBed } from '@angular/core/testing';
import { AnalysisSseService, AnalysisStatusEvent } from './analysis-sse.service';

class MockEventSource {
  static instances: MockEventSource[] = [];
  url: string;
  withCredentials: boolean;
  closed = false;
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
  }

  emit(name: string, data: AnalysisStatusEvent): void {
    const cbs = this.listeners.get(name) ?? [];
    cbs.forEach(cb => cb({ data: JSON.stringify(data) } as MessageEvent));
  }

  triggerError(): void {
    this.onerror?.call(this as unknown as EventSource, new Event('error'));
  }
}

describe('AnalysisSseService', () => {
  let service: AnalysisSseService;
  let originalEventSource: typeof EventSource | undefined;

  beforeEach(() => {
    originalEventSource = (globalThis as { EventSource?: typeof EventSource }).EventSource;
    (globalThis as { EventSource: unknown }).EventSource = MockEventSource;
    MockEventSource.instances = [];
    TestBed.configureTestingModule({});
    service = TestBed.inject(AnalysisSseService);
  });

  afterEach(() => {
    (globalThis as { EventSource: unknown }).EventSource = originalEventSource;
  });

  it('émet plusieurs événements consécutifs sans fermer le stream après le 1er', () => {
    const received: AnalysisStatusEvent[] = [];
    let completed = false;

    const sub = service.stream('case-1').subscribe({
      next: e => received.push(e),
      complete: () => { completed = true; },
    });

    const es = MockEventSource.instances[0];
    expect(es).toBeDefined();
    expect(es.url).toBe('/api/v1/case-files/case-1/analysis-status/stream');
    expect(es.withCredentials).toBe(true);

    es.emit('CASE_ANALYSIS_DONE', { caseFileId: 'case-1', status: 'DONE', jobType: 'CASE_ANALYSIS' });
    es.emit('DOCUMENT_ANALYSIS_DONE', { caseFileId: 'case-1', status: 'DONE', jobType: 'DOCUMENT_ANALYSIS' });
    es.emit('ENRICHED_ANALYSIS_DONE', { caseFileId: 'case-1', status: 'DONE', jobType: 'ENRICHED_ANALYSIS' });

    expect(received).toHaveLength(3);
    expect(received.map(e => e.jobType)).toEqual(['CASE_ANALYSIS', 'DOCUMENT_ANALYSIS', 'ENRICHED_ANALYSIS']);
    expect(es.closed).toBe(false);
    expect(completed).toBe(false);

    sub.unsubscribe();
    expect(es.closed).toBe(true);
  });

  it('émet les événements FAILED comme les DONE', () => {
    const received: AnalysisStatusEvent[] = [];
    service.stream('case-2').subscribe(e => received.push(e));

    const es = MockEventSource.instances[0];
    es.emit('CASE_ANALYSIS_FAILED', { caseFileId: 'case-2', status: 'FAILED', jobType: 'CASE_ANALYSIS' });

    expect(received).toEqual([{ caseFileId: 'case-2', status: 'FAILED', jobType: 'CASE_ANALYSIS' }]);
  });

  it('ferme et complete sur erreur réseau', () => {
    let completed = false;
    service.stream('case-3').subscribe({ complete: () => { completed = true; } });

    const es = MockEventSource.instances[0];
    es.triggerError();

    expect(es.closed).toBe(true);
    expect(completed).toBe(true);
  });

  it('ferme la source quand le souscripteur se désabonne', () => {
    const sub = service.stream('case-4').subscribe();
    const es = MockEventSource.instances[0];
    expect(es.closed).toBe(false);

    sub.unsubscribe();
    expect(es.closed).toBe(true);
  });
});
