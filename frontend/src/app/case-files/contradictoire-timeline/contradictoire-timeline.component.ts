import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ContradictoireService } from '../../core/services/contradictoire.service';
import { DocumentService } from '../../core/services/document.service';
import { ConclusionsService } from '../../core/services/conclusions.service';
import { Document } from '../../core/models/document.model';
import { ConclusionVersionSummary } from '../../core/models/conclusion.model';
import {
  ContradictoireParty,
  ContradictoireRound,
  ContradictoireTimeline,
} from '../../core/models/contradictoire.model';

/**
 * F-282 / SF-282-01 — frise du cycle contradictoire (onglet Suivi).
 * Élève le calendrier procédural en vue d'échanges : qui a déposé quoi, à qui le
 * tour, sous quel délai. Au round « à vous », invite à générer la réplique
 * (émet `generateReplyRequested` → l'onglet Décision).
 */
@Component({
  selector: 'app-contradictoire-timeline',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSidenavModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './contradictoire-timeline.component.html',
  styleUrl: './contradictoire-timeline.component.scss',
})
export class ContradictoireTimelineComponent implements OnInit {
  @Input({ required: true }) caseFileId!: string;
  /**
   * Au clic « Générer ma réplique » : conservé pour rétro-compatibilité (le
   * parent peut encore réagir, p. ex. bascule d'onglet). SF-282-03 : le bouton
   * **navigue** désormais directement vers la page Conclusions en portant le
   * `roundId` du round « à vous » (auto-rattachement Part B).
   */
  @Output() generateReplyRequested = new EventEmitter<void>();

