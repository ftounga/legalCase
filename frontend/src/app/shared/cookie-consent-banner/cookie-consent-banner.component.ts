import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConsentService } from '../../core/services/consent.service';

@Component({
  selector: 'app-cookie-consent-banner',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './cookie-consent-banner.component.html',
  styleUrl: './cookie-consent-banner.component.scss'
})
export class CookieConsentBannerComponent implements OnInit {
  private consentService = inject(ConsentService);

  visible = false;

  ngOnInit(): void {
    this.visible = this.consentService.hasConsent() === null;
  }

  accept(): void {
    this.consentService.grantConsent();
    this.visible = false;
  }

  deny(): void {
    this.consentService.denyConsent();
    this.visible = false;
  }
}
