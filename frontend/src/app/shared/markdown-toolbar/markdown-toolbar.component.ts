import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

/**
 * F-264 / SF-264-01 — Actions de mise en forme markdown émises par la barre
 * d'outils. L'insertion réelle (sur la sélection / au curseur du `textarea`)
 * est gérée par le composant hôte (`conclusions-section`), qui seul connaît
 * l'état du brouillon et la position du curseur.
 */
export type MarkdownAction =
  | 'h2'
  | 'h3'
  | 'bold'
  | 'italic'
  | 'list'
  | 'quote';

/**
 * F-264 / SF-264-01 — Barre d'outils markdown.
 *
 * Composant de présentation pur, isolable et réutilisable (corpus de style,
 * blog…). Il n'opère **pas** lui-même sur le texte : chaque bouton émet une
 * `MarkdownAction` via `action`. L'hôte applique l'insertion correspondante
 * (préfixe de ligne pour titre/liste/citation, enveloppe de sélection pour
 * gras/italique).
 *
 * Standalone, OnPush. Aucun état interne, aucune dépendance réseau.
 */
@Component({
  selector: 'app-markdown-toolbar',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './markdown-toolbar.component.html',
  styleUrl: './markdown-toolbar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MarkdownToolbarComponent {
  /** Désactive tous les boutons (ex. pendant un enregistrement). */
  @Input() disabled = false;

  /** Émet l'intention de mise en forme à appliquer par l'hôte. */
  @Output() readonly action = new EventEmitter<MarkdownAction>();

  /** Émet l'action demandée (ignorée si la barre est désactivée). */
  emit(value: MarkdownAction): void {
    if (this.disabled) {
      return;
    }
    this.action.emit(value);
  }
}
