import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DashboardService } from '../core/services/dashboard.service';
import { DashboardSummary, DashboardDeadline } from '../core/models/dashboard.model';
import { fadeInUp } from '../shared/animations';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, DatePipe, MatIconModule, MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  animations: [fadeInUp],
  host: { '[@fadeInUp]': '' },
})
export class DashboardComponent implements OnInit {
  loading = signal(true);
  error = signal(false);
  summary = signal<DashboardSummary | null>(null);

  constructor(private dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.load();
  }

  retry(): void {
    this.error.set(false);
    this.loading.set(true);
    this.load();
  }

  scrollTo(sectionId: string): void {
    document.getElementById(sectionId)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  private load(): void {
    this.dashboardService.getSummary().subscribe({
      next: data => {
        this.summary.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      }
    });
  }

  daysUntil(dueDateStr: string): number {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const due = new Date(dueDateStr);
    due.setHours(0, 0, 0, 0);
    return Math.round((due.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
  }

  deadlineClass(d: DashboardDeadline): string {
    const days = this.daysUntil(d.dueDate);
    if (days <= 3) return 'urgent-critical';
    return 'urgent-warn';
  }

  domainColor(domain: string): string {
    switch (domain) {
      case 'DROIT_DU_TRAVAIL':  return '#27AE60';
      case 'DROIT_FAMILLE':     return '#C9973A';
      case 'DROIT_IMMIGRATION': return '#1A3A5C';
      default: return '#1A3A5C';
    }
  }

  domainLabel(domain: string): string {
    switch (domain) {
      case 'DROIT_DU_TRAVAIL':  return 'Travail';
      case 'DROIT_FAMILLE':     return 'Famille';
      case 'DROIT_IMMIGRATION': return 'Immigration';
      default: return domain;
    }
  }

  analysisTypeLabel(type: string): string {
    return type === 'ENRICHED' ? 'Enrichie' : 'Standard';
  }
}
