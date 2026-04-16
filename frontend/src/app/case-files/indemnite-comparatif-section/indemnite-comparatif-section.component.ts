import { Component, Input, OnInit, OnChanges, SimpleChanges, Optional, signal, computed } from '@angular/core';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { CaseAnalysisResult, TravailExtractedData } from '../../core/models/case-analysis.model';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { IndemniteComparatifService } from '../../core/services/indemnite-comparatif.service';
import { IndemniteComparatifResponse } from '../../core/models/indemnite-comparatif.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';

interface TypeRuptureOption {
  value: string;
  label: string;
}

export type IndemniteAlertField = 'TYPE_RUPTURE' | 'ANCIENNETE' | 'SALAIRE';
export type IndemniteAlertSource = 'F96' | 'QUESTION_IA' | 'IA' | 'MULTI';
export type IndemniteAlertLevel = 'blocker' | 'warning';

export interface IndemniteCoherenceAlert {
  field: IndemniteAlertField;
  level: IndemniteAlertLevel;
  source: IndemniteAlertSource;
  expectedDisplay: string;
  contributors: IndemniteAlertSource[];
  f96Raison?: string | null;
  questionText?: string | null;
  questionAnswer?: string | null;
}

const KNOWN_TYPE_RUPTURE_VALUES = new Set([
  'LICENCIEMENT', 'LICENCIEMENT_ECONOMIQUE', 'RUPTURE_CONVENTIONNELLE',
  'LICENCIEMENT_ORDINAIRE', 'RUPTURE_AMIABLE',
]);

const TYPES_FR: TypeRuptureOption[] = [
  { value: 'LICENCIEMENT', label: 'Licenciement (cause réelle et sérieuse)' },
  { value: 'LICENCIEMENT_ECONOMIQUE', label: 'Licenciement économique' },
  { value: 'RUPTURE_CONVENTIONNELLE', label: 'Rupture conventionnelle homologuée' },
];
const TYPES_BE: TypeRuptureOption[] = [
  { value: 'LICENCIEMENT_ORDINAIRE', label: 'Licenciement ordinaire' },
  { value: 'RUPTURE_AMIABLE', label: 'Rupture amiable' },
];

