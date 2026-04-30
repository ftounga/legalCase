import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { paymentRequiredInterceptor } from './payment-required.interceptor';
import { QuotaErrorStateService } from '../services/quota-error-state.service';

describe('paymentRequiredInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let quotaErrorState: QuotaErrorStateService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([paymentRequiredInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    quotaErrorState = TestBed.inject(QuotaErrorStateService);
  });

  afterEach(() => {
    httpMock.verify();
    quotaErrorState.clear();
  });

  it('pousse une QuotaError dans QuotaErrorState sur 402 avec code', (done) => {
    http.post('/api/v1/case-files/abc/analyze', {}).subscribe({
      next: () => fail('expected error'),
      error: (err: HttpErrorResponse) => {
        expect(err.status).toBe(402);
        const state = quotaErrorState.error();
        expect(state).not.toBeNull();
        expect(state?.code).toBe('TOKEN_BUDGET_EXCEEDED');
        expect(state?.message).toBe('Budget tokens mensuel dépassé.');
        expect(state?.sourceUrl).toBe('/api/v1/case-files/abc/analyze');
        expect(state?.receivedAt).toBeGreaterThan(0);
        done();
      },
    });

    const req = httpMock.expectOne('/api/v1/case-files/abc/analyze');
    req.flush(
      { error: '402 PAYMENT_REQUIRED', message: 'Budget tokens mensuel dépassé.', code: 'TOKEN_BUDGET_EXCEEDED' },
      { status: 402, statusText: 'Payment Required' }
    );
  });

  it('fallback générique sur 402 sans code (legacy)', (done) => {
    http.post('/api/v1/foo', {}).subscribe({
      next: () => fail('expected error'),
      error: () => {
        const state = quotaErrorState.error();
        expect(state?.code).toBeNull();
        expect(state?.message).toBe('Backend legacy message');
        done();
      },
    });

    const req = httpMock.expectOne('/api/v1/foo');
    req.flush(
      { error: '402', message: 'Backend legacy message' },
      { status: 402, statusText: 'Payment Required' }
    );
  });

  it('fallback message par défaut si body 402 vide', (done) => {
    http.post('/api/v1/foo', {}).subscribe({
      next: () => fail('expected error'),
      error: () => {
        const state = quotaErrorState.error();
        expect(state?.code).toBeNull();
        expect(state?.message).toBe('Limite atteinte');
        done();
      },
    });

    const req = httpMock.expectOne('/api/v1/foo');
    req.flush(null, { status: 402, statusText: 'Payment Required' });
  });

  it('ne touche pas au state sur 200', () => {
    http.get('/api/v1/foo').subscribe();
    const req = httpMock.expectOne('/api/v1/foo');
    req.flush({ ok: true });
    expect(quotaErrorState.error()).toBeNull();
  });

  it('ne touche pas au state sur erreurs ≠ 402', (done) => {
    http.get('/api/v1/foo').subscribe({
      next: () => fail('expected error'),
      error: () => {
        expect(quotaErrorState.error()).toBeNull();
        done();
      },
    });
    const req = httpMock.expectOne('/api/v1/foo');
    req.flush({ message: 'server error' }, { status: 500, statusText: 'Server Error' });
  });

  it('successive 402s écrase le state avec la dernière (pas d\'empilement)', (done) => {
    http.post('/api/v1/a', {}).subscribe({ error: () => {} });
    httpMock.expectOne('/api/v1/a').flush(
      { code: 'TOKEN_BUDGET_EXCEEDED', message: 'msg1' },
      { status: 402, statusText: 'Payment Required' }
    );

    http.post('/api/v1/b', {}).subscribe({
      error: () => {
        expect(quotaErrorState.error()?.code).toBe('CHAT_MESSAGE_LIMIT_EXCEEDED');
        expect(quotaErrorState.error()?.sourceUrl).toBe('/api/v1/b');
        done();
      },
    });
    httpMock.expectOne('/api/v1/b').flush(
      { code: 'CHAT_MESSAGE_LIMIT_EXCEEDED', message: 'msg2' },
      { status: 402, statusText: 'Payment Required' }
    );
  });
});
