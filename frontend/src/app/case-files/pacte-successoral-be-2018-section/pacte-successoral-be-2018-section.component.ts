import { Component, Input, OnChanges, OnInit, SimpleChanges, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-211-05 : wrapper informationnel V1 pour l'outil "Pacte successoral 2018
 * — Belgique" (F-211 SF-211-04 backend, Loi du 31/07/2017 réformant le droit
 * des successions, entrée en vigueur 01/09/2018).
 *
 * Outil BE pur — pas d'équivalent FR strict.
 *
 * Le composant de saisie complet (formulaire pacte, vérification des conditions
 * légales, génération de l'acte authentique) sera livré dans une SF ultérieure.
 *
 * Pattern de référence : `rupture-amiable-info-section` (SF-132-03 / F-237 SF-237-02).
 */
@Component({
  selector: 'app-pacte-successoral-be-2018-section',
  standalone: true,
  imports: [CommonModule, MatIconModule, ToolJurisprudenceCitationsComponent],
  templateUrl: './pacte-successoral-be-2018-section.component.html',
  styleUrl: './pacte-successoral-be-2018-section.component.scss',
})
export class PacteSuccessoralBe2018SectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99f — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'pacte-successoral-be-2018';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'PACTE SUCCESSORAL 2018 (BE)';
  static readonly TOOL_ICON = 'description';

  /**
   * F-237 SF-237-02 : composant informationnel V1 — exempté du helper PrefillRules.
   * `getPrefillCount({}) === 0` (pas de formulaire de saisie tant que le composant
   * complet n'est pas livré).
   */
  static readonly PREFILL_COUNT_ALWAYS_ZERO = true;
  static getPrefillCount(): number {
    return 0;
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;

  collapsed = signal(false);

  readonly cadreJuridique =
    "Loi du 31 juillet 2017 réformant le droit des successions (entrée en vigueur 01/09/2018) — pactes successoraux global, ponctuel et de famille.";

  readonly notes = [
    "Backend opérationnel : analyse de validité du pacte (qualité des parties, forme authentique notariée obligatoire, équilibre du pacte global, consentement éclairé).",
    "Outil BE pur — la réforme 2018 a autorisé certains pactes successoraux historiquement interdits. Pas d'équivalent FR strict.",
    "Le composant de saisie complet (formulaire détaillé pacte global/ponctuel/famille, vérification des conditions légales, génération de l'acte) sera livré dans une SF ultérieure.",
  ];

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }
}
