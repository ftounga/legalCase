import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

import { DecisionToolCardComponent } from '../../decisional-tools-panel/decision-tool-card/decision-tool-card.component';
import { DecisionToolSummary, MetierAlertLevel } from '../../decisional-tools-panel/decision-tool-summary.model';
import { DashboardTile } from '../../../core/models/case-dashboard.model';

/**
 * F-167 SF-167-01 — Tile dashboard générique. Délègue le rendu au composant
 * partagé `<app-decision-tool-card>` (SF-177-01).
 *
 * Reçoit une {@link DashboardTile} DTO provenant du backend (record
 * `fr.ailegalcase.casefile.DashboardTile`), émet `open(toolId)` au clic.
 *
 * Mapping `theme` → icône Material reproduit côté frontend (le backend ne
 * renvoie que le code de thème, voir SF-167-05 pour la convergence avec
 * `THEME_BY_TOOL_ID` / `TOOL_REGISTRY`).
 */
@Component({
  selector: 'app-dashboard-tile',
  standalone: true,
  imports: [DecisionToolCardComponent],
  template: `
    <app-decision-tool-card
      [toolId]="tile.toolId"
      [theme]="tile.theme"
      [icon]="iconForTheme(tile.theme)"
      [title]="tile.label"
      [summary]="summaryFromTile()"
      [metierAlertLevel]="metierAlertFromTile()"
      (open)="open.emit(tile.toolId)">
    </app-decision-tool-card>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardTileComponent {
  @Input({ required: true }) tile!: DashboardTile;

  @Output() open = new EventEmitter<string>();

  iconForTheme(theme: string): string {
    // F-192 SF-192-02 — la tile résumé pistes retenues utilise une icône
    // dédiée (push_pin) pour signal visuel cohérent avec les badges or des
    // sorties outils + pills cards panel F-IA-04.
    if (this.tile?.toolId === 'RETAINED_PISTES_SUMMARY') {
      return 'push_pin';
    }
    // F-194 SF-194-02 — la tile résumé pièces manquantes utilise `description`
    // (cohérent thème DOCUMENTS) pour signaler le périmètre documentaire et
    // distinguer de la tile pistes (push_pin).
    if (this.tile?.toolId === 'F-194-pieces-summary') {
      return 'description';
    }
    // F-195 SF-195-02 — la tile résumé risques utilise `warning` (cohérent
    // thème DIAGNOSTIC + nature du bloc Risques de la synthèse) pour la
    // distinguer des autres tiles diagnostiques (health_and_safety).
    if (this.tile?.toolId === 'F-195-risques-summary') {
      return 'warning';
    }
    // F-196 SF-196-02 — la tile résumé questions complémentaires utilise
    // `quiz` (cohérent thème DOCUMENTS + nature du bloc Questions F-94 de la
    // synthèse) pour la distinguer de la tile pièces (description).
    if (this.tile?.toolId === 'F-196-questions-summary') {
      return 'quiz';
    }
    switch (theme) {
      case 'INDEMNITES':
        return 'euro';
      case 'VALIDITE':
        return 'verified';
      case 'DELAIS':
        return 'schedule';
      case 'DOCUMENTS':
        return 'description';
      case 'DIAGNOSTIC':
        return 'health_and_safety';
      default:
        return 'extension';
    }
  }

  summaryFromTile(): DecisionToolSummary {
    return {
      label: this.tile.label,
      primaryValue: this.tile.primaryValue,
      secondaryValue: this.tile.secondaryValue ?? undefined,
      alertLevel: this.metierAlertFromTile() ?? undefined,
    };
  }

  metierAlertFromTile(): MetierAlertLevel | null {
    const lvl = this.tile.alertLevel;
    if (lvl === 'OK' || lvl === 'WARNING' || lvl === 'ALERT') {
      return lvl;
    }
    return null;
  }
}
