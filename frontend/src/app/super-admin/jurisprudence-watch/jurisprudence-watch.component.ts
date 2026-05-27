import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { Subscription, interval, switchMap } from 'rxjs';

import {
  ArbitrateDecision,
  JurisprudenceAuditLog,
  JurisprudenceBootstrapEntry,
  JurisprudenceBootstrapJobStatusResponse,
  JurisprudenceWatchAdminClientService,
  JurisprudenceWatchFlag,
} from './jurisprudence-watch-admin.service';

export interface BootstrapParseResult {
  entries: JurisprudenceBootstrapEntry[];
  errors: string[];
}

const BOOTSTRAP_MAX_ENTRIES = 200;
const BOOTSTRAP_MAX_FILE_SIZE_BYTES = 1_048_576; // 1 Mo — SF-JU-01-07
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
 * F-JU-01 / SF-JU-01-05 + SF-JU-01-06 + SF-JU-01-07 — dashboard admin
 * `/super-admin/jurisprudence-watch`.
 *
 * 3 onglets : Bootstrap (lancement initial via copier-coller ou upload .csv),
 * Flags PENDING (3 actions inline), Audit log (lecture seule).
 */
@Component({
  selector: 'app-jurisprudence-watch',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule, MatProgressBarModule, MatTabsModule, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './jurisprudence-watch.component.html',
  styleUrl: './jurisprudence-watch.component.scss',
})
export class JurisprudenceWatchComponent implements OnInit, OnDestroy {

  readonly bootstrapMaxEntries = BOOTSTRAP_MAX_ENTRIES;
  readonly bootstrapMaxFileSizeBytes = BOOTSTRAP_MAX_FILE_SIZE_BYTES;
  /** SF-JU-01-10 — intervalle de polling du job de bootstrap (ms). */
  readonly bootstrapPollIntervalMs = 5000;

  flags: JurisprudenceWatchFlag[] = [];
  auditLog: JurisprudenceAuditLog[] = [];
  loadingFlags = false;
  loadingAudit = false;
  arbitratingId: string | null = null;
  arbitrateComment = '';

  csvInput = '';
  parseResult: BootstrapParseResult = { entries: [], errors: [] };
  loadingBootstrap = false;
  loadingFile = false;
  /** SF-JU-01-10 — état courant du job async, mis à jour par polling. */
  bootstrapJob: JurisprudenceBootstrapJobStatusResponse | null = null;
  private pollSubscription: Subscription | null = null;

  @ViewChild('fileInput', { static: false })
  protected fileInput?: ElementRef<HTMLInputElement>;

  private readonly client = inject(JurisprudenceWatchAdminClientService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.loadFlags();
    this.loadAudit();
  }

  ngOnDestroy(): void {
    this.pollSubscription?.unsubscribe();
    this.pollSubscription = null;
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

  /** SF-JU-01-07 — déclenche l'ouverture du sélecteur fichier OS via le bouton stylé. */
  protected triggerFileSelector(): void {
    this.fileInput?.nativeElement.click();
  }

  /** SF-JU-01-07 — lit le fichier CSV sélectionné et alimente le textarea. */
  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length > 0 ? input.files[0] : null;
    if (!file) {
      return;
    }
    if (file.size === 0) {
      this.snackBar.open('Fichier vide', 'OK', { duration: 4000 });
      input.value = '';
      return;
    }
    if (file.size > BOOTSTRAP_MAX_FILE_SIZE_BYTES) {
      this.snackBar.open('Fichier trop volumineux (max 1 Mo)', 'OK', { duration: 4000 });
      input.value = '';
      return;
    }
    this.loadingFile = true;
    const reader = new FileReader();
    reader.onload = () => {
      const text = typeof reader.result === 'string' ? reader.result : '';
      this.onCsvInputChange(text);
      this.loadingFile = false;
      const msg = `Fichier "${file.name}" chargé (${this.parseResult.entries.length} entrées détectées)`;
      this.snackBar.open(msg, 'OK', { duration: 4000 });
      input.value = '';
      this.cdr.markForCheck();
    };
    reader.onerror = () => {
      this.loadingFile = false;
      this.snackBar.open('Erreur de lecture du fichier', 'OK', { duration: 4000 });
      input.value = '';
      this.cdr.markForCheck();
    };
    reader.readAsText(file, 'UTF-8');
  }

  /**
   * SF-JU-01-10 — démarre le bootstrap en mode async + lance le polling de
   * progression jusqu'au terminal state. Le backend retourne immédiatement un
   * jobId ; on récupère l'état via GET /bootstrap/jobs/{id} toutes les 5 s.
   */
  protected runBootstrap(): void {
    if (!this.canLaunchBootstrap()) {
      if (this.parseResult.errors.length > 0) {
        this.snackBar.open(this.parseResult.errors[0], 'OK', { duration: 5000 });
      }
      return;
    }
    this.pollSubscription?.unsubscribe();
    this.pollSubscription = null;
    this.bootstrapJob = null;
    this.loadingBootstrap = true;
    this.client.triggerBootstrap(this.parseResult.entries).subscribe({
      next: started => {
        this.bootstrapJob = {
          jobId: started.jobId,
          status: 'RUNNING',
          entriesTotal: started.entriesTotal,
          entriesProcessed: 0,
          mappingsCreated: 0,
          entriesSkipped: 0,
          durationMs: null,
          errorMessage: null,
          startedAt: started.startedAt,
          completedAt: null,
        };
        this.startBootstrapPolling(started.jobId);
        this.cdr.markForCheck();
      },
      error: err => {
        this.loadingBootstrap = false;
        const message = err?.error?.message || err?.message || 'erreur inconnue';
        this.snackBar.open(`Échec du lancement : ${message}`, 'OK', { duration: 5000 });
        this.cdr.markForCheck();
      },
    });
  }

  private startBootstrapPolling(jobId: string): void {
    this.pollSubscription = interval(this.bootstrapPollIntervalMs)
      .pipe(switchMap(() => this.client.getBootstrapJobStatus(jobId)))
      .subscribe({
        next: status => {
          this.bootstrapJob = status;
          if (status.status === 'DONE' || status.status === 'FAILED') {
            this.stopBootstrapPolling(status);
          }
          this.cdr.markForCheck();
        },
        error: err => {
          // Erreur transitoire pendant le polling → on logge et on continue.
          // L'admin verra le compteur figé si l'erreur persiste.
          const message = err?.error?.message || err?.message || 'erreur inconnue';
          this.snackBar.open(`Erreur de polling : ${message}`, 'OK', { duration: 3000 });
          this.cdr.markForCheck();
        },
      });
  }

  private stopBootstrapPolling(status: JurisprudenceBootstrapJobStatusResponse): void {
    this.pollSubscription?.unsubscribe();
    this.pollSubscription = null;
    this.loadingBootstrap = false;
    if (status.status === 'DONE') {
      const msg = `Bootstrap terminé : ${status.entriesProcessed} processed, `
                + `${status.mappingsCreated} created, `
                + `${status.entriesSkipped} skipped`
                + (status.durationMs !== null ? ` (${status.durationMs}ms)` : '');
      this.snackBar.open(msg, 'OK', { duration: 5000 });
      this.loadAudit();
    } else {
      const reason = status.errorMessage || 'erreur inconnue';
      this.snackBar.open(`Échec du bootstrap : ${reason}`, 'OK', { duration: 6000 });
    }
  }
}
