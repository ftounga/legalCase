import { TestBed } from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ClientErrorService } from './client-error.service';

describe('ClientErrorService', () => {
  let service: ClientErrorService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        ClientErrorService
      ]
    });
    service = TestBed.inject(ClientErrorService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // S-01 : report() émet un POST vers /api/v1/logs/client-error
  it('emits a POST to /api/v1/logs/client-error', () => {
    service.report({ message: 'boom' });
    const req = httpMock.expectOne('/api/v1/logs/client-error');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.message).toBe('boom');
    expect(req.request.body.timestamp).toBeDefined();
    req.flush({ status: 'logged' });
  });

  // S-02 : dédup — même payload 2x = 1 seul POST
  it('deduplicates identical errors within the same session', () => {
    service.report({ message: 'duplicate', stack: 'at X' });
    service.report({ message: 'duplicate', stack: 'at X' });
    const reqs = httpMock.match('/api/v1/logs/client-error');
    expect(reqs.length).toBe(1);
    reqs[0].flush({ status: 'logged' });
  });

  // S-03 : backend 500 → service swallow (pas d'erreur propagée)
  it('swallows backend errors without throwing', () => {
    expect(() => {
      service.report({ message: 'server down' });
      const req = httpMock.expectOne('/api/v1/logs/client-error');
      req.flush('boom', { status: 500, statusText: 'ISE' });
    }).not.toThrow();
  });

  // S-04 : tronque le message > 500 chars avant POST
  it('truncates message to 500 chars', () => {
    const longMsg = 'a'.repeat(800);
    service.report({ message: longMsg });
    const req = httpMock.expectOne('/api/v1/logs/client-error');
    expect(req.request.body.message.length).toBe(500);
    req.flush({ status: 'logged' });
  });

  // S-05 : tronque la stack > 4000 chars
  it('truncates stack to 4000 chars', () => {
    const longStack = 'b'.repeat(5000);
    service.report({ message: 'ok', stack: longStack });
    const req = httpMock.expectOne('/api/v1/logs/client-error');
    expect(req.request.body.stack.length).toBe(4000);
    req.flush({ status: 'logged' });
  });

  // S-06 : reset dedup pour tests
  it('resetDedupForTest allows re-reporting same payload', () => {
    service.report({ message: 'same' });
    service.resetDedupForTest();
    service.report({ message: 'same' });
    const reqs = httpMock.match('/api/v1/logs/client-error');
    expect(reqs.length).toBe(2);
    reqs.forEach((r) => r.flush({ status: 'logged' }));
  });
});
