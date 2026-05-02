import { CommonModule, DatePipe } from '@angular/common';
import { Component, Inject, OnInit, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BacklogAdminService } from '../../../core/services/backlog-admin.service';
import { BacklogFeatureDetail } from '../../../core/models/backlog.model';
import { BacklogStatusBadgeComponent } from '../shared/backlog-status-badge.component';

export interface BacklogFeatureDetailDialogData {
  code: string;
}

const DOMAIN_LABELS: Record<string, string> = {
  DROIT_TRAVAIL: 'Droit du travail',
  IMMIGRATION: 'Immigration',
  FAMILLE: 'Famille',
  TRANSVERSAL: 'Transversal',
  MARKETING: 'Marketing',
  UNKNOWN: '—',
};
const PRIORITY_LABELS: Record<string, string> = {
  HIGH: 'Haute',
  MEDIUM: 'Moyenne',
  LOW: 'Basse',
  UNKNOWN: '—',
};

const SUBFEATURE_DESCRIPTION_MAX = 240;

export function expectedSubfeatureMdPath(parentCode: string | null | undefined, subCode: string | null | undefined): string | null {
  if (!parentCode || !subCode) return null;
  if (!/^F-[A-Z0-9-]+$/.test(parentCode)) return null;
  if (!subCode.startsWith('SF-')) return null;
  return `docs/features/${parentCode}/${subCode}-*.md`;
}

@Component({
  selector: 'app-backlog-feature-detail-dialog',
  standalone: true,
  imports: [
    CommonModule, DatePipe,
    MatDialogModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule,
    BacklogStatusBadgeComponent,
  ],
  templateUrl: './backlog-feature-detail-dialog.component.html',
  styleUrl: './backlog-feature-detail-dialog.component.scss',
})
export class BacklogFeatureDetailDialogComponent implements OnInit {
  detail = signal<BacklogFeatureDetail | null>(null);
  loading = signal(true);
  notFound = signal(false);

  readonly domainLabel = (d: string | null | undefined) => d ? (DOMAIN_LABELS[d] ?? d) : '—';
  readonly priorityLabel = (p: string | null | undefined) => p ? (PRIORITY_LABELS[p] ?? p) : '—';

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: BacklogFeatureDetailDialogData,
    private dialogRef: MatDialogRef<BacklogFeatureDetailDialogComponent>,
    private backlogService: BacklogAdminService,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit(): void {
    if (!this.data?.code) {
      this.dialogRef.close();
      return;
    }
    this.backlogService.getFeatureDetail(this.data.code).subscribe({
      next: detail => {
        this.detail.set(detail);
        this.loading.set(false);
      },
      error: err => {
        this.loading.set(false);
        if (err?.status === 404) {
          this.notFound.set(true);
          return;
        }
        this.snackBar.open('Erreur lors du chargement du détail', 'Fermer', {
          duration: 4000, panelClass: ['snack-error'],
        });
        this.dialogRef.close();
      },
    });
  }

  close(): void {
    this.dialogRef.close();
  }

  truncate(text: string | null | undefined): string {
    if (!text) return '';
    if (text.length <= SUBFEATURE_DESCRIPTION_MAX) return text;
    return text.slice(0, SUBFEATURE_DESCRIPTION_MAX).trimEnd() + '…';
  }

  expectedPath(parentCode: string | null | undefined, subCode: string | null | undefined): string | null {
    return expectedSubfeatureMdPath(parentCode, subCode);
  }
}
