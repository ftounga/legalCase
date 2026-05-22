import { ChangeDetectionStrategy, ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';

import {
  ArbitrateDecision,
  JurisprudenceAuditLog,
  JurisprudenceWatchAdminClientService,
  JurisprudenceWatchFlag,
} from './jurisprudence-watch-admin.service';

/**
 * F-JU-01 / SF-JU-01-05 — dashboard admin `/super-admin/jurisprudence-watch`.
 *
 * 2 onglets : Flags PENDING (avec 3 actions inline) + Audit log (lecture seule).
 */
@Component({
  selector: 'app-jurisprudence-watch',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule, MatTabsModule, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './jurisprudence-watch.component.html',
  styleUrl: './jurisprudence-watch.component.scss',
})
export class JurisprudenceWatchComponent implements OnInit {

  flags: JurisprudenceWatchFlag[] = [];
  auditLog: JurisprudenceAuditLog[] = [];
  loadingFlags = false;
  loadingAudit = false;
  arbitratingId: string | null = null;
  arbitrateComment = '';

  private readonly client = inject(JurisprudenceWatchAdminClientService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.loadFlags();
    this.loadAudit();
  }

  protected loadFlags(): void {
    this.loadingFlags = true;
    this.client.listFlags('PENDING', 0, 50).subscribe({
      next: page => {
        this.flags = page.content;
        this.loadingFlags = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loadingFlags = false;
        this.snackBar.open('Échec du chargement des flags', 'OK', { duration: 3000 });
        this.cdr.markForCheck();
      },
    });
  }

  protected loadAudit(): void {
    this.loadingAudit = true;
    this.client.listAuditLog(0, 50).subscribe({
      next: page => {
        this.auditLog = page.content;
        this.loadingAudit = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loadingAudit = false;
        this.cdr.markForCheck();
      },
    });
  }

  protected openArbitrate(flagId: string): void {
    this.arbitratingId = flagId;
    this.arbitrateComment = '';
  }

  protected cancelArbitrate(): void {
    this.arbitratingId = null;
    this.arbitrateComment = '';
  }

  protected arbitrate(flagId: string, decision: ArbitrateDecision): void {
    const comment = this.arbitrateComment.trim() || undefined;
    this.client.arbitrate(flagId, decision, comment).subscribe({
      next: () => {
        this.flags = this.flags.filter(f => f.id !== flagId);
        this.arbitratingId = null;
        this.arbitrateComment = '';
        this.snackBar.open('Décision appliquée.', 'OK', { duration: 3000 });
        this.loadAudit();
        this.cdr.markForCheck();
      },
      error: () => {
        this.snackBar.open('Échec de l\'arbitrage.', 'OK', { duration: 3000 });
        this.cdr.markForCheck();
      },
    });
  }
}
