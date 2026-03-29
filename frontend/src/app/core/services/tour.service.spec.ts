import { TestBed } from '@angular/core/testing';
import { TourService } from './tour.service';

describe('TourService', () => {
  let service: TourService;
  const WS_ID = 'ws-test-1';
  const STORAGE_KEY = 'onboarding_tour_done_' + WS_ID;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(TourService);
  });

  afterEach(() => localStorage.clear());

  // U-01 : shouldShow — clé absente → true
  it('U-01: shouldShow returns true when key absent', () => {
    expect(service.shouldShow(WS_ID)).toBeTrue();
  });

  // U-02 : shouldShow — clé présente → false
  it('U-02: shouldShow returns false when key present', () => {
    localStorage.setItem(STORAGE_KEY, '1');
    expect(service.shouldShow(WS_ID)).toBeFalse();
  });

  // U-03 : shouldShow — workspaceId null → false
  it('U-03: shouldShow returns false for null workspaceId', () => {
    expect(service.shouldShow(null)).toBeFalse();
  });

  // U-04 : start() → isActive = true, currentStep = 0
  it('U-04: start() activates tour at step 0', () => {
    service.start(WS_ID);
    expect(service.isActive()).toBeTrue();
    expect(service.currentStep()).toBe(0);
  });

  // U-05 : next() → currentStep = 1
  it('U-05: next() advances to step 1', () => {
    service.start(WS_ID);
    service.next();
    expect(service.currentStep()).toBe(1);
  });

  // U-06 : next() sur étape 4 → isActive = false, localStorage posé
  it('U-06: next() on last step stops tour and sets localStorage', () => {
    service.start(WS_ID);
    service.next(); // → 1
    service.next(); // → 2
    service.next(); // → 3
    service.next(); // → 4
    service.next(); // → stop (5 >= TOTAL_STEPS)
    expect(service.isActive()).toBeFalse();
    expect(localStorage.getItem(STORAGE_KEY)).toBe('1');
  });

  // U-07 : skip() → isActive = false, localStorage posé
  it('U-07: skip() stops tour and sets localStorage', () => {
    service.start(WS_ID);
    service.skip();
    expect(service.isActive()).toBeFalse();
    expect(localStorage.getItem(STORAGE_KEY)).toBe('1');
  });
});
