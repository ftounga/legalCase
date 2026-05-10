import { Component, Input, OnChanges, OnInit, SimpleChanges, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

/**
 * SF-132-03 : outil informationnel dédié "Rupture amiable" (Belgique).
 * Affiché conditionnellement par case-file-detail quand
 * compensationEstimate.typeRupture == RUPTURE_AMIABLE && country == BELGIQUE.
 *
 * Contrairement aux autres outils décisionnels, celui-ci ne calcule ni persiste
 * rien : la rupture amiable belge n'a pas de barème légal, c'est une négociation
 * libre entre les parties. Le composant se contente de rappeler le cadre juridique.
 */
@Component({
  selector: 'app-rupture-amiable-info-section',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './rupture-amiable-info-section.component.html',
  styleUrl: './rupture-amiable-info-section.component.scss'
})
export class RuptureAmiableInfoSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-11 : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'RUPTURE AMIABLE';
  static readonly TOOL_ICON = 'handshake';

  /**
   * F-177 SF-177-12 / F-236 SF-236-02 — outil informationnel pur (pas de
   * formulaire, pas de pré-fill possible). Conservé pour cohérence avec
   * le contrat unifié de tous les outils décisionnels.
   */
  static getPrefillCount(): number {
    return 0;
  }

  @Input() caseFileId!: string;
  // F-177 SF-177-11 : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;

  collapsed = signal(false);

  readonly messages = [
    "Aucun barème légal ne s'impose en rupture amiable belge. Le montant est librement négocié entre les parties.",
    "Le salarié conserve le droit à l'indemnité compensatoire de préavis si la rupture n'est pas effective (cf. F-DT-05)."
  ];

  ngOnInit(): void {
    // F-177 SF-177-11 : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-11 : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }
}
