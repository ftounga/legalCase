import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TrialBannerComponent } from './trial-banner.component';
import { BillingService } from '../../core/services/billing.service';
import { AnalyticsService } from '../../core/services/analytics.service';
import { Router } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { Workspace } from '../../core/models/workspace.model';

const freeWorkspace: Workspace = {
  id: 'ws1', name: 'Test', slug: 'test', planCode: 'FREE', status: 'ACTIVE',
  expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString()
};

const starterWorkspace: Workspace = {
  id: 'ws2', name: 'Test', slug: 'test', planCode: 'STARTER', status: 'ACTIVE'
};

describe('TrialBannerComponent', () => {
  let component: TrialBannerComponent;
  let fixture: ComponentFixture<TrialBannerComponent>;
  let billingServiceSpy: jest.Mocked<BillingService>;
  let routerSpy: jest.Mocked<Router>;

  beforeEach(async () => {
    billingServiceSpy = jasmine.createSpyObj('BillingService', ['shouldShowTrialBanner']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [TrialBannerComponent, NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        { provide: BillingService, useValue: billingServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: AnalyticsService, useValue: jasmine.createSpyObj('AnalyticsService', ['trackEvent']) }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TrialBannerComponent);
    component = fixture.componentInstance;
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('visible si plan FREE et shouldShowTrialBanner → true', () => {
    billingServiceSpy.shouldShowTrialBanner.mockReturnValue(true);
    component.workspace = freeWorkspace;
    component.ngOnChanges();
    expect(component.visible()).toBe(true);
  });

  it('non visible si plan STARTER', () => {
    billingServiceSpy.shouldShowTrialBanner.mockReturnValue(false);
    component.workspace = starterWorkspace;
    component.ngOnChanges();
    expect(component.visible()).toBe(false);
  });

  it('goToPlans — navigue vers /workspace/billing', () => {
    component.goToPlans();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/workspace/billing']);
  });

  it('goToPlans — trackEvent upgrade_clicked', () => {
    const analyticsService = TestBed.inject(AnalyticsService) as jest.Mocked<AnalyticsService>;
    component.goToPlans();
    expect(analyticsService.trackEvent).toHaveBeenCalledWith('upgrade_clicked');
  });

  it('dismiss — masque la bannière', () => {
    billingServiceSpy.shouldShowTrialBanner.mockReturnValue(true);
    component.workspace = freeWorkspace;
    component.ngOnChanges();
    component.dismiss();
    expect(component.visible()).toBe(false);
  });
});
