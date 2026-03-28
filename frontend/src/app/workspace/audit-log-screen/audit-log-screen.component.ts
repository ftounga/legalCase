import { Component, OnInit, signal, computed } from '@angular/core';
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
    MatFormFieldModule, MatInputModule, MatSelectModule
  ],
  templateUrl: './audit-log-screen.component.html',
  styleUrl: './audit-log-screen.component.scss'
})
export class AuditLogScreenComponent implements OnInit {
  allLogs = signal<AuditLogEntry[]>([]);
  loading = signal(true);
  accessDenied = signal(false);
  exporting = signal(false);

  searchText = signal('');
  actionFilter = signal('ALL');

  readonly columns = ['createdAt', 'action', 'userEmail', 'caseFileTitle', 'documentName'];

  readonly filteredLogs = computed(() => {
    const text = this.searchText().toLowerCase().trim();
    const action = this.actionFilter();
    return this.allLogs().filter(log => {
      const matchesAction = action === 'ALL' || log.action === action;
      const matchesText = !text ||
        log.userEmail.toLowerCase().includes(text) ||
        log.caseFileTitle.toLowerCase().includes(text) ||
        log.documentName.toLowerCase().includes(text);
      return matchesAction && matchesText;
    });
  });

  constructor(
    private auditLogService: AuditLogService,
    private snackBar: MatSnackBar
  ) {}

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

  ngOnInit(): void {
    this.auditLogService.getAuditLogs().subscribe({
      next: logs => {
        this.allLogs.set(logs);
        this.loading.set(false);
      },
      error: (err: any) => {
        this.loading.set(false);
        if (err.status === 403) {
          this.accessDenied.set(true);
        } else {
          this.snackBar.open('Erreur lors du chargement du journal.', 'Fermer', {
            duration: 4000, panelClass: ['snack-error']
          });
        }
      }
    });
  }
}
