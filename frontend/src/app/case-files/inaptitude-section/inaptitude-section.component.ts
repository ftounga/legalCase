import { Component, Input, OnInit, OnChanges, SimpleChanges, Optional, signal, computed } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { InaptitudeService } from '../../core/services/inaptitude.service';
import {
  InaptitudeResponse,
  OrigineInaptitude,
  OrigineInaptitudeOption,
  ORIGINES_BE,
  ORIGINES_FR,
} from '../../core/models/inaptitude.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { TravailExtractedData, PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert, CoherenceAlertSource } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

/**
 * SF-155-04-A2 : types pour les alertes de cohérence IA (pattern F-IA-03 aligné
 * sur `immigration-title-decision-section`).
 * SF-155-05 : interface factorisée via `CoherenceAlert<F>` shared.
 */
export type InaptitudeAlertField = 'SALAIRE' | 'ORIGINE' | 'AVIS_DATE' | 'RECLASSEMENT';
export type InaptitudeAlertSource = CoherenceAlertSource;
export type InaptitudeCoherenceAlert = CoherenceAlert<InaptitudeAlertField>;

/**
 * SF-155-04-A2 : mapping origine IA (enum tripartite AT/MP/MO) vers enum
 * frontend FR binaire (PROFESSIONNELLE/NON_PROFESSIONNELLE — article L.1226-14
 * CT couvre accident travail + maladie professionnelle).
 */
const ORIGINE_IA_TO_FRONT: Record<string, OrigineInaptitude> = {
  ACCIDENT_TRAVAIL: 'PROFESSIONNELLE',
  MALADIE_PROFESSIONNELLE: 'PROFESSIONNELLE',
  MALADIE_ORDINAIRE: 'NON_PROFESSIONNELLE',
};

const SALAIRE_DIVERGENCE_THRESHOLD = 0.10; // 10 %
const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

/**
 * SF-DT-15-02 : outil décisionnel dédié "Licenciement pour inaptitude"
 * (F-DT-15). FR + BE. Consomme l'API SF-DT-15-01. Affiché conditionnellement
 * par le panel F-IA-04 (tool_id 'F-DT-15-inaptitude').
 *
 * SF-155-04-A2 : ajout du pré-remplissage IA (5 champs) + validation F-IA-03
 * (alertes cohérence) aligné sur le pattern canonique
 * `immigration-title-decision-section` (F-IM-05 SF-IM-05-04).
 */
