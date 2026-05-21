import { TestBed } from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { ClientErrorService } from './client-error.service';
import { clientErrorHttpInterceptor } from './client-error-http.interceptor';

describe('clientErrorHttpInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let clientErrorService: { report: jest.Mock };

  beforeEach(() => {
    clientErrorService = { report: jest.fn() };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([clientErrorHttpInterceptor])),
        provideHttpClientTesting(),
        { provide: ClientErrorService, useValue: clientErrorService }
      ]
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // X-01 : 500 → report appelé + erreur re-thrown
  it('reports HTTP 5xx errors and re-throws', (done) => {
    http.get('/api/v1/things').subscribe({
      next: () => done.fail('should error'),
      error: (err) => {
        expect(err.status).toBe(500);
        expect(clientErrorService.report).toHaveBeenCalledTimes(1);
        const payload = clientErrorService.report.mock.calls[0][0];
        expect(payload.message).toContain('HTTP 500');
        expect(payload.message).toContain('/api/v1/things');
        done();
      }
    });
    const req = httpMock.expectOne('/api/v1/things');
    req.flush('boom', { status: 500, statusText: 'ISE' });
  });

  // X-02 : 404 → report NON appelé
  it('does NOT report 4xx errors', (done) => {
    http.get('/api/v1/missing').subscribe({
      next: () => done.fail('should error'),
      error: () => {
        expect(clientErrorService.report).not.toHaveBeenCalled();
        done();
      }
    });
    const req = httpMock.expectOne('/api/v1/missing');
    req.flush('not found', { status: 404, statusText: 'NF' });
  });

  // X-03 : 200 → report NON appelé
  it('does NOT report 200 responses', () => {
    http.get('/api/v1/ok').subscribe();
    const req = httpMock.expectOne('/api/v1/ok');
    req.flush({});
    expect(clientErrorService.report).not.toHaveBeenCalled();
  });

  // X-04 : anti-boucle — un 500 sur l'endpoint lui-même n'est pas reporté
  it('does NOT re-report a 500 from the self-reporting endpoint', (done) => {
    http.post('/api/v1/logs/client-error', { message: 'x' }).subscribe({
      next: () => done.fail('should error'),
      error: () => {
        expect(clientErrorService.report).not.toHaveBeenCalled();
        done();
      }
    });
    const req = httpMock.expectOne('/api/v1/logs/client-error');
    req.flush('boom', { status: 500, statusText: 'ISE' });
  });

  // X-05 : 503 → report appelé (full 5xx range)
  it('reports 503 service unavailable', (done) => {
    http.get('/api/v1/whatever').subscribe({
      next: () => done.fail('should error'),
      error: () => {
        expect(clientErrorService.report).toHaveBeenCalled();
        const payload = clientErrorService.report.mock.calls[0][0];
        expect(payload.message).toContain('HTTP 503');
        done();
      }
    });
    const req = httpMock.expectOne('/api/v1/whatever');
    req.flush('boom', { status: 503, statusText: 'SU' });
  });
});
