import { Component, Input, OnChanges, OnInit, SimpleChanges, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-211-05 : wrapper informationnel V1 pour l'outil "Mesures provisoires
 * Tribunal de la famille — Belgique" (F-211 SF-211-03 backend, CJ art. 1280).
 *
 * Mode visibility ALWAYS_ON sur dossier Famille BE : mesures provisoires
 * urgentes pertinentes pour toutes les procédures familiales (DC, DDI, autres).
 *
 * Le composant de saisie complet (formulaire mesures, calcul délais, génération
 * de la requête CJ 1280) sera livré dans une SF ultérieure.
 *
 * Pattern de référence : `rupture-amiable-info-section` (SF-132-03 / F-237 SF-237-02).
 */
@Component({
  selector: 'app-tribunal-famille-be-mesures-provisoires-section',
  standalone: true,
  imports: [CommonModule, MatIconModule, ToolJurisprudenceCitationsComponent],
  templateUrl: './tribunal-famille-be-mesures-provisoires-section.component.html',
  styleUrl: './tribunal-famille-be-mesures-provisoires-section.component.scss',
})
export class TribunalFamilleBeMesuresProvisoiresSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99f — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'tribunal-famille-be-mesures-prov';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'MESURES PROVISOIRES TRIBUNAL FAMILLE (BE)';
  static readonly TOOL_ICON = 'gavel';

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
    "Code judiciaire belge, art. 1280 : mesures provisoires du Tribunal de la famille (résidence enfants, contribution alimentaire, jouissance immeuble, etc.).";

  readonly notes = [
    "Backend opérationnel : analyse de la pertinence des mesures provisoires demandables (résidence des enfants, droit de visite, contribution alimentaire entre époux et pour enfants, jouissance du logement familial, interdiction d'aliéner).",
    "Outil transversal : pertinent pour toute procédure familiale BE en cours (divorce DC, DDI, séparation de fait), d'où le mode visibility ALWAYS_ON.",
    "Le composant de saisie complet (formulaire mesures demandées, calcul délais procéduraux, génération de la requête CJ 1280) sera livré dans une SF ultérieure.",
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
