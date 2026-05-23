import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HttpErrorResponse } from '@angular/common/http';

import {
  PromoCodeRedemptionService,
  PROMO_CODE_ERROR_MESSAGES_FR,
  PROMO_CODE_FALLBACK_ERROR_FR,
  mapPromoCodeError,
} from './promo-code-redemption.service';
import { PromoCodeErrorCode, RedeemResponse } from './promo-code-redemption.model';

describe('PromoCodeRedemptionService', () => {
  let service: PromoCodeRedemptionService;
  let httpMock: HttpTestingController;
  const WORKSPACE_ID = 'ws-1234';
  const URL = `/api/v1/workspaces/${WORKSPACE_ID}/billing/promo-codes/redeem`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PromoCodeRedemptionService],
    });
    service = TestBed.inject(PromoCodeRedemptionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('redeem() POSTs the code on the workspace-scoped endpoint', () => {
    const expected: RedeemResponse = {
      newExpiresAt: '2026-06-30T23:59:59Z',
      addedDays: 30,
      partnerLabel: 'Adhérents ACE',
    };

    service.redeem(WORKSPACE_ID, 'ACE2026').subscribe(res => {
      expect(res).toEqual(expected);
    });

    const req = httpMock.expectOne(URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ code: 'ACE2026' });
    req.flush(expected);
  });

  it('redeem() propagates HttpErrorResponse to caller', () => {
    let captured: unknown = null;
    service.redeem(WORKSPACE_ID, 'BAD').subscribe({
      next: () => fail('should error'),
      error: e => (captured = e),
    });

    httpMock.expectOne(URL).flush(
      { error: 'Conflict', message: 'Code invalide', code: 'PROMO_CODE_NOT_FOUND' },
      { status: 404, statusText: 'Not Found' }
    );

    expect(captured).toBeInstanceOf(HttpErrorResponse);
  });
});

describe('mapPromoCodeError — mapping codes applicatifs → message FR', () => {
  const cases: { code: PromoCodeErrorCode; expectedSubstr: string }[] = [
    { code: 'PROMO_CODE_NOT_FOUND', expectedSubstr: 'invalide' },
    { code: 'PROMO_CODE_INACTIVE', expectedSubstr: 'plus actif' },
    { code: 'PROMO_CODE_EXPIRED', expectedSubstr: 'expiré' },
    { code: 'PROMO_CODE_EXHAUSTED', expectedSubstr: 'nombre maximal' },
    { code: 'WORKSPACE_NOT_TRIALING', expectedSubstr: 'essai est terminé' },
    { code: 'WORKSPACE_ALREADY_REDEEMED_TRIAL_EXTENSION', expectedSubstr: 'déjà appliqué' },
    { code: 'PROMO_CODE_TYPE_NOT_SUPPORTED_YET', expectedSubstr: 'prochainement' },
    { code: 'FORBIDDEN_WORKSPACE', expectedSubstr: 'non autorisée' },
  ];

  for (const { code, expectedSubstr } of cases) {
    it(`${code} → message FR contient « ${expectedSubstr} »`, () => {
      const msg = mapPromoCodeError({ error: { code } });
      expect(msg).toBe(PROMO_CODE_ERROR_MESSAGES_FR[code]);
      expect(msg.toLowerCase()).toContain(expectedSubstr.toLowerCase());
    });
  }

  it('fallback générique si code inconnu', () => {
    expect(mapPromoCodeError({ error: { code: 'WHATEVER' } })).toBe(PROMO_CODE_FALLBACK_ERROR_FR);
  });

  it('fallback générique si body sans code', () => {
    expect(mapPromoCodeError({ error: {} })).toBe(PROMO_CODE_FALLBACK_ERROR_FR);
  });

  it('fallback générique si erreur réseau (pas de body)', () => {
    expect(mapPromoCodeError(new Error('network'))).toBe(PROMO_CODE_FALLBACK_ERROR_FR);
  });

  it('aucun message ne contient les wordings interdits (gratuit/cadeau/promotion)', () => {
    const forbidden = /(gratuit|cadeau|promotion)/i;
    for (const msg of Object.values(PROMO_CODE_ERROR_MESSAGES_FR)) {
      expect(msg).not.toMatch(forbidden);
    }
    expect(PROMO_CODE_FALLBACK_ERROR_FR).not.toMatch(forbidden);
  });
});
