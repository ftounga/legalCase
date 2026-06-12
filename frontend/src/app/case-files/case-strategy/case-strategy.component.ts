import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { marked } from 'marked';
import { CaseStrategyService } from '../../core/services/case-strategy.service';
import { CaseStrategy } from '../../core/models/case-strategy.model';

/**
 * F-286 / SF-286-01 — carte « Stratégie de dossier » (onglet Décision, coiffe de la
 * colonne verdict, au-dessus du tableau de bord décisionnel).
 *
 * <p>Affiche la recommandation stratégique consolidée (couche LLM de synthèse EN LECTURE
 * des verdicts d'outils CALCULÉS + synthèse). N'altère AUCUN outil. Reprend le principe
 * F-258 : tant qu'aucun outil n'est calculé, n'invente rien — affiche un encart honnête.</p>
 *
 * <p>Design : gabarit F-282 (carte « document » étroite, navy/or, Merriweather/Inter/
 * JetBrains Mono, états vide/chargement/erreur soignés, CTA navy).</p>
 */
@Component({
  selector: 'app-case-strategy',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './case-strategy.component.html',
  styleUrl: './case-strategy.component.scss',
})
export class CaseStrategyComponent implements OnInit {
  @Input({ required: true }) caseFileId!: string;

  private readonly service = inject(CaseStrategyService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly strategy = signal<CaseStrategy | null>(null);
  readonly loading = signal(true);
  readonly generating = signal(false);

  readonly status = computed(() => this.strategy()?.status ?? null);
  readonly hasReco = computed(() => this.status() === 'READY' && !!this.strategy()?.content);
  /** Génération demandée mais 0 outil calculé : rien d'inventé (reprise F-258). */
  readonly emptyInput = computed(() => this.status() === 'EMPTY_INPUT');
  readonly toolsConsidered = computed(() => this.strategy()?.toolsConsidered ?? 0);
  readonly generatedAt = computed(() => this.strategy()?.generatedAt ?? null);

  /** HTML rendu (sanitizé par Angular au binding [innerHTML]) du markdown de la reco. */
  readonly contentHtml = computed<string>(() => {
    const content = this.strategy()?.content;
    if (!content) return '';
    return (marked.parse(content, { async: false }) as string).trim();
  });

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (s) => {
        this.strategy.set(s);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  generate(): void {
    if (this.generating()) return;
    this.generating.set(true);
    this.cdr.markForCheck();
    this.service.generate(this.caseFileId).subscribe({
      next: (s) => {
        this.strategy.set(s);
        this.generating.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.generating.set(false);
        this.snackBar.open('Échec de la génération de la stratégie.', 'Fermer', {
          duration: 5000,
          panelClass: ['snack-error'],
        });
        this.cdr.markForCheck();
      },
    });
  }
}
