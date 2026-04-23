import {
  Component,
  Input,
  OnChanges,
  OnInit,
  SimpleChanges,
  Type,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseFileService, VisibleToolSet } from '../../core/services/case-file.service';
import { AncienneteSectionComponent } from '../anciennete-section/anciennete-section.component';
import { LicenciementSectionComponent } from '../licenciement-section/licenciement-section.component';
import { RuptureConvSectionComponent } from '../rupture-conv-section/rupture-conv-section.component';
import { RuptureConvIndemniteSectionComponent } from '../rupture-conv-indemnite-section/rupture-conv-indemnite-section.component';
import { IndemniteComparatifSectionComponent } from '../indemnite-comparatif-section/indemnite-comparatif-section.component';
import { PrudhomeFicheSectionComponent } from '../prudhome-fiche-section/prudhome-fiche-section.component';
import { TribunalTravailFicheSectionComponent } from '../tribunal-travail-fiche-section/tribunal-travail-fiche-section.component';
import { PartageImmobilierSectionComponent } from '../partage-immobilier-section/partage-immobilier-section.component';
import { CalendrierGardeSectionComponent } from '../calendrier-garde-section/calendrier-garde-section.component';
import { DivorceChecklistSectionComponent } from '../divorce-checklist-section/divorce-checklist-section.component';
import { ImmigrationTitleDecisionSectionComponent } from '../immigration-title-decision-section/immigration-title-decision-section.component';
import { ImmigrationRecoursSectionComponent } from '../immigration-recours-section/immigration-recours-section.component';
import { ImmigrationWorkRightSectionComponent } from '../immigration-work-right-section/immigration-work-right-section.component';
import { ImmigrationChecklistSectionComponent } from '../immigration-checklist-section/immigration-checklist-section.component';

/**
 * SF-IA-04-02 : panel conteneur qui consomme le moteur d'affichage
 * conditionnel F-IA-04 et rend les outils décisionnels d'un dossier en
 * trois couches (always-on / contextual / catalog).
 *
 * Ce composant est isolé dans cette SF : il n'est intégré dans
 * `case-file-detail` qu'en SF-IA-04-03, et dans le dashboard F-IA-02
 * qu'en SF-IA-04-04.
 */
@Component({
  selector: 'app-decisional-tools-panel',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatChipsModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './decisional-tools-panel.component.html',
  styleUrls: ['./decisional-tools-panel.component.scss'],
})
export class DecisionToolsPanelComponent implements OnInit, OnChanges {
  private readonly caseFileService = inject(CaseFileService);
  private readonly snackBar = inject(MatSnackBar);

  @Input({ required: true }) caseFileId!: string;
  @Input() synthesis: unknown = null;

  readonly loading = signal(false);
  readonly visibility = signal<VisibleToolSet | null>(null);

  /**
   * Registre statique `tool_id -> ComponentType`. Un nouvel outil décisionnel
   * se branche en ajoutant une ligne ici (et dans la seed migration côté
   * backend). Les tool_id non mappés sont skippés avec warning.
   */
  static readonly TOOL_REGISTRY: ReadonlyMap<string, Type<unknown>> = new Map<string, Type<unknown>>([
    ['F-DT-04-fiche-prudhomale', PrudhomeFicheSectionComponent],
    ['F-DT-06-requete-tribunal-travail', TribunalTravailFicheSectionComponent],
    ['F-DT-07-anciennete-conges-prime', AncienneteSectionComponent],
    ['F-DT-08-licenciement-validity', LicenciementSectionComponent],
    ['F-DT-09-comparateur-indemnites', IndemniteComparatifSectionComponent],
    ['F-DT-10-rupture-conv-validity', RuptureConvSectionComponent],
    ['F-132-rupture-conv-indemnite', RuptureConvIndemniteSectionComponent],
    ['F-FA-05-partage-immobilier', PartageImmobilierSectionComponent],
    ['F-FA-06-calendrier-garde', CalendrierGardeSectionComponent],
    ['F-FA-07-checklist-divorce', DivorceChecklistSectionComponent],
    ['F-IM-01-checklist-pieces', ImmigrationChecklistSectionComponent],
    ['F-IM-05-arbre-decisionnel-titre', ImmigrationTitleDecisionSectionComponent],
    ['F-IM-06-recours', ImmigrationRecoursSectionComponent],
    ['F-IM-07-droit-au-travail', ImmigrationWorkRightSectionComponent],
  ]);

  ngOnInit(): void {
    if (this.caseFileId) {
      this.loadVisibility();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['caseFileId'] && !changes['caseFileId'].firstChange && this.caseFileId) {
      this.loadVisibility();
    }
  }

  private loadVisibility(): void {
    this.loading.set(true);
    this.caseFileService.getDecisionToolsVisibility(this.caseFileId).subscribe({
      next: (result) => {
        this.visibility.set(result);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.visibility.set({ alwaysOn: [], contextual: [], catalog: [] });
        this.snackBar.open(
          'Impossible de charger les outils du dossier. Réessayez plus tard.',
          'Fermer',
          { duration: 4000 }
        );
      },
    });
  }

  resolveComponent(toolId: string): Type<unknown> | null {
    const component = DecisionToolsPanelComponent.TOOL_REGISTRY.get(toolId);
    if (!component) {
      // eslint-disable-next-line no-console
      console.warn(`[decisional-tools-panel] Unknown toolId: ${toolId}`);
      return null;
    }
    return component;
  }

  resolvedAlwaysOn(): { toolId: string; component: Type<unknown> }[] {
    const v = this.visibility();
    if (!v) return [];
    return v.alwaysOn
      .map((toolId) => ({ toolId, component: this.resolveComponent(toolId) }))
      .filter((x): x is { toolId: string; component: Type<unknown> } => x.component !== null);
  }

  resolvedContextual(): { toolId: string; component: Type<unknown> }[] {
    const v = this.visibility();
    if (!v) return [];
    return v.contextual
      .map((toolId) => ({ toolId, component: this.resolveComponent(toolId) }))
      .filter((x): x is { toolId: string; component: Type<unknown> } => x.component !== null);
  }

  isEmpty(): boolean {
    const v = this.visibility();
    if (!v) return false;
    return v.alwaysOn.length === 0 && v.contextual.length === 0;
  }

  componentInputs(): Record<string, unknown> {
    return {
      caseFileId: this.caseFileId,
      synthesis: this.synthesis,
    };
  }
}
