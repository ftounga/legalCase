import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { TourService } from './core/services/tour.service';
import { TourOverlayComponent } from './tour/tour-overlay.component';
import { CookieConsentBannerComponent } from './shared/cookie-consent-banner/cookie-consent-banner.component';
import { ConsentService } from './core/services/consent.service';
import { LoadingService } from './core/services/loading.service';
import { HelpChatWidgetComponent } from './help-chat-widget/help-chat-widget.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, TourOverlayComponent, CookieConsentBannerComponent, MatProgressBarModule, HelpChatWidgetComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent implements OnInit {
  protected tourService = inject(TourService);
  private consentService = inject(ConsentService);
  readonly loading = inject(LoadingService);
  title = 'frontend';

  ngOnInit(): void {
    this.consentService.initIfConsented();
  }
}
