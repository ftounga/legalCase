import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

const CONSENT_KEY = 'ga_consent';

@Injectable({ providedIn: 'root' })
export class ConsentService {
  hasConsent(): boolean | null {
    try {
      const value = localStorage.getItem(CONSENT_KEY);
      if (value === 'granted') return true;
      if (value === 'denied') return false;
      return null;
    } catch {
      return null;
    }
  }

  grantConsent(): void {
    try {
      localStorage.setItem(CONSENT_KEY, 'granted');
    } catch {
      // localStorage unavailable — proceed without persisting
    }
    this.loadGa4();
  }

  denyConsent(): void {
    try {
      localStorage.setItem(CONSENT_KEY, 'denied');
    } catch {
      // localStorage unavailable
    }
  }

  initIfConsented(): void {
    if (this.hasConsent() === true) {
      this.loadGa4();
    }
  }

  private loadGa4(): void {
    const id = environment.gaId;
    if (!id) return;
    if (document.getElementById('ga4-script')) return;

    const script = document.createElement('script');
    script.id = 'ga4-script';
    script.async = true;
    script.src = `https://www.googletagmanager.com/gtag/js?id=${id}`;
    document.head.appendChild(script);

    const inline = document.createElement('script');
    inline.text = `
      window.dataLayer = window.dataLayer || [];
      function gtag(){dataLayer.push(arguments);}
      gtag('js', new Date());
      gtag('config', '${id}');
    `;
    document.head.appendChild(inline);
  }
}
