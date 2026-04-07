import { AnalyticsService } from './analytics.service';
import { ConsentService } from './consent.service';
import * as env from '../../../environments/environment';

describe('AnalyticsService', () => {
  let service: AnalyticsService;
  let consentService: jest.Mocked<ConsentService>;

  beforeEach(() => {
    consentService = jasmine.createSpyObj('ConsentService', ['hasConsent', 'grantConsent', 'denyConsent', 'initIfConsented']);
    service = new AnalyticsService(consentService);
    sessionStorage.clear();
  });

  afterEach(() => {
    delete (window as any).gtag;
    sessionStorage.clear();
  });

  // --- trackEvent (existing) ---

  it('calls gtag with event name and params when gtag is present', () => {
    const gtag = jest.fn();
    (window as any).gtag = gtag;

    service.trackEvent('analysis_launched', { type: 'STANDARD' });

    expect(gtag).toHaveBeenCalledWith('event', 'analysis_launched', { type: 'STANDARD' });
  });

  it('calls gtag with empty params when no params provided', () => {
    const gtag = jest.fn();
    (window as any).gtag = gtag;

    service.trackEvent('pdf_exported');

    expect(gtag).toHaveBeenCalledWith('event', 'pdf_exported', {});
  });

  it('does not throw when gtag is absent', () => {
    delete (window as any).gtag;

    expect(() => service.trackEvent('upgrade_clicked')).not.toThrow();
  });

  it('swallows exception thrown by gtag', () => {
    (window as any).gtag = () => { throw new Error('GA4 error'); };

    expect(() => service.trackEvent('analysis_launched')).not.toThrow();
  });

  // --- captureUtmParams ---

  it('captures UTM params from URL into sessionStorage', () => {
    Object.defineProperty(window, 'location', {
      value: { ...window.location, search: '?utm_source=google&utm_medium=cpc&utm_campaign=launch&gclid=abc123' },
      writable: true
    });

    service.captureUtmParams();

    const stored = JSON.parse(sessionStorage.getItem('ads_utm_params')!);
    expect(stored.utm_source).toBe('google');
    expect(stored.utm_medium).toBe('cpc');
    expect(stored.utm_campaign).toBe('launch');
    expect(stored.gclid).toBe('abc123');
  });

  it('does not store anything when no UTM params in URL', () => {
    Object.defineProperty(window, 'location', {
      value: { ...window.location, search: '' },
      writable: true
    });

    service.captureUtmParams();

    expect(sessionStorage.getItem('ads_utm_params')).toBeNull();
  });

  // --- trackConversion ---

  it('calls gtag conversion with correct ID and UTM params', () => {
    const gtag = jest.fn();
    (window as any).gtag = gtag;
    consentService.hasConsent.mockReturnValue(true);
    sessionStorage.setItem('ads_utm_params', JSON.stringify({ utm_source: 'google', gclid: 'xyz' }));
    Object.defineProperty(env, 'environment', {
      value: { ...env.environment, googleAdsConversionId: 'AW-123/abc' },
      writable: true
    });

    service.trackConversion();

    expect(gtag).toHaveBeenCalledWith('event', 'conversion', {
      send_to: 'AW-123/abc',
      utm_source: 'google',
      gclid: 'xyz'
    });
  });

  it('does not fire conversion when consent is not granted', () => {
    const gtag = jest.fn();
    (window as any).gtag = gtag;
    consentService.hasConsent.mockReturnValue(false);
    Object.defineProperty(env, 'environment', {
      value: { ...env.environment, googleAdsConversionId: 'AW-123/abc' },
      writable: true
    });

    service.trackConversion();

    expect(gtag).not.toHaveBeenCalled();
  });

  it('does not fire conversion when conversionId is empty', () => {
    const gtag = jest.fn();
    (window as any).gtag = gtag;
    consentService.hasConsent.mockReturnValue(true);
    Object.defineProperty(env, 'environment', {
      value: { ...env.environment, googleAdsConversionId: '' },
      writable: true
    });

    service.trackConversion();

    expect(gtag).not.toHaveBeenCalled();
  });

  it('does not throw when gtag is absent during trackConversion', () => {
    delete (window as any).gtag;
    consentService.hasConsent.mockReturnValue(true);
    Object.defineProperty(env, 'environment', {
      value: { ...env.environment, googleAdsConversionId: 'AW-123/abc' },
      writable: true
    });

    expect(() => service.trackConversion()).not.toThrow();
  });
});
