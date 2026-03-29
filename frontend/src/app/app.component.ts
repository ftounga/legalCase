import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TourService } from './core/services/tour.service';
import { TourOverlayComponent } from './tour/tour-overlay.component';
import { routeAnimations } from './shared/animations';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, TourOverlayComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
  animations: [routeAnimations],
})
export class AppComponent {
  protected tourService = inject(TourService);
  title = 'frontend';

  getRouteState(outlet: RouterOutlet) {
    return outlet.isActivated ? outlet.activatedRoute : '';
  }
}
