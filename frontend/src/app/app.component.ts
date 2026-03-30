import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TourService } from './core/services/tour.service';
import { TourOverlayComponent } from './tour/tour-overlay.component';
import { CookieConsentBannerComponent } from './shared/cookie-consent-banner/cookie-consent-banner.component';
import { ConsentService } from './core/services/consent.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, TourOverlayComponent, CookieConsentBannerComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit {
  protected tourService = inject(TourService);
  private consentService = inject(ConsentService);
  title = 'frontend';

  ngOnInit(): void {
    this.consentService.initIfConsented();
  }
}
