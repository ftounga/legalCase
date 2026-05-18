import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { WorkspaceService } from '../../core/services/workspace.service';
import { StyleCorpusService } from '../../core/services/style-corpus.service';
import {
  StyleCorpusDocumentStatus,
  StyleCorpusDocumentSummary,
} from '../../core/models/style-corpus.model';
import { ConfirmDeleteCorpusDialogComponent } from './confirm-delete-corpus-dialog.component';

/** Intervalle de polling des documents en cours de traitement (ms). */
const POLL_INTERVAL_MS = 3000;

/** Libellés FR des statuts de traitement d'un document de corpus. */
const STATUS_LABELS: Record<StyleCorpusDocumentStatus, string> = {
  PENDING: 'En attente',
  PROCESSING: 'Analyse en cours',
  DONE: 'Style appris',
  FAILED: 'Échec',
};

/**
 * F-98 / SF-98-48 — Écran cabinet de gestion du corpus de style.
 *
 * Permet à l'avocat de constituer son corpus de style : téléverser des
 * conclusions de référence, suivre leur traitement, les activer/désactiver
 * et les retirer. Le style est appris ; le contenu client n'est pas conservé
 * (minimisation RGPD — cf. SF-98-46).
 *
 * Ce n'est PAS un outil décisionnel : pas de `TOOL_REGISTRY`, pas de panel
 * F-IA-04. C'est un écran de gestion de niveau workspace.
 *
 * Standalone, OnPush + signals. L'état étant muté dans des `subscribe()`, un
 * `ChangeDetectorRef.markForCheck()` est appelé dans chaque `next` ET `error`
 * (cf. mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-style-corpus',
  standalone: true,
  imports: [
    DatePipe,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSlideToggleModule,
  ],
  templateUrl: './style-corpus.component.html',
  styleUrl: './style-corpus.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StyleCorpusComponent implements OnInit, OnDestroy {
  /** Documents du corpus de style du workspace courant. */
  readonly documents = signal<StyleCorpusDocumentSummary[]>([]);
  /** Chargement initial (résolution workspace + GET liste). */
  readonly loading = signal(true);
  /** Vrai si le chargement initial a échoué — écran indisponible. */
  readonly unavailable = signal(false);
  /** Upload d'un document en cours (POST). */
  readonly uploading = signal(false);
  /** Ids des documents dont le toggle actif/inactif est en cours (PATCH). */
  readonly togglingIds = signal<ReadonlySet<string>>(new Set());
  /** Ids des documents dont la suppression est en cours (DELETE). */
  readonly deletingIds = signal<ReadonlySet<string>>(new Set());

  /** Libellés des statuts, exposés au template. */
  readonly statusLabels = STATUS_LABELS;

  /** Vrai quand le corpus est vide (après chargement réussi). */
  readonly isEmpty = computed<boolean>(
    () => !this.loading() && !this.unavailable() && this.documents().length === 0,
  );

  private readonly workspaceService = inject(WorkspaceService);
  private readonly styleCorpusService = inject(StyleCorpusService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly cdr = inject(ChangeDetectorRef);

  /** Workspace courant — résolu au montage (pattern écrans `workspace/*`). */
  private workspaceId: string | null = null;
  /** Handle du polling actif (null = aucun polling en cours). */
  private pollHandle: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.workspaceService.getCurrentWorkspace().subscribe({
      next: (ws) => {
        this.workspaceId = ws.id;
        this.loadDocuments(true);
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading.set(false);
        this.unavailable.set(true);
        this.snackBar.open(
          'Corpus de style indisponible — impossible de charger votre cabinet.',
          'Fermer',
          { duration: 4000, panelClass: ['snack-error'] },
        );
        this.cdr.markForCheck();
      },
    });
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  /**
   * Déclenche le téléversement d'une conclusion de référence.
   * Sur succès, recharge la liste et relance le polling si nécessaire.
   */
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    // Réinitialise l'input pour permettre de re-téléverser le même fichier.
    input.value = '';
    if (!file || !this.workspaceId || this.uploading()) {
      return;
    }
    this.uploading.set(true);
    this.styleCorpusService.upload(this.workspaceId, file).subscribe({
      next: () => {
        this.uploading.set(false);
        this.snackBar.open(
          'Document ajouté — analyse du style en cours.',
          'Fermer',
          { duration: 3000, panelClass: ['snack-success'] },
        );
        this.loadDocuments(false);
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.uploading.set(false);
        const msg =
          err?.error?.message ||
          'Impossible de téléverser ce document. Formats acceptés : PDF, Word, texte (max 50 Mo).';
        this.snackBar.open(msg, 'Fermer', {
          duration: 6000,
          panelClass: ['snack-error'],
        });
        this.cdr.markForCheck();
      },
    });
  }

  /** Active ou désactive un document du corpus. */
  toggleActive(doc: StyleCorpusDocumentSummary): void {
    if (!this.workspaceId || this.togglingIds().has(doc.id)) {
      return;
    }
    const nextActive = !doc.active;
    this.markToggling(doc.id, true);
    this.styleCorpusService
      .setActive(this.workspaceId, doc.id, nextActive)
      .subscribe({
        next: (updated) => {
          this.markToggling(doc.id, false);
          this.replaceDocument(updated);
          this.cdr.markForCheck();
        },
        error: () => {
          this.markToggling(doc.id, false);
          this.snackBar.open(
            'Impossible de modifier ce document du corpus.',
            'Fermer',
            { duration: 4000, panelClass: ['snack-error'] },
          );
          this.cdr.markForCheck();
        },
      });
  }

  /**
   * Demande confirmation puis supprime un document du corpus.
   * Confirmation via `MatDialog` (action destructive).
   */
  confirmDelete(doc: StyleCorpusDocumentSummary): void {
    if (!this.workspaceId || this.deletingIds().has(doc.id)) {
      return;
    }
    this.dialog
      .open(ConfirmDeleteCorpusDialogComponent, {
        data: { filename: doc.originalFilename },
        width: '420px',
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.deleteDocument(doc.id);
        }
      });
  }

  /** Vrai si le toggle d'un document est en cours de traitement. */
  isToggling(id: string): boolean {
    return this.togglingIds().has(id);
  }

  /** Vrai si la suppression d'un document est en cours de traitement. */
  isDeleting(id: string): boolean {
    return this.deletingIds().has(id);
  }

  /** Libellé FR d'un statut de traitement. */
  statusLabel(status: StyleCorpusDocumentStatus): string {
    return STATUS_LABELS[status];
  }

  private deleteDocument(id: string): void {
    if (!this.workspaceId) {
      return;
    }
    this.markDeleting(id, true);
    this.styleCorpusService.remove(this.workspaceId, id).subscribe({
      next: () => {
        this.markDeleting(id, false);
        this.documents.update((docs) => docs.filter((d) => d.id !== id));
        this.snackBar.open('Document retiré du corpus.', 'Fermer', {
          duration: 3000,
          panelClass: ['snack-success'],
        });
        this.cdr.markForCheck();
      },
      error: () => {
        this.markDeleting(id, false);
        this.snackBar.open(
          'Impossible de retirer ce document du corpus.',
          'Fermer',
          { duration: 4000, panelClass: ['snack-error'] },
        );
        this.cdr.markForCheck();
      },
    });
  }

  /**
   * Recharge la liste des documents.
   * `initial` distingue le chargement de montage (qui éteint `loading`) des
   * recharges silencieuses (upload, polling) qui n'affichent pas le spinner.
   */
  private loadDocuments(initial: boolean): void {
    if (!this.workspaceId) {
      return;
    }
    this.styleCorpusService.list(this.workspaceId).subscribe({
      next: (docs) => {
        this.documents.set(docs);
        if (initial) {
          this.loading.set(false);
        }
        this.syncPolling();
        this.cdr.markForCheck();
      },
      error: () => {
        if (initial) {
          this.loading.set(false);
          this.unavailable.set(true);
        }
        this.snackBar.open(
          'Impossible de charger le corpus de style.',
          'Fermer',
          { duration: 4000, panelClass: ['snack-error'] },
        );
        this.cdr.markForCheck();
      },
    });
  }

  /** Démarre ou arrête le polling selon la présence de documents transitoires. */
  private syncPolling(): void {
    const hasPending = this.documents().some(
      (d) => d.status === 'PENDING' || d.status === 'PROCESSING',
    );
    if (hasPending) {
      this.startPolling();
    } else {
      this.stopPolling();
    }
  }

  /** Démarre le polling de la liste (idempotent). */
  private startPolling(): void {
    if (this.pollHandle !== null) {
      return;
    }
    this.pollHandle = setInterval(
      () => this.loadDocuments(false),
      POLL_INTERVAL_MS,
    );
  }

  /** Arrête le polling s'il est actif. */
  private stopPolling(): void {
    if (this.pollHandle !== null) {
      clearInterval(this.pollHandle);
      this.pollHandle = null;
    }
  }

  /** Remplace un document de la liste par sa version mise à jour. */
  private replaceDocument(updated: StyleCorpusDocumentSummary): void {
    this.documents.update((docs) =>
      docs.map((d) => (d.id === updated.id ? updated : d)),
    );
  }

  private markToggling(id: string, on: boolean): void {
    this.togglingIds.update((set) => this.withFlag(set, id, on));
  }

  private markDeleting(id: string, on: boolean): void {
    this.deletingIds.update((set) => this.withFlag(set, id, on));
  }

  private withFlag(
    set: ReadonlySet<string>,
    id: string,
    on: boolean,
  ): ReadonlySet<string> {
    const next = new Set(set);
    if (on) {
      next.add(id);
    } else {
      next.delete(id);
    }
    return next;
  }
}
