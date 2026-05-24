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
  JurisprudenceBootstrapEntry,
  JurisprudenceBootstrapResponse,
  JurisprudenceWatchAdminClientService,
  JurisprudenceWatchFlag,
} from './jurisprudence-watch-admin.service';

export interface BootstrapParseResult {
  entries: JurisprudenceBootstrapEntry[];
  errors: string[];
}

const BOOTSTRAP_MAX_ENTRIES = 200;
const TOOL_OR_BRANCHE_REGEX = /^[a-zA-Z0-9_-]{1,100}$/;
const BOOTSTRAP_EXAMPLE_CSV = [
  'f-dt-07,anciennete-licenciement,ancienneté préavis indemnité licenciement',
  'f-im-05,titre-sejour-vie-privee-familiale,L. 423-23 CESEDA vie privée familiale',
  'f-fa-05,partage-immobilier-divorce,partage indivision divorce immobilier'
].join('\n');

export function parseBootstrapCsv(input: string): BootstrapParseResult {
  const entries: JurisprudenceBootstrapEntry[] = [];
  const errors: string[] = [];
  const lines = input.split(/\r?\n/);
  lines.forEach((rawLine, index) => {
    const line = rawLine.trim();
    if (line.length === 0) {
      return;
    }
    const cols = line.split(',').map(c => c.trim());
    const lineNo = index + 1;
    if (cols.length < 3) {
      errors.push(`Ligne ${lineNo} invalide : ${cols.length} colonne(s), 3 minimum requises`);
      return;
    }
    const [toolId, brancheCalculId, motCleRecherche, juridictionFiltre, dateMin] = cols;
    if (!TOOL_OR_BRANCHE_REGEX.test(toolId)) {
      errors.push(`Ligne ${lineNo} invalide : toolId "${toolId}" ne respecte pas le format`);
      return;
    }
    if (!TOOL_OR_BRANCHE_REGEX.test(brancheCalculId)) {
      errors.push(`Ligne ${lineNo} invalide : brancheCalculId "${brancheCalculId}" ne respecte pas le format`);
      return;
    }
    if (motCleRecherche.length === 0 || motCleRecherche.length > 500) {
      errors.push(`Ligne ${lineNo} invalide : motCleRecherche vide ou > 500 caractères`);
      return;
    }
    const entry: JurisprudenceBootstrapEntry = { toolId, brancheCalculId, motCleRecherche };
    if (juridictionFiltre && juridictionFiltre.length > 0) {
      if (juridictionFiltre.length > 50) {
        errors.push(`Ligne ${lineNo} invalide : juridictionFiltre > 50 caractères`);
        return;
      }
      entry.juridictionFiltre = juridictionFiltre;
    }
    if (dateMin && dateMin.length > 0) {
      if (!/^\d{4}-\d{2}-\d{2}$/.test(dateMin)) {
        errors.push(`Ligne ${lineNo} invalide : dateMin "${dateMin}" doit être au format YYYY-MM-DD`);
        return;
      }
      entry.dateMin = dateMin;
    }
    entries.push(entry);
  });
  return { entries, errors };
}

/**
 * F-JU-01 / SF-JU-01-05 + SF-JU-01-06 — dashboard admin `/super-admin/jurisprudence-watch`.
 *
 * 3 onglets : Bootstrap (lancement initial), Flags PENDING (3 actions inline),
 * Audit log (lecture seule).
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

  readonly bootstrapMaxEntries = BOOTSTRAP_MAX_ENTRIES;

  flags: JurisprudenceWatchFlag[] = [];
  auditLog: JurisprudenceAuditLog[] = [];
  loadingFlags = false;
  loadingAudit = false;
  arbitratingId: string | null = null;
  arbitrateComment = '';

  csvInput = '';
  parseResult: BootstrapParseResult = { entries: [], errors: [] };
  loadingBootstrap = false;
  lastBootstrapResult: JurisprudenceBootstrapResponse | null = null;

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

  protected onCsvInputChange(value: string): void {
    this.csvInput = value;
    this.parseResult = parseBootstrapCsv(value);
  }

  protected loadExample(): void {
    this.csvInput = BOOTSTRAP_EXAMPLE_CSV;
    this.parseResult = parseBootstrapCsv(this.csvInput);
  }

  protected canLaunchBootstrap(): boolean {
    return !this.loadingBootstrap
        && this.parseResult.entries.length > 0
        && this.parseResult.entries.length <= BOOTSTRAP_MAX_ENTRIES
        && this.parseResult.errors.length === 0;
  }

  protected runBootstrap(): void {
    if (!this.canLaunchBootstrap()) {
      if (this.parseResult.errors.length > 0) {
        this.snackBar.open(this.parseResult.errors[0], 'OK', { duration: 5000 });
      }
      return;
    }
    this.loadingBootstrap = true;
    this.lastBootstrapResult = null;
    this.client.triggerBootstrap(this.parseResult.entries).subscribe({
      next: response => {
        this.lastBootstrapResult = response;
        this.loadingBootstrap = false;
        const msg = `Bootstrap terminé : ${response.entriesProcessed} processed, `
                  + `${response.mappingsCreated} created, `
                  + `${response.entriesSkipped} skipped (${response.durationMs}ms)`;
        this.snackBar.open(msg, 'OK', { duration: 5000 });
        this.loadAudit();
        this.cdr.markForCheck();
      },
      error: err => {
        this.loadingBootstrap = false;
        const message = err?.error?.message || err?.message || 'erreur inconnue';
        this.snackBar.open(`Échec du bootstrap : ${message}`, 'OK', { duration: 5000 });
        this.cdr.markForCheck();
      },
    });
  }
}
