import { AnalyticsService } from './analytics.service';

describe('AnalyticsService', () => {
  let service: AnalyticsService;

  beforeEach(() => {
    service = new AnalyticsService();
  });

  afterEach(() => {
    delete (window as any).gtag;
  });

  it('calls gtag with event name and params when gtag is present', () => {
    const gtag = jasmine.createSpy('gtag');
    (window as any).gtag = gtag;

    service.trackEvent('analysis_launched', { type: 'STANDARD' });

    expect(gtag).toHaveBeenCalledWith('event', 'analysis_launched', { type: 'STANDARD' });
  });

  it('calls gtag with empty params when no params provided', () => {
    const gtag = jasmine.createSpy('gtag');
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
});
