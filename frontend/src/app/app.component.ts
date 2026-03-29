import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TourService } from './core/services/tour.service';
import { TourOverlayComponent } from './tour/tour-overlay.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, TourOverlayComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  protected tourService = inject(TourService);
  title = 'frontend';
}