@Component({
  selector: 'app-inaptitude-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './inaptitude-section.component.html',
  styleUrl: './inaptitude-section.component.scss',
})
export class InaptitudeSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // SF-155-04-A2 : sources IA pour pré-fill + validation F-IA-03
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<InaptitudeResponse | null>(null);

  salaireMensuelReference = signal<number | null>(null);
  ancienneteAnnees = signal<number | null>(null);
  origineInaptitude = signal<OrigineInaptitude | null>(null);
  reclassementRespecte = signal<boolean>(false);
  avisMedecinTravailDate = signal<string | null>(null);

  // SF-155-04-A2 : provenance IA par champ (badge "Pré-rempli depuis l'analyse").
  // Effacée dès que l'avocat modifie manuellement un champ (onXxxChange).
  provenanceSalaire = signal<'IA' | null>(null);
  provenanceAnciennete = signal<'IA' | null>(null);
  provenanceOrigineInaptitude = signal<'IA' | null>(null);
  provenanceAvisMedecinDate = signal<'IA' | null>(null);
  provenanceReclassement = signal<'IA' | null>(null);

  originesDisponibles = computed<OrigineInaptitudeOption[]>(() =>
    this.workspaceCountry === 'BELGIQUE' ? ORIGINES_BE : ORIGINES_FR
  );

  /** SF-155-04-A2 : note info IA (pas une alerte bloquante) si salaire déduit d'un net × 1,30. */
  salaireEstDeduitNote = computed<boolean>(() => this.aiData?.salaireEstDeduit === true);

  /** SF-155-04-A2 : alertes de cohérence F-IA-03 entre valeurs avocat et IA. */
  coherenceAlerts = computed<Partial<Record<InaptitudeAlertField, InaptitudeCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<InaptitudeAlertField, InaptitudeCoherenceAlert>> = {};
    const salaireAlert = this.buildSalaireAlert();
    if (salaireAlert) alerts.SALAIRE = salaireAlert;
    const origineAlert = this.buildOrigineAlert();
    if (origineAlert) alerts.ORIGINE = origineAlert;
    const avisAlert = this.buildAvisDateAlert();
    if (avisAlert) alerts.AVIS_DATE = avisAlert;
    const reclassAlert = this.buildReclassementAlert();
    if (reclassAlert) alerts.RECLASSEMENT = reclassAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length };
  });

  constructor(
    private service: InaptitudeService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // SF-155-04-A2 : ré-applique le pré-fill si aiData change APRÈS ngOnInit,
    // tant que l'avocat n'a pas encore validé un résultat (form actif, pas de
    // persistance côté backend). Pas d'écrasement si result() est déjà chargé.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const s = this.salaireMensuelReference();
    const a = this.ancienneteAnnees();
    const o = this.origineInaptitude();
    return s !== null && s > 0
      && a !== null && a >= 0 && Number.isInteger(a)
      && o !== null;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request = {
      salaireMensuelReference: this.salaireMensuelReference()!,
      ancienneteAnnees: this.ancienneteAnnees()!,
      origineInaptitude: this.origineInaptitude()!,
      reclassementRespecte: this.reclassementRespecte(),
      ...(this.avisMedecinTravailDate()
        ? { avisMedecinTravailDate: this.avisMedecinTravailDate()! }
        : {}),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Indemnité calculée', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors du calcul';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.salaireMensuelReference.set(r.salaireMensuelReference);
        this.ancienneteAnnees.set(r.ancienneteAnnees);
        this.origineInaptitude.set(r.origineInaptitude);
        this.reclassementRespecte.set(r.reclassementRespecte);
        this.avisMedecinTravailDate.set(r.avisMedecinTravailDate);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        // SF-155-04-A2 : dans ce cas, on tente un pré-fill depuis l'analyse IA.
        this.loading.set(false);
        this.prefillFromAi();
      },
    });
  }

  /**
   * SF-155-04-A2 : pré-remplit les champs depuis l'analyse IA (`aiData`).
   * Appelée uniquement quand aucune analyse persistée n'existe (404) ou quand
   * `aiData` change pendant l'édition (ngOnChanges). Ne jamais écraser une
   * analyse déjà persistée côté backend.
   */
  private prefillFromAi(): void {
    if (!this.aiData) return;

    // 1. Salaire brut mensuel → salaireMensuelReference
    const salaireIa = this.aiData.salaireBrutMensuel;
    if (typeof salaireIa === 'number' && salaireIa > 0) {
      this.salaireMensuelReference.set(salaireIa);
      this.provenanceSalaire.set('IA');
    }

    // 2. Ancienneté dérivée depuis dateEntree (YYYY-MM-DD historique)
    const dateEntree = this.aiData.dateEntree;
    if (dateEntree && ISO_DATE_REGEX.test(dateEntree)) {
      const anciennete = this.computeAncienneteAnnees(dateEntree);
      if (anciennete !== null) {
        this.ancienneteAnnees.set(anciennete);
        this.provenanceAnciennete.set('IA');
      }
    }

    // 3. Origine inaptitude : mapping AT/MP → PROFESSIONNELLE, MO → NON_PROFESSIONNELLE
    const origineIa = this.aiData.origineInaptitudePressentie;
    if (origineIa && ORIGINE_IA_TO_FRONT[origineIa]) {
      const mapped = ORIGINE_IA_TO_FRONT[origineIa];
      // Si on est côté BE, le mapping FR ne s'applique pas — skip silencieux
      // (les origines BE ne sont pas encore extraites par le prompt IA, cf.
      // mini-spec backend SF-155-04-00-BE-travail §2.2).
      if (this.workspaceCountry !== 'BELGIQUE') {
        this.origineInaptitude.set(mapped);
        this.provenanceOrigineInaptitude.set('IA');
      }
    }

    // 4. Date avis médecin du travail (YYYY-MM-DD strict)
    const avisDate = this.aiData.avisMedecinTravailDate;
    if (avisDate && ISO_DATE_REGEX.test(avisDate)) {
      this.avisMedecinTravailDate.set(avisDate);
      this.provenanceAvisMedecinDate.set('IA');
    }

    // 5. Reclassement respecté : OUI → true, NON → false, INCONNU → ne touche pas
    const reclassIa = this.aiData.reclassementRespecteDetected?.reponse;
    if (reclassIa === 'OUI') {
      this.reclassementRespecte.set(true);
      this.provenanceReclassement.set('IA');
    } else if (reclassIa === 'NON') {
      this.reclassementRespecte.set(false);
      this.provenanceReclassement.set('IA');
    }
    // INCONNU ou undefined → laisser la valeur avocat (défaut false)
  }

  /**
   * Calcule l'ancienneté en années entières (arrondi inférieur) entre `dateEntree`
   * et aujourd'hui. Retourne `null` si date future ou date invalide.
   */
  private computeAncienneteAnnees(dateEntree: string): number | null {
    const entree = new Date(dateEntree);
    if (isNaN(entree.getTime())) return null;
    const now = new Date();
    if (entree > now) return null;
    const diffMs = now.getTime() - entree.getTime();
    const years = Math.floor(diffMs / (365.25 * 24 * 60 * 60 * 1000));
    return years >= 0 ? years : null;
  }

  // Handlers manuels : effacent le badge de provenance IA correspondant
  onSalaireChange(value: number | null): void {
    this.salaireMensuelReference.set(value);
    this.provenanceSalaire.set(null);
  }

  onAncienneteChange(value: number | null): void {
    this.ancienneteAnnees.set(value);
    this.provenanceAnciennete.set(null);
  }

  onOrigineChange(value: OrigineInaptitude | null): void {
    this.origineInaptitude.set(value);
    this.provenanceOrigineInaptitude.set(null);
  }

  onAvisMedecinDateChange(value: string | null): void {
    this.avisMedecinTravailDate.set(value || null);
    this.provenanceAvisMedecinDate.set(null);
  }

  onReclassementChange(value: boolean): void {
    this.reclassementRespecte.set(value);
    this.provenanceReclassement.set(null);
  }

  // Helpers alert builders (pattern F-IM-05 / F-IA-03)
  alertTooltip(alert: InaptitudeCoherenceAlert): string {
    return alert.reason;
  }

  alertBadgeLabel(alert: InaptitudeCoherenceAlert): string {
    return `Incohérence détectée (${alert.expectedDisplay})`;
  }

  private buildSalaireAlert(): InaptitudeCoherenceAlert | null {
    const iaVal = this.aiData?.salaireBrutMensuel;
    const user = this.salaireMensuelReference();
    if (typeof iaVal !== 'number' || iaVal <= 0) return null;
    if (user === null || user <= 0) return null;
    const ratio = Math.abs(user - iaVal) / iaVal;
    if (ratio <= SALAIRE_DIVERGENCE_THRESHOLD) return null;
    return CoherenceAlertBuilder.forField<InaptitudeAlertField>('SALAIRE')
      .addSource('IA', {
        expectedDisplay: `${iaVal.toLocaleString('fr-FR')} €`,
        reason: `Analyse du dossier : salaire ${iaVal.toLocaleString('fr-FR')} € (écart > 10 % avec la valeur saisie)`,
      })
      .build();
  }

  private buildOrigineAlert(): InaptitudeCoherenceAlert | null {
    const iaCode = this.aiData?.origineInaptitudePressentie;
    if (!iaCode || !ORIGINE_IA_TO_FRONT[iaCode]) return null;
    const mapped = ORIGINE_IA_TO_FRONT[iaCode];
    const user = this.origineInaptitude();
    if (!user) return null;
    if (user === mapped) return null;
    // Ignorer côté BE (mapping non applicable)
    if (this.workspaceCountry === 'BELGIQUE') return null;
    const label = mapped === 'PROFESSIONNELLE'
      ? 'Origine professionnelle'
      : 'Origine non professionnelle';
    return CoherenceAlertBuilder.forField<InaptitudeAlertField>('ORIGINE')
      .addSource('IA', {
        expectedDisplay: label,
        reason: `Analyse du dossier : origine ${iaCode} → ${label}`,
      })
      .build();
  }

  private buildAvisDateAlert(): InaptitudeCoherenceAlert | null {
    const iaDate = this.aiData?.avisMedecinTravailDate;
    if (!iaDate || !ISO_DATE_REGEX.test(iaDate)) return null;
    const user = this.avisMedecinTravailDate();
    if (!user) return null;
    if (user === iaDate) return null;
    return CoherenceAlertBuilder.forField<InaptitudeAlertField>('AVIS_DATE')
      .addSource('IA', {
        expectedDisplay: iaDate,
        reason: `Analyse du dossier : avis du ${iaDate}`,
      })
      .build();
  }

  private buildReclassementAlert(): InaptitudeCoherenceAlert | null {
    const iaDetect = this.aiData?.reclassementRespecteDetected;
    if (!iaDetect || iaDetect.reponse === 'INCONNU' || !iaDetect.reponse) return null;
    const iaRespecte = iaDetect.reponse === 'OUI';
    if (iaRespecte === this.reclassementRespecte()) return null;
    return CoherenceAlertBuilder.forField<InaptitudeAlertField>('RECLASSEMENT')
      .addSource('IA', {
        expectedDisplay: iaRespecte ? 'Reclassement respecté' : 'Reclassement NON respecté',
        reason: iaDetect.justification
          ? `Analyse du dossier : ${iaDetect.reponse} (${iaDetect.justification})`
          : `Analyse du dossier : obligation ${iaRespecte ? 'respectée' : 'NON respectée'}`,
      })
      .build();
  }
}
