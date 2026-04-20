import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { BillingService } from './billing.service';
import { Workspace } from '../models/workspace.model';

const freeWorkspace: Workspace = {
  id: 'ws1', name: 'Test', slug: 'test', planCode: 'FREE', status: 'ACTIVE',
  expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString()
};

const starterWorkspace: Workspace = {
  id: 'ws2', name: 'Test', slug: 'test', planCode: 'STARTER', status: 'ACTIVE'
};

describe('BillingService', () => {
  let service: BillingService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(BillingService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('createCheckoutSession — POST /api/v1/stripe/checkout-session', () => {
    service.createCheckoutSession('STARTER').subscribe(res => {
      expect(res.checkoutUrl).toBe('https://checkout.stripe.com/pay/test');
    });
    const req = http.expectOne('/api/v1/stripe/checkout-session');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ planCode: 'STARTER' });
    req.flush({ checkoutUrl: 'https://checkout.stripe.com/pay/test' });
  });

  it('createTopupSession — POST /api/v1/stripe/topup-session', () => {
    service.createTopupSession('TOKENS_5M').subscribe(res => {
      expect(res.checkoutUrl).toBe('https://checkout.stripe.com/pay/topup');
    });
    const req = http.expectOne('/api/v1/stripe/topup-session');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ packCode: 'TOKENS_5M' });
    req.flush({ checkoutUrl: 'https://checkout.stripe.com/pay/topup' });
  });

  it('shouldShowTrialBanner — FREE plan, première fois → true', () => {
    expect(service.shouldShowTrialBanner(freeWorkspace)).toBe(true);
  });

  it('shouldShowTrialBanner — FREE plan, deuxième fois → false (déjà montré)', () => {
    service.shouldShowTrialBanner(freeWorkspace); // première fois
    expect(service.shouldShowTrialBanner(freeWorkspace)).toBe(false);
  });

  it('shouldShowTrialBanner — plan STARTER → false', () => {
    expect(service.shouldShowTrialBanner(starterWorkspace)).toBe(false);
  });

  // SF-123-03 : résumé seats
  it('getSeatsSummary — GET /api/v1/billing/seats-summary', () => {
    const mockResponse = {
      planCode: 'TEAM', seatCount: 4, includedSeats: 3, maxSeats: 6,
      extraSeatPriceCents: 5900, baseMonthlyCostCents: 21900, totalMonthlyCostCents: 27800
    };
    service.getSeatsSummary().subscribe(res => {
      expect(res.planCode).toBe('TEAM');
      expect(res.totalMonthlyCostCents).toBe(27800);
    });
    const req = http.expectOne('/api/v1/billing/seats-summary');
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });
});
