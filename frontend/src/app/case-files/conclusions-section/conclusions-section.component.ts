import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ConclusionsService } from '../../core/services/conclusions.service';
import {
  ConclusionResponse,
  ConclusionStatus,
} from '../../core/models/conclusion.model';

/**
 * Intervalle de polling de l'état des conclusions (ms).
 * Mini-spec SF-98-01 : « polling GET …/conclusions toutes les 3 s ».
 */
const POLL_INTERVAL_MS = 3000;

/**
 * F-98 / SF-98-01 — Section « Conclusions » du détail dossier.
 *
 * Placée dans l'onglet Décision, juste après le tableau de bord décisionnel.
 * Permet à l'avocat de générer un projet de conclusions juridiques à partir
 * de la synthèse, du stade procédural, des outils décisionnels et des pistes
 * stratégiques du dossier.
 *
 * Ce n'est PAS un outil décisionnel : pas de `TOOL_REGISTRY`, pas de panel
 * F-IA-04, pas de gate F-IA-03. C'est un générateur de document.
 *
 * Standalone, OnPush + signals. L'état étant muté dans des `subscribe()`, un
 * `ChangeDetectorRef.markForCheck()` est appelé dans chaque `next` ET `error`
 * (cf. mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-conclusions-section',
  standalone: true,
  imports: [
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './conclusions-section.component.html',
  styleUrl: './conclusions-section.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConclusionsSectionComponent implements OnInit, OnDestroy {
  /** Dossier dont on génère les conclusions (obligatoire). */
  @Input() caseFileId!: string;

  /**
   * Vrai si le stade procédural du dossier est entièrement renseigné.
   * Optionnel : si `undefined`, on laisse le backend (`409 STAGE_NOT_SET`)
   * piloter le message guidant.
   */
  @Input() procedureStageComplete?: boolean;

  /**
   * Vrai si le dossier a au moins une analyse terminée.
   * Optionnel : si `undefined`, on laisse le backend
   * (`409 ANALYSIS_NOT_READY`) piloter le message guidant.
   */
  @Input() hasCompletedAnalysis?: boolean;

  /** État courant des conclusions du dossier. */
  readonly conclusion = signal<ConclusionResponse | null>(null);
  /** Chargement initial (GET au montage). */
  readonly loading = signal(true);
  /** Vrai si le GET initial a échoué — section indisponible. */
  readonly unavailable = signal(false);
  /** Déclenchement de génération en cours (POST). */
  readonly generating = signal(false);
  /** Copie du texte dans le presse-papier en cours. */
  readonly copying = signal(false);

  private readonly conclusionsService = inject(ConclusionsService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly cdr = inject(ChangeDetectorRef);

  /** Handle du polling actif (null = aucun polling en cours). */
  private pollHandle: ReturnType<typeof setInterval> | null = null;

  /** Statut courant, par défaut `NOT_GENERATED` tant que rien n'est chargé. */
  readonly status = computed<ConclusionStatus>(
    () => this.conclusion()?.status ?? 'NOT_GENERATED',
  );

  /** Vrai si tous les pré-requis fonctionnels connus sont satisfaits. */
  readonly prerequisitesMet = computed<boolean>(
    () =>
      this.procedureStageComplete !== false &&
      this.hasCompletedAnalysis !== false,
  );

  /**
   * Message guidant à afficher quand un pré-requis manque.
   * Le stade procédural est cité en premier (onglet Dossier).
   */
  readonly missingPrerequisiteMessage = computed<string | null>(() => {
    if (this.procedureStageComplete === false) {
      return 'Renseignez le stade procédural du dossier (onglet Dossier) avant de générer les conclusions.';
    }
    if (this.hasCompletedAnalysis === false) {
      return 'Lancez et terminez l\'analyse du dossier avant de générer les conclusions.';
    }
    return null;
  });

  ngOnInit(): void {
    this.conclusionsService.getConclusion(this.caseFileId).subscribe({
      next: (res) => {
        this.conclusion.set(res);
        this.loading.set(false);
        if (res.status === 'PENDING' || res.status === 'PROCESSING') {
          this.startPolling();
        }
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading.set(false);
        this.unavailable.set(true);
        this.snackBar.open(
          'Conclusions indisponibles — impossible de charger l\'état du dossier.',
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
   * Déclenche (ou relance) la génération du projet de conclusions.
   * Sur succès `202`, démarre le polling de l'état.
   */
  generate(): void {
    if (this.generating()) {
      return;
    }
    this.generating.set(true);
    this.conclusionsService.generate(this.caseFileId).subscribe({
      next: () => {
        this.generating.set(false);
        // Reflète immédiatement l'état PENDING en attendant le 1er poll.
        const current = this.conclusion();
        this.conclusion.set({
          id: current?.id ?? null,
          caseFileId: this.caseFileId,
          status: 'PENDING',
          content: null,
          jurisdictionLabel: current?.jurisdictionLabel ?? null,
          stageLabel: current?.stageLabel ?? null,
          positionLabel: current?.positionLabel ?? null,
          modelUsed: null,
          generatedAt: null,
          errorMessage: null,
          createdAt: current?.createdAt ?? null,
          updatedAt: current?.updatedAt ?? null,
        });
        this.startPolling();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.generating.set(false);
        const msg =
          err?.error?.message ||
          'Impossible de lancer la génération des conclusions.';
        this.snackBar.open(msg, 'Fermer', {
          duration: 6000,
          panelClass: ['snack-error'],
        });
        this.cdr.markForCheck();
      },
    });
  }

  /** Copie le texte des conclusions dans le presse-papier. */
  copy(): void {
    const content = this.conclusion()?.content;
    if (!content || this.copying()) {
      return;
    }
    this.copying.set(true);
    navigator.clipboard.writeText(content).then(
      () => {
        this.copying.set(false);
        this.snackBar.open(
          'Projet de conclusions copié dans le presse-papier.',
          'Fermer',
          { duration: 3000, panelClass: ['snack-success'] },
        );
        this.cdr.markForCheck();
      },
      () => {
        this.copying.set(false);
        this.snackBar.open(
          'Impossible de copier le texte dans le presse-papier.',
          'Fermer',
          { duration: 4000, panelClass: ['snack-error'] },
        );
        this.cdr.markForCheck();
      },
    );
  }

  /** Démarre le polling de l'état (idempotent). */
  private startPolling(): void {
    if (this.pollHandle !== null) {
      return;
    }
    this.pollHandle = setInterval(() => this.pollOnce(), POLL_INTERVAL_MS);
  }

  /** Arrête le polling s'il est actif. */
  private stopPolling(): void {
    if (this.pollHandle !== null) {
      clearInterval(this.pollHandle);
      this.pollHandle = null;
    }
  }

  /** Un tour de polling : recharge l'état, s'arrête sur DONE / FAILED. */
  private pollOnce(): void {
    this.conclusionsService.getConclusion(this.caseFileId).subscribe({
      next: (res) => {
        this.conclusion.set(res);
        if (res.status === 'DONE' || res.status === 'FAILED') {
          this.stopPolling();
        }
        this.cdr.markForCheck();
      },
      error: () => {
        // On arrête le polling sur erreur réseau pour ne pas boucler ;
        // l'avocat peut recharger la page ou relancer.
        this.stopPolling();
        this.cdr.markForCheck();
      },
    });
  }
}