@Component({
  selector: 'app-indemnite-comparatif-section',
  standalone: true,
  imports: [
    FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatSelectModule, MatFormFieldModule,
    MatInputModule, MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './indemnite-comparatif-section.component.html',
  styleUrl: './indemnite-comparatif-section.component.scss'
})
export class IndemniteComparatifSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() aiData?: TravailExtractedData | null;
  @Input() synthesis?: CaseAnalysisResult | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;

  private synthesisSignal = signal<CaseAnalysisResult | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<IndemniteComparatifResponse | null>(null);

  country = signal('FRANCE');
  typeRupture = signal<string>('LICENCIEMENT');
  typeRuptureNote = signal<string | null>(null);
  ancienneteAnnees = signal(5);
  ancienneteMois = signal(0);
  age = signal(35);
  salaireMensuel = signal(3000);

  typeRuptureOptions = computed<TypeRuptureOption[]>(() =>
    this.country() === 'BELGIQUE' ? TYPES_BE : TYPES_FR
  );

  barMaxWidth = computed(() => {
    const r = this.result();
    if (!r) return 0;
    return r.baremePlafondMois;
  });

  coherenceAlerts = computed<Partial<Record<IndemniteAlertField, IndemniteCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<IndemniteAlertField, IndemniteCoherenceAlert>> = {};
    const typeAlert = this.buildTypeRuptureAlert();
    if (typeAlert) alerts.TYPE_RUPTURE = typeAlert;
    const ancAlert = this.buildAncienneteAlert();
    if (ancAlert) alerts.ANCIENNETE = ancAlert;
    const salAlert = this.buildSalaireAlert();
    if (salAlert) alerts.SALAIRE = salAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return {
      total: values.length,
      blockers: values.filter(a => a.level === 'blocker').length,
    };
  });

  private buildTypeRuptureAlert(): IndemniteCoherenceAlert | null {
    const userValue = this.typeRupture();
    if (!userValue) return null;

    const contributors: IndemniteAlertSource[] = [];
    let expectedValue: string | null = null;
    let f96Raison: string | null = null;
    let questionText: string | null = null;
    let questionAnswer: string | null = null;

    // B — F-96 VERIFIED avec expected_value
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'DT09_TYPE_RUPTURE') continue;
      if (chk.statut !== 'VERIFIED') continue;
      const ev = chk.expectedValue?.toUpperCase();
      if (!ev || !KNOWN_TYPE_RUPTURE_VALUES.has(ev)) continue;
      if (!expectedValue) {
        expectedValue = ev;
        f96Raison = chk.raison ?? null;
        contributors.push('F96');
      }
      break;
    }

    // C — Question IA "oui" avec expected_value
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'DT09_TYPE_RUPTURE') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ') || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue?.toUpperCase();
      if (!ev || !KNOWN_TYPE_RUPTURE_VALUES.has(ev)) continue;
      if (!expectedValue) {
        expectedValue = ev;
        questionText = q.questionText;
        questionAnswer = q.answerText ?? null;
        contributors.push('QUESTION_IA');
      } else if (ev === expectedValue) {
        questionText = q.questionText;
        questionAnswer = q.answerText ?? null;
        contributors.push('QUESTION_IA');
      }
      break;
    }

    // D — IA detection (compensationEstimate)
    const iaType = this.synthesisSignal()?.compensationEstimate?.typeRupture?.toUpperCase();
    if (iaType && KNOWN_TYPE_RUPTURE_VALUES.has(iaType)) {
      if (!expectedValue) {
        expectedValue = iaType;
        contributors.push('IA');
      } else if (iaType === expectedValue) {
        contributors.push('IA');
      }
    }

    if (!expectedValue) return null;
    if (userValue === expectedValue) return null;

    const primary: IndemniteAlertSource = contributors[0];
    return {
      field: 'TYPE_RUPTURE',
      level: 'blocker',
      source: contributors.length > 1 ? 'MULTI' : primary,
      expectedDisplay: expectedValue,
      contributors,
      f96Raison,
      questionText,
      questionAnswer,
    };
  }

  private buildAncienneteAlert(): IndemniteCoherenceAlert | null {
    const ce = this.synthesisSignal()?.compensationEstimate;
    if (!ce) return null;
    const iaYears = ce.ancienneteAnnees;
    const iaMonths = ce.ancienneteMois ?? 0;
    if (iaYears == null) return null;
    const iaTotalMois = iaYears * 12 + iaMonths;
    const userYears = this.ancienneteAnnees();
    const userMois = this.ancienneteMois();
    if (userYears <= 0 && userMois <= 0) return null;
    const userTotalMois = userYears * 12 + userMois;
    // Seuil 1 mois pour éviter les faux positifs mais rester actionnable.
    if (Math.abs(userTotalMois - iaTotalMois) < 1) return null;
    return {
      field: 'ANCIENNETE',
      level: 'warning',
      source: 'IA',
      contributors: ['IA'],
      expectedDisplay: `${iaYears} ans ${iaMonths ? iaMonths + ' mois' : ''}`.trim(),
    };
  }

  private buildSalaireAlert(): IndemniteCoherenceAlert | null {
    const ce = this.synthesisSignal()?.compensationEstimate;
    if (!ce || ce.salaireReference == null) return null;
    const ia = ce.salaireReference;
    const user = this.salaireMensuel();
    if (user <= 0) return null;
    const base = Math.max(Math.abs(ia), 1);
    if (Math.abs(user - ia) / base < 0.05) return null;
    return {
      field: 'SALAIRE',
      level: 'warning',
      source: 'IA',
      contributors: ['IA'],
      expectedDisplay: `${ia} €`,
    };
  }

  alertTooltip(alert: IndemniteCoherenceAlert): string {
    const parts: string[] = [];
    for (const src of alert.contributors) {
      if (src === 'F96') {
        parts.push(`Checklist procédurale : ${alert.expectedDisplay}${alert.f96Raison ? ' (' + alert.f96Raison + ')' : ''}`);
      } else if (src === 'QUESTION_IA') {
        parts.push(`Question complémentaire : "${alert.questionText}" → "${alert.questionAnswer}"`);
      } else if (src === 'IA') {
        parts.push(`Analyse du dossier : ${alert.expectedDisplay}`);
      }
    }
    return parts.length > 1 ? `Contredit ${parts.join(' ET ')}` : (parts[0] ?? `L'IA a détecté : ${alert.expectedDisplay}`);
  }

  alertBadgeLabel(alert: IndemniteCoherenceAlert): string {
    const prefix = (() => {
      switch (alert.source) {
        case 'F96': return 'Incohérence Checklist procédurale';
        case 'QUESTION_IA': return 'Incohérence Question complémentaire';
        case 'IA': return 'Incohérence détectée';
        case 'MULTI': return 'Incohérence multiple';
      }
    })();
    return alert.field === 'TYPE_RUPTURE' ? `${prefix} (${alert.expectedDisplay})` : prefix;
  }

  constructor(
    private comparatifService: IndemniteComparatifService,
    private snackBar: MatSnackBar,
    @Optional() private refreshService: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    this.synthesisSignal.set(this.synthesis);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.loadExisting();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['synthesis']) this.synthesisSignal.set(this.synthesis);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if ((changes['aiData'] || changes['synthesis']) && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapsed(): void {
    this.collapsed.update(v => !v);
  }

  onCountryChange(): void {
    // Reset type if incompatible with new country
    const allowed = this.typeRuptureOptions().map(o => o.value);
    if (!allowed.includes(this.typeRupture())) {
      this.typeRupture.set(allowed[0]);
    }
  }

  loadExisting(): void {
    this.loading.set(true);
    this.comparatifService.get(this.caseFileId).subscribe({
      next: resp => {
        this.result.set(resp);
        this.prefillForm(resp);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        this.prefillFromAi();
        this.showForm.set(true);
        this.loading.set(false);
      },
    });
  }

  calculate(): void {
    this.calculating.set(true);
    this.comparatifService.calculate(this.caseFileId, {
      country: this.country(),
      typeRupture: this.typeRupture(),
      ancienneteAnnees: this.ancienneteAnnees(),
      ancienneteMois: this.ancienneteMois(),
      age: this.age(),
      salaireMensuel: this.salaireMensuel(),
    }).subscribe({
      next: resp => {
        this.result.set(resp);
        this.showForm.set(false);
        this.calculating.set(false);
        this.refreshService?.triggerRefresh();
      },
      error: () => {
        this.calculating.set(false);
        this.snackBar.open('Erreur lors du calcul', 'Fermer', { duration: 4000 });
      },
    });
  }

  editForm(): void {
    const r = this.result();
    if (r) this.prefillForm(r);
    this.showForm.set(true);
  }

  barWidth(mois: number): string {
    const max = this.barMaxWidth();
    if (!max || max === 0) return '0%';
    return Math.min(100, (mois / max) * 100) + '%';
  }

  private prefillForm(resp: IndemniteComparatifResponse): void {
    this.country.set(resp.country);
    this.ancienneteAnnees.set(resp.ancienneteAnnees);
    if (resp.ancienneteMois != null) this.ancienneteMois.set(resp.ancienneteMois);
    this.age.set(resp.age);
    this.salaireMensuel.set(resp.salaireMensuel);
    if (resp.typeRupture) {
      this.typeRupture.set(resp.typeRupture);
    } else {
      // Legacy result sans type — fallback par défaut selon pays
      this.typeRupture.set(resp.country === 'BELGIQUE' ? 'LICENCIEMENT_ORDINAIRE' : 'LICENCIEMENT');
    }
  }

  private prefillFromAi(): void {
    if (this.aiData?.salaireBrutMensuel) {
      this.salaireMensuel.set(this.aiData.salaireBrutMensuel);
    }
    const ce = this.synthesis?.compensationEstimate;
    if (ce?.ancienneteAnnees != null) this.ancienneteAnnees.set(ce.ancienneteAnnees);
    if (ce?.ancienneteMois != null) this.ancienneteMois.set(ce.ancienneteMois);
    this.applyTypeRupturePrefill();
  }

  private applyTypeRupturePrefill(): void {
    const iaType = this.synthesis?.compensationEstimate?.typeRupture;
    if (!iaType) {
      this.typeRuptureNote.set(null);
      return;
    }
    const allowed = this.typeRuptureOptions().map(o => o.value);
    if (allowed.includes(iaType)) {
      this.typeRupture.set(iaType);
      this.typeRuptureNote.set(null);
    } else {
      // IA détecte autre chose (démission, prise d'acte, ou type de l'autre pays)
      this.typeRupture.set(allowed[0]);
      this.typeRuptureNote.set(
        `L'IA a détecté un type "${iaType}" non couvert par cet outil. Vérifier que le comparateur est adapté.`
      );
    }
  }
}
