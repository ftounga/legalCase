import {
  ChangeDetectionStrategy,
  Component,
  Input,
} from '@angular/core';
import { MatProgressBarModule } from '@angular/material/progress-bar';

/**
 * SF-287-01 — Barre de progression PRÉCISE de la génération des conclusions.
 *
 * Affichée à l'état PENDING/PROCESSING à la place du spinner muet. Le pourcentage
 * est RÉEL (tokens reçus / `maxTokens`, plafonné à 100 %) et la section en cours
 * est le dernier titre `##`/`###` détecté dans le flux partiel.
 *
 * `mat-progress-bar` en mode **déterminé** (`[value]="percent"`), stylé design
 * system (piste marine `#1A3A5C`, remplissage or `#C9973A`, hauteur confortable,
 * coins arrondis, transition fluide). Accessible : `aria-valuenow` reflété sur
 * la barre, libellé Inter lisible. Standalone, OnPush (entrées immuables).
 */
@Component({
  selector: 'app-conclusion-generation-progress',
  standalone: true,
  imports: [MatProgressBarModule],
  templateUrl: './conclusion-generation-progress.component.html',
  styleUrl: './conclusion-generation-progress.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConclusionGenerationProgressComponent {
  /** Pourcentage d'avancement (0..100). Borné par le composant par sécurité. */
  @Input() percent = 0;

  /** Dernier titre de section détecté (`##`/`###`), ou `null` (préambule en cours). */
  @Input() currentSection: string | null = null;

  /** Index 1-based de la section en cours (indicatif), ou `null`. */
  @Input() sectionIndex: number | null = null;

  /** Pourcentage borné [0,100] et arrondi, pour l'affichage et `aria-valuenow`. */
  get clampedPercent(): number {
    if (!Number.isFinite(this.percent)) {
      return 0;
    }
    return Math.max(0, Math.min(100, Math.round(this.percent)));
  }

  /**
   * Libellé de la section en cours. Avant le premier titre détecté, affiche un
   * libellé d'attente honnête (« Préparation de l'acte… ») plutôt qu'une section
   * inventée.
   */
  get sectionLabel(): string {
    const section = this.currentSection?.trim();
    if (!section) {
      return 'Préparation de l’acte…';
    }
    return this.sectionIndex != null
      ? `${section} (section ${this.sectionIndex})`
      : section;
  }
}
