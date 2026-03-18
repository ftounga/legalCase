import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TrialBannerComponent } from './trial-banner.component';
import { BillingService } from '../../core/services/billing.service';
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
  let billingServiceSpy: jasmine.SpyObj<BillingService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    billingServiceSpy = jasmine.createSpyObj('BillingService', ['shouldShowTrialBanner']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [TrialBannerComponent, NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        { provide: BillingService, useValue: billingServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TrialBannerComponent);
    component = fixture.componentInstance;
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('visible si plan FREE et shouldShowTrialBanner → true', () => {
    billingServiceSpy.shouldShowTrialBanner.and.returnValue(true);
    component.workspace = freeWorkspace;
    component.ngOnChanges();
    expect(component.visible()).toBeTrue();
  });

  it('non visible si plan STARTER', () => {
    billingServiceSpy.shouldShowTrialBanner.and.returnValue(false);
    component.workspace = starterWorkspace;
    component.ngOnChanges();
    expect(component.visible()).toBeFalse();
  });

  it('goToPlans — navigue vers /workspace/billing', () => {
    component.goToPlans();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/workspace/billing']);
  });

  it('dismiss — masque la bannière', () => {
    billingServiceSpy.shouldShowTrialBanner.and.returnValue(true);
    component.workspace = freeWorkspace;
    component.ngOnChanges();
    component.dismiss();
    expect(component.visible()).toBeFalse();
  });
});
