import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { ConsentService } from './consent.service';

declare const window: Window & { gtag?: (...args: unknown[]) => void };

const UTM_KEYS = ['utm_source', 'utm_medium', 'utm_campaign', 'utm_term', 'utm_content', 'gclid'] as const;
const UTM_STORAGE_KEY = 'ads_utm_params';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  constructor(private consentService: ConsentService) {}

  trackEvent(name: string, params?: Record<string, unknown>): void {
    try {
      if (typeof window.gtag === 'function') {
        window.gtag('event', name, params ?? {});
      }
    } catch {
      // fail-open — never block the user action
    }
  }

  captureUtmParams(): void {
    try {
      const params = new URLSearchParams(window.location.search);
      const captured: Record<string, string> = {};
      for (const key of UTM_KEYS) {
        const value = params.get(key);
        if (value) {
          captured[key] = value;
        }
      }
      if (Object.keys(captured).length > 0) {
        sessionStorage.setItem(UTM_STORAGE_KEY, JSON.stringify(captured));
      }
    } catch {
      // fail-open — sessionStorage unavailable
    }
  }

  trackConversion(): void {
    try {
      const conversionId = environment.googleAdsConversionId;
      if (!conversionId) return;
      if (this.consentService.hasConsent() !== true) return;
      if (typeof window.gtag !== 'function') return;

      const utmParams = this.getStoredUtmParams();
      window.gtag('event', 'conversion', {
        send_to: conversionId,
        ...utmParams
      });
    } catch {
      // fail-open — never block the user action
    }
  }

  private getStoredUtmParams(): Record<string, string> {
    try {
      const raw = sessionStorage.getItem(UTM_STORAGE_KEY);
      return raw ? JSON.parse(raw) : {};
    } catch {
      return {};
    }
  }
}
