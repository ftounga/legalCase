import { TestBed } from '@angular/core/testing';
import { OnboardingWizardService } from './onboarding-wizard.service';

describe('OnboardingWizardService', () => {
  let service: OnboardingWizardService;
  const WS_ID = 'ws-test-123';

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(OnboardingWizardService);
    localStorage.removeItem(`onboarding_wizard_done_${WS_ID}`);
  });

  afterEach(() => {
    localStorage.removeItem(`onboarding_wizard_done_${WS_ID}`);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('shouldShow — clé absente → true', () => {
    expect(service.shouldShow(WS_ID)).toBeTrue();
  });

  it('shouldShow — clé présente → false', () => {
    localStorage.setItem(`onboarding_wizard_done_${WS_ID}`, '1');
    expect(service.shouldShow(WS_ID)).toBeFalse();
  });

  it('markDone — pose la clé localStorage', () => {
    service.markDone(WS_ID);
    expect(localStorage.getItem(`onboarding_wizard_done_${WS_ID}`)).toBe('1');
  });

  it('shouldShow — après markDone → false', () => {
    service.markDone(WS_ID);
    expect(service.shouldShow(WS_ID)).toBeFalse();
  });

  it('shouldShow — workspaceId null → false', () => {
    expect(service.shouldShow(null)).toBeFalse();
  });

  it('shouldShow — workspaceId undefined → false', () => {
    expect(service.shouldShow(undefined)).toBeFalse();
  });
});
