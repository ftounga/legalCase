import { Component, Input, signal } from '@angular/core';
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
export class RuptureAmiableInfoSectionComponent {
  @Input() caseFileId!: string;

  collapsed = signal(false);

  readonly messages = [
    "Aucun barème légal ne s'impose en rupture amiable belge. Le montant est librement négocié entre les parties.",
    "Le salarié conserve le droit à l'indemnité compensatoire de préavis si la rupture n'est pas effective (cf. F-DT-05)."
  ];

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }
}
