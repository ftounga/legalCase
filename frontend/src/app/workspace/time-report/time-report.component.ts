import { Component, OnInit, signal, computed } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { TimeReportService } from '../../core/services/time-report.service';
import { TimeService } from '../../core/services/time.service';
import { TimeReportRow, BillingRateResponse } from '../../core/models/time-tracking.models';

@Component({
  selector: 'app-time-report',
  standalone: true,
  imports: [
    DecimalPipe,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './time-report.component.html',
  styleUrl: './time-report.component.scss'
})
export class TimeReportComponent implements OnInit {
  readonly months = this.buildMonths();
  selectedMonth = signal(this.months[0].value);

  rows = signal<TimeReportRow[]>([]);
  loading = signal(false);
  loadError = signal(false);
  exporting = signal(false);

  billingRate = signal<BillingRateResponse | null>(null);
  billingRateInput = signal<number | null>(null);
  savingBillingRate = signal(false);

  readonly totalSeconds = computed(() =>
    this.rows().reduce((acc, r) => acc + r.totalSeconds, 0)
  );
  readonly totalAmount = computed(() =>
    this.rows().reduce((acc, r) => acc + (r.amount ?? 0), 0)
  );

  constructor(
    private reportService: TimeReportService,
    private timeService: TimeService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.load();
    this.timeService.getBillingRate().subscribe({
      next: rate => {
        this.billingRate.set(rate);
        if (rate) this.billingRateInput.set(rate.ratePerHour);
      },
      error: () => {}
    });
  }

  onMonthChange(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.reportService.getMyReport(this.selectedMonth()).subscribe({
      next: rows => {
        this.rows.set(rows);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set(true);
        this.loading.set(false);
      }
    });
  }

  exportCsv(): void {
    this.exporting.set(true);
    this.snackBar.open('Export en cours…', undefined, { duration: 3000 });
    this.reportService.exportCsv(this.selectedMonth()).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `rapport-temps-${this.selectedMonth()}.csv`;
        a.click();
        URL.revokeObjectURL(url);
        this.exporting.set(false);
        this.snackBar.open('Export terminé', 'Fermer', { duration: 3000, panelClass: ['snack-success'] });
      },
      error: () => {
        this.exporting.set(false);
        this.snackBar.open('Erreur lors de l\'export.', 'Fermer', { duration: 4000, panelClass: ['snack-error'] });
      }
    });
  }

  saveBillingRate(): void {
    const rate = this.billingRateInput();
    if (rate == null || rate <= 0) return;
    this.savingBillingRate.set(true);
    this.timeService.saveBillingRate(rate).subscribe({
      next: saved => {
        this.billingRate.set(saved);
        this.billingRateInput.set(saved.ratePerHour);
        this.savingBillingRate.set(false);
        this.snackBar.open('Taux horaire enregistré', 'Fermer', { duration: 3000, panelClass: ['snack-success'] });
      },
      error: () => {
        this.savingBillingRate.set(false);
        this.snackBar.open('Erreur lors de l\'enregistrement.', 'Fermer', { duration: 4000, panelClass: ['snack-error'] });
      }
    });
  }

  formatDuration(seconds: number): string {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    if (h > 0) return `${h}h ${String(m).padStart(2, '0')}min`;
    if (m > 0) return `${m}min`;
    return `${seconds}s`;
  }

  private buildMonths(): { label: string; value: string }[] {
    const result: { label: string; value: string }[] = [];
    const now = new Date();
    for (let i = 0; i < 13; i++) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      const value = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
      const label = d.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
      result.push({ value, label });
    }
    return result;
  }
}