  private readonly service = inject(ContradictoireService);
  private readonly documentService = inject(DocumentService);
  private readonly conclusionsService = inject(ConclusionsService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly timeline = signal<ContradictoireTimeline | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly showForm = signal(false);
  readonly editingId = signal<string | null>(null);

  /** SF-282-03 : documents du dossier, source du sélecteur « Document lié » (ADVERSE). */
  readonly documents = signal<Document[]>([]);
  /** SF-282-03 : true si le chargement des documents a échoué (dégradation gracieuse). */
  readonly documentsUnavailable = signal(false);

  /** SF-282-03 : versions de conclusions du dossier, source du sélecteur « Version liée » (OURS). */
  readonly versions = signal<ConclusionVersionSummary[]>([]);
  /** SF-282-03 : true si le chargement des versions a échoué (dégradation gracieuse). */
  readonly versionsUnavailable = signal(false);

  // ── SF-282-03 — drawer d'aperçu d'une pièce adverse (pattern F-289 V1.1) ──
  /** Round dont la pièce est prévisualisée dans le drawer (null = fermé). */
  readonly previewRound = signal<ContradictoireRound | null>(null);
  /** Vrai pendant la vérification de disponibilité de la pièce. */
  readonly previewLoading = signal(false);
  /** Vrai si la pièce est introuvable / supprimée. */
  readonly previewError = signal(false);
  /** URL sûre de rendu inline de la pièce prévisualisée. */
  readonly previewUrl = signal<SafeResourceUrl | null>(null);

  readonly rounds = computed<ContradictoireRound[]>(() => this.timeline()?.rounds ?? []);
  readonly summary = computed(() => this.timeline()?.summary ?? null);
  readonly awaitingOurs = computed(() => this.summary()?.awaitingParty === 'OURS');

  readonly form = this.fb.group({
    party: this.fb.nonNullable.control<ContradictoireParty>('ADVERSE', Validators.required),
    label: this.fb.control<string>('', Validators.maxLength(200)),
    datedAt: this.fb.nonNullable.control<string>('', Validators.required),
    responseDueAt: this.fb.control<string>(''),
    sourceDocumentId: this.fb.control<string | null>(null),
    sourceConclusionId: this.fb.control<string | null>(null),
  });

  /** SF-282-03 : true si la partie sélectionnée dans le formulaire est « Nous ». */
  readonly formIsOurs = computed(() => this.form.controls.party.value === 'OURS');

  ngOnInit(): void {
    this.load();
    this.loadDocuments();
    this.loadVersions();
    // SF-282-03 — à chaque bascule de partie, on réinitialise l'id de l'AUTRE
    // type pour ne jamais envoyer les deux sources (le backend rejetterait).
    this.form.controls.party.valueChanges.subscribe((party) => {
      if (party === 'OURS') {
        this.form.controls.sourceDocumentId.setValue(null, { emitEvent: false });
      } else {
        this.form.controls.sourceConclusionId.setValue(null, { emitEvent: false });
      }
      this.cdr.markForCheck();
    });
  }

  /** SF-282-03 : charge les versions de conclusions pour le sélecteur (partie OURS). */
  private loadVersions(): void {
    this.conclusionsService.listVersions(this.caseFileId).subscribe({
      next: (versions) => {
        this.versions.set(versions ?? []);
        this.versionsUnavailable.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.versions.set([]);
        this.versionsUnavailable.set(true);
        this.cdr.markForCheck();
      },
    });
  }

  /** SF-282-03 : libellé d'option d'une version (« v3 · Brouillon »). */
  versionOptionLabel(v: ConclusionVersionSummary): string {
    return `v${v.versionNumber} · ${this.lifecycleLabel(v.lifecycleStatus)}`;
  }

  /** SF-282-03 : libellé FR d'un cycle de vie de version. */
  private lifecycleLabel(status: string): string {
    switch (status) {
      case 'DRAFT':
        return 'Brouillon';
      case 'VALIDATED':
        return 'Validé';
      case 'DEPOSITED':
        return 'Déposé';
      default:
        return status;
    }
  }

  /** SF-282-03 : charge les documents du dossier pour le sélecteur de pièce source. */
  private loadDocuments(): void {
    this.documentService.list(this.caseFileId).subscribe({
      next: (docs) => {
        this.documents.set(docs ?? []);
        this.documentsUnavailable.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.documents.set([]);
        this.documentsUnavailable.set(true);
        this.cdr.markForCheck();
      },
    });
  }

  /** SF-282-03 : nom de fichier de la pièce liée à un round (null si supprimée/inconnue). */
  sourceDocumentName(documentId: string | null): string | null {
    if (!documentId) return null;
    return this.documents().find((d) => d.id === documentId)?.originalFilename ?? null;
  }

  /** SF-282-03 : URL de téléchargement de la pièce liée (réutilise DocumentService). */
  sourceDocumentUrl(documentId: string): string {
    return this.documentService.downloadUrl(this.caseFileId, documentId);
  }

  private load(): void {
    this.loading.set(true);
    this.service.timeline(this.caseFileId).subscribe({
      next: (t) => {
        this.timeline.set(t);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  openAdd(party: ContradictoireParty): void {
    this.editingId.set(null);
    this.form.reset({
      party,
      label: '',
      datedAt: '',
      responseDueAt: '',
      sourceDocumentId: null,
      sourceConclusionId: null,
    });
    this.loadDocuments(); // rafraîchit la liste (des pièces ont pu être uploadées depuis le montage de l'onglet)
    this.loadVersions(); // rafraîchit l'historique des versions (des conclusions ont pu être générées)
    this.showForm.set(true);
  }

  openEdit(round: ContradictoireRound): void {
    this.editingId.set(round.id);
    this.form.reset({
      party: round.party,
      label: round.label ?? '',
      datedAt: round.datedAt,
      responseDueAt: round.responseDueAt ?? '',
      sourceDocumentId: round.sourceDocumentId ?? null,
      sourceConclusionId: round.sourceConclusionId ?? null,
    });
    this.loadDocuments(); // rafraîchit la liste (des pièces ont pu être uploadées depuis le montage de l'onglet)
    this.loadVersions(); // rafraîchit l'historique des versions
    this.showForm.set(true);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.editingId.set(null);
  }

  save(): void {
    if (this.form.invalid || this.saving()) return;
    const raw = this.form.getRawValue();
    if (raw.responseDueAt && raw.responseDueAt < raw.datedAt) {
      this.snackBar.open('L’échéance ne peut pas précéder la date de l’échange.', 'Fermer', {
        duration: 5000,
        panelClass: ['snack-error'],
      });
      return;
    }
    // SF-282-03 — une seule source, du bon type : OURS → conclusion uniquement ;
    // ADVERSE → document uniquement. On force l'autre id à null par sécurité
    // (la bascule de partie l'a déjà réinitialisé) pour ne jamais envoyer les deux.
    const isOurs = raw.party === 'OURS';
    const input = {
      party: raw.party,
      label: raw.label?.trim() || null,
      datedAt: raw.datedAt,
      responseDueAt: raw.responseDueAt || null,
      sourceDocumentId: isOurs ? null : raw.sourceDocumentId || null,
      sourceConclusionId: isOurs ? raw.sourceConclusionId || null : null,
    };
    this.saving.set(true);
    const id = this.editingId();
    const op$ = id
      ? this.service.update(this.caseFileId, id, input)
      : this.service.create(this.caseFileId, input);
    op$.subscribe({
      next: () => {
        this.saving.set(false);
        this.showForm.set(false);
        this.editingId.set(null);
        this.load();
      },
      error: () => {
        this.saving.set(false);
        this.snackBar.open('Échec de l’enregistrement de l’échange.', 'Fermer', {
          duration: 5000,
          panelClass: ['snack-error'],
        });
        this.cdr.markForCheck();
      },
    });
  }

  remove(round: ContradictoireRound): void {
    this.service.delete(this.caseFileId, round.id).subscribe({
      next: () => this.load(),
      error: () => {
        this.snackBar.open('Échec de la suppression.', 'Fermer', {
          duration: 5000,
          panelClass: ['snack-error'],
        });
      },
    });
  }

  onGenerateReply(): void {
    this.generateReplyRequested.emit();
  }

  partyLabel(party: ContradictoireParty): string {
    return party === 'OURS' ? 'Nous' : 'Partie adverse';
  }
}
