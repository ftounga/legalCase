import { Component, OnInit, signal, ViewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuditLogService } from '../../core/services/audit-log.service';
import { AuditLogEntry } from '../../core/models/audit-log-entry.model';

@Component({
  selector: 'app-audit-log-screen',
  standalone: true,
  imports: [
    RouterLink, FormsModule, DatePipe,
    MatCardModule, MatTableModule, MatProgressSpinnerModule,
    MatIconModule, MatButtonModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatPaginatorModule
  ],
  templateUrl: './audit-log-screen.component.html',
  styleUrl: './audit-log-screen.component.scss'
})
export class AuditLogScreenComponent implements OnInit {
  @ViewChild(MatPaginator) paginator!: MatPaginator;

  logs = signal<AuditLogEntry[]>([]);
  totalElements = signal(0);
  loading = signal(true);
  accessDenied = signal(false);
  exporting = signal(false);

  dateFrom = signal('');
  dateTo = signal('');

  pageIndex = signal(0);
  pageSize = signal(20);

  readonly columns = ['createdAt', 'action', 'userEmail', 'caseFileTitle', 'documentName'];

  constructor(
    private auditLogService: AuditLogService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadLogs();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadLogs();
  }

  loadLogs(): void {
    this.loading.set(true);
    const from = this.dateFrom() ? new Date(this.dateFrom()).toISOString() : undefined;
    const to   = this.dateTo()   ? new Date(this.dateTo() + 'T23:59:59Z').toISOString() : undefined;
    this.auditLogService.getAuditLogs(from, to, this.pageIndex(), this.pageSize()).subscribe({
      next: page => {
        this.logs.set(page.content);
        this.totalElements.set(page.totalElements);
        this.loading.set(false);
      },
      error: (err: any) => {
        this.loading.set(false);
        if (err.status === 403) {
          this.accessDenied.set(true);
        } else if (err.status === 400) {
          this.snackBar.open('Dates invalides.', 'Fermer', { duration: 4000, panelClass: ['snack-error'] });
        } else {
          this.snackBar.open('Erreur lors du chargement du journal.', 'Fermer', {
            duration: 4000, panelClass: ['snack-error']
          });
        }
      }
    });
  }

  onDateChange(): void {
    this.pageIndex.set(0);
    if (this.paginator) this.paginator.firstPage();
    this.loadLogs();
  }

  exportCsv(): void {
    this.exporting.set(true);
    this.auditLogService.exportCsv().subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'audit-log.csv';
        a.click();
        URL.revokeObjectURL(url);
        this.exporting.set(false);
      },
      error: () => {
        this.snackBar.open("Erreur lors de l'export.", 'Fermer', { duration: 4000, panelClass: ['snack-error'] });
        this.exporting.set(false);
      }
    });
  }
}
