import { ChangeDetectionStrategy, Component, Input, computed, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

import {
  buildDoctrineUrl,
  buildLexisPlusUrl,
  buildLextensoUrl,
  extractKeywords,
} from './jurisprudence-deeplink-builder';

/**
 * F-241 SF-241-01 — Boutons de recherche jurispru externe.
 *
 * Affiche 3 boutons compacts qui ouvrent dans un nouvel onglet la recherche
 * jurispru pré-formulée chez Doctrine / Lexis Plus / Lextenso depuis le texte
 * d'un point juridique. L'avocat conserve son propre abonnement chez l'éditeur
 * cible.
 *
 * Inputs :
 *  - [pointText] (requis) : texte du point juridique à transformer en query
 *  - [workspaceCountry] (optionnel, défaut 'FR') : 'FR' ou 'BE'. En 'BE',
 *    seul Doctrine BE est exposé (Lexis+ et Lextenso n'ont pas d'offre BE
 *    pertinente en V1).
 */
@Component({
  selector: 'app-jurisprudence-deeplinks',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatTooltipModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './jurisprudence-deeplinks.component.html',
  styleUrl: './jurisprudence-deeplinks.component.scss',
})
export class JurisprudenceDeeplinksComponent {
  private readonly pointTextSignal = signal<string>('');
  private readonly workspaceCountrySignal = signal<'FR' | 'BE'>('FR');

  @Input({ required: true })
  set pointText(value: string | null | undefined) {
    this.pointTextSignal.set(value ?? '');
  }

  @Input()
  set workspaceCountry(value: 'FR' | 'BE' | null | undefined) {
    this.workspaceCountrySignal.set(value === 'BE' ? 'BE' : 'FR');
  }

  protected readonly keywords = computed(() => extractKeywords(this.pointTextSignal()));
  protected readonly hasContent = computed(() => this.keywords().length > 0);
  protected readonly isBe = computed(() => this.workspaceCountrySignal() === 'BE');

  protected readonly doctrineUrl = computed(() =>
    buildDoctrineUrl(this.keywords(), this.workspaceCountrySignal())
  );
  protected readonly lexisPlusUrl = computed(() => buildLexisPlusUrl(this.keywords()));
  protected readonly lextensoUrl = computed(() => buildLextensoUrl(this.keywords()));
}
