import { Component, OnInit } from '@angular/core';
import { AsyncPipe, NgIf } from '@angular/common';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HomeService, HealthResponse } from './home.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [AsyncPipe, NgIf, MatCardModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit {
  health$!: Observable<HealthResponse | null>;

  constructor(private homeService: HomeService) {}

  ngOnInit(): void {
    this.health$ = this.homeService.getHello().pipe(
      catchError(() => of(null))
    );
  }
}
