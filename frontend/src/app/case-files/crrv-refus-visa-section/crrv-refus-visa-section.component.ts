import { Component, Input, OnChanges, OnInit, SimpleChanges, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

/**
 * F-208 — Wrapper informationnel pour l'outil décisionnel "Commission de Recours contre Refus de Visa — Recours hiérarchique 2 mois".
 *
 * Backend complet livré dans la PR #915 (calculator/analyzer + endpoint POST/GET +
 * table crrv_refus_visa_analyses + migration Liquibase). Ce wrapper
 * frontend rappelle le cadre juridique et l'existence de l'endpoint ; le composant
 * de saisie/visualisation complet (formulaire + verdict + pré-fill IA + F-IA-03)
 * sera livré dans une SF ultérieure de F-208.
 *
 * Pattern miroir de {@code RuptureAmiableInfoSectionComponent} (composant
 * informationnel sans HTTP).
 */
@Component({
  selector: 'app-crrv-refus-visa-section',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './crrv-refus-visa-section.component.html',
  styleUrl: './crrv-refus-visa-section.component.scss'
})
export class CrrvRefusVisaSectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'CRRV REFUS VISA';
  static readonly TOOL_ICON = 'mail';

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;

  collapsed = signal(false);

  readonly title = 'Commission de Recours contre Refus de Visa — Recours hiérarchique 2 mois';
  readonly description = "Outil P1 — Recours hiérarchique 2 mois contre refus de visa consulaire (Décret 2000-1093). Calculator backend disponible via POST /api/v1/case-files/{id}/crrv-refus-visa-analysis.";

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
