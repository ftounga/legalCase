import { TestBed } from '@angular/core/testing';
import { ConsentService } from './consent.service';

describe('ConsentService', () => {
  let service: ConsentService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ConsentService);
    localStorage.clear();
    document.getElementById('ga4-script')?.remove();
  });

  it('hasConsent() returns null when localStorage is empty', () => {
    expect(service.hasConsent()).toBeNull();
  });

  it('grantConsent() writes ga_consent=granted', () => {
    service.grantConsent();
    expect(localStorage.getItem('ga_consent')).toBe('granted');
  });

  it('grantConsent() does not inject GA4 script when gaId is empty (test env)', () => {
    service.grantConsent();
    // environment.gaId is '' in test environment — no script injected
    expect(document.getElementById('ga4-script')).toBeNull();
  });

  it('denyConsent() writes ga_consent=denied and does not inject GA4 script', () => {
    service.denyConsent();
    expect(localStorage.getItem('ga_consent')).toBe('denied');
    expect(document.getElementById('ga4-script')).toBeNull();
  });

  it('hasConsent() returns true after grantConsent()', () => {
    service.grantConsent();
    expect(service.hasConsent()).toBe(true);
  });

  it('hasConsent() returns false after denyConsent()', () => {
    service.denyConsent();
    expect(service.hasConsent()).toBe(false);
  });
});
