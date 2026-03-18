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

  it('shouldShowTrialBanner — FREE plan, première fois → true', () => {
    expect(service.shouldShowTrialBanner(freeWorkspace)).toBeTrue();
  });

  it('shouldShowTrialBanner — FREE plan, deuxième fois → false (déjà montré)', () => {
    service.shouldShowTrialBanner(freeWorkspace); // première fois
    expect(service.shouldShowTrialBanner(freeWorkspace)).toBeFalse();
  });

  it('shouldShowTrialBanner — plan STARTER → false', () => {
    expect(service.shouldShowTrialBanner(starterWorkspace)).toBeFalse();
  });
});
