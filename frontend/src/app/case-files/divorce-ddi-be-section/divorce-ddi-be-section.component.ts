import { Component, Input, OnChanges, OnInit, SimpleChanges, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

/**
 * SF-211-05 : wrapper informationnel V1 pour l'outil "Divorce — désunion
 * irrémédiable, orientation 3 voies — Belgique" (F-211 SF-211-02 backend,
 * Code civil belge art. 229 §§1/2/3).
 *
 * Diffère de l'outil F-FA-11 (validation globale désunion irrémédiable) :
 * F-FA-29 oriente entre les 3 voies §1 (faits), §2 (consentement), §3 (séparation).
 *
 * Le composant de saisie complet (questionnaire d'orientation, calcul des
 * conditions §1 / §2 / §3) sera livré dans une SF ultérieure.
 *
 * Pattern de référence : `rupture-amiable-info-section` (SF-132-03 / F-237 SF-237-02).
 */
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

@Component({
  selector: 'app-divorce-ddi-be-section',
  standalone: true,
  imports: [CommonModule, MatIconModule,
    ToolJurisprudenceCitationsComponent],
  templateUrl: './divorce-ddi-be-section.component.html',
  styleUrl: './divorce-ddi-be-section.component.scss',
})
export class DivorceDdiBeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-FA-11-desunion-irremediable-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'DIVORCE DDI 3 VOIES (BE)';
  static readonly TOOL_ICON = 'rule';

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
    "Code civil belge, art. 229 §1 (faits) / §2 (consentement) / §3 (séparation de fait > 1 an).";

  readonly notes = [
    "Backend opérationnel : analyse des conditions d'ouverture des 3 voies (faits rendant impossible la poursuite de la vie commune §1 ; consentement mutuel à divorcer §2 ; séparation de fait > 1 an §3).",
    "Diffère de F-FA-11 (validation globale désunion irrémédiable) — cet outil oriente l'avocat sur la voie procédurale la plus adaptée selon les faits du dossier.",
    "Le composant de saisie complet (questionnaire détaillé par voie, calculateur du délai de séparation, génération de la requête appropriée) sera livré dans une SF ultérieure.",
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
