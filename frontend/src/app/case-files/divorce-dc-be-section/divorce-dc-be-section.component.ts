import { Component, Input, OnChanges, OnInit, SimpleChanges, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

/**
 * SF-211-05 : wrapper informationnel V1 pour l'outil "Divorce par consentement
 * mutuel — Belgique" (F-211 SF-211-01 backend, CJ art. 1287+).
 *
 * Le composant de saisie complet (formulaire convention complète, pré-fill IA,
 * validation F-IA-03) sera livré dans une SF ultérieure dédiée. En attendant,
 * ce wrapper rend la card cliquable et signale à l'avocat que le backend est
 * opérationnel.
 *
 * Pattern de référence : `rupture-amiable-info-section` (SF-132-03 / F-237 SF-237-02).
 */
@Component({
  selector: 'app-divorce-dc-be-section',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './divorce-dc-be-section.component.html',
  styleUrl: './divorce-dc-be-section.component.scss',
})
export class DivorceDcBeSectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'DIVORCE CONSENTEMENT MUTUEL (BE)';
  static readonly TOOL_ICON = 'handshake';

  /**
   * F-237 SF-237-02 : composant informationnel V1 — exempté du helper PrefillRules.
   * L'étiquette `PREFILL_COUNT_ALWAYS_ZERO = true` indique au test d'intégrité
   * (`prefill-count-integrity.spec.ts`) qu'il vérifie uniquement
   * `getPrefillCount({}) === 0`. Pas de pré-fill IA tant que le formulaire complet
   * n'est pas livré.
   */
  static readonly PREFILL_COUNT_ALWAYS_ZERO = true;
  static getPrefillCount(): number {
    return 0;
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;

  collapsed = signal(false);

  readonly cadreJuridique =
    "Code judiciaire belge, art. 1287 à 1304 (procédure DC) ; Code civil belge, art. 1287-1294 (convention préalable).";

  readonly notes = [
    "Backend opérationnel : analyse de la procédure DC (vérification convention préalable, délai de réflexion 3 mois, audience devant le tribunal de la famille).",
    "Le composant de saisie complet (convention détaillée, calcul délais procéduraux, génération check-list pièces) sera livré dans une SF ultérieure dédiée.",
    "En attendant, l'endpoint /api/v1/case-files/{id}/divorce-dc-be-analysis reste accessible via les outils internes / l'API.",
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
