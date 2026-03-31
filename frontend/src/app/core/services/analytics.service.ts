import { Injectable } from '@angular/core';

declare const window: Window & { gtag?: (...args: unknown[]) => void };

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  trackEvent(name: string, params?: Record<string, unknown>): void {
    try {
      if (typeof window.gtag === 'function') {
        window.gtag('event', name, params ?? {});
      }
    } catch {
      // fail-open — never block the user action
    }
  }
}
