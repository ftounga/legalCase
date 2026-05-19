/**
 * F-240 SF-240-02 — Tests unitaires LegalConsentService
 * CS-01 : acceptConsent POST le bon URL et body
 * CS-02 : propage la réponse 201
 * CS-03 : propage les erreurs HTTP
 */
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { LegalConsentService, CURRENT_CONSENT_VERSION } from './legal-consent.service';
import { ConsentAcceptanceRequest, ConsentAcceptanceResponse } from '../models/consent.model';

describe('LegalConsentService', () => {
  let service: LegalConsentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        LegalConsentService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(LegalConsentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // CS-01 : acceptConsent POST le bon URL et body
  it('CS-01 : acceptConsent envoie POST /api/v1/consent/accept avec le bon body', () => {
    const request: ConsentAcceptanceRequest = {
      consentTypes: ['SIGNUP_TERMS', 'PRIVACY_POLICY'],
      version: CURRENT_CONSENT_VERSION
    };

    service.acceptConsent(request).subscribe();

    const req = httpMock.expectOne('/api/v1/consent/accept');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      consentTypes: ['SIGNUP_TERMS', 'PRIVACY_POLICY'],
      version: '2026-05-11'
    });
    req.flush({ acceptances: [] }, { status: 201, statusText: 'Created' });
  });

  // CS-02 : propage la réponse 201
  it('CS-02 : acceptConsent propage correctement la réponse 201', () => {
    const mockResponse: ConsentAcceptanceResponse = {
      acceptances: [
        {
          id: 'uuid-1',
          userId: 'user-1',
          consentType: 'SIGNUP_TERMS',
          version: CURRENT_CONSENT_VERSION,
          acceptedAt: '2026-05-11T10:00:00Z',
          acceptanceIp: null,
          acceptanceUserAgent: null,
          workspaceId: null
        }
      ]
    };

    let result: ConsentAcceptanceResponse | undefined;
    service.acceptConsent({ consentTypes: ['SIGNUP_TERMS', 'PRIVACY_POLICY'], version: CURRENT_CONSENT_VERSION })
      .subscribe(r => result = r);

    const req = httpMock.expectOne('/api/v1/consent/accept');
    req.flush(mockResponse, { status: 201, statusText: 'Created' });

    expect(result).toEqual(mockResponse);
  });

  // CS-03 : propage les erreurs HTTP
  it('CS-03 : acceptConsent propage les erreurs HTTP', () => {
    let errorReceived: any;
    service.acceptConsent({ consentTypes: ['SIGNUP_TERMS', 'PRIVACY_POLICY'], version: CURRENT_CONSENT_VERSION })
      .subscribe({
        next: () => fail('devrait échouer'),
        error: err => errorReceived = err
      });

    const req = httpMock.expectOne('/api/v1/consent/accept');
    req.flush({ message: 'Bad Request' }, { status: 400, statusText: 'Bad Request' });

    expect(errorReceived).toBeTruthy();
    expect(errorReceived.status).toBe(400);
  });

  it('CURRENT_CONSENT_VERSION vaut "2026-05-11"', () => {
    expect(CURRENT_CONSENT_VERSION).toBe('2026-05-11');
  });
});
