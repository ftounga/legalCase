import { Component, Input, OnInit, OnChanges, SimpleChanges, signal, computed } from '@angular/core';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { FormsModule } from '@angular/forms';
import { ImmigrationTitleDecisionService } from '../../core/services/immigration-title-decision.service';
import { TitleDecisionResponse, TitleRecommendation } from '../../core/models/immigration-title-decision.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';

const MOTIFS_ENUM = new Set(['TRAVAIL', 'ETUDES', 'FAMILLE', 'ASILE', 'AUTRE']);

export type IM05AlertField = 'MOTIF' | 'NATIONALITE_UE';
export type IM05AlertSource = 'F96' | 'QUESTION_IA' | 'IA' | 'MULTI';

export interface IM05CoherenceAlert {
  field: IM05AlertField;
  source: IM05AlertSource;
  contributors: IM05AlertSource[];
  expectedDisplay: string;
  reason: string;
}

const CODE_TO_MOTIF: Record<string, string> = {
  VLS_TS_ETUDIANT: 'ETUDES',
  CARTE_A_ETUDES: 'ETUDES',
  VLS_TS_SALARIE: 'TRAVAIL',
  CST_SALARIE: 'TRAVAIL',
  CARTE_PLURIANNUELLE: 'TRAVAIL',
  APS: 'TRAVAIL',
  CARTE_A_TRAVAIL: 'TRAVAIL',
  PERMIS_UNIQUE: 'TRAVAIL',
  CST_VPF: 'FAMILLE',
  CARTE_A_FAMILLE: 'FAMILLE',
  RECEPISSE_ASILE: 'ASILE',
  ATTESTATION_IMMATRICULATION: 'ASILE',
  ANNEXE_15: 'ASILE',
  // CARTE_RESIDENT, CARTE_B, CARTE_C : titres génériques stables, pas de mapping motif
};

@Component({
  selector: 'app-immigration-title-decision-section',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule, MatIconModule,
    MatSelectModule, MatFormFieldModule,
    MatProgressSpinnerModule, MatSlideToggleModule,
    MatTooltipModule,
  ],
  templateUrl: './immigration-title-decision-section.component.html',
  styleUrl: './immigration-title-decision-section.component.scss'
})
export class ImmigrationTitleDecisionSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;

  private aiDataSignal = signal<ImmigrationExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  resolving = signal(false);
  showForm = signal(true);
  decision = signal<TitleDecisionResponse | null>(null);

  country = signal('FRANCE');
  nationaliteUe = signal(false);
  motif = signal('TRAVAIL');
  duree = signal('LONG_SEJOUR');
  situationFamiliale = signal<string | null>(null);

  readonly countries = [
    { value: 'FRANCE', label: 'France' },
    { value: 'BELGIQUE', label: 'Belgique' },
  ];

  readonly motifs = [
    { value: 'TRAVAIL', label: 'Travail' },
    { value: 'ETUDES', label: 'Études' },
    { value: 'FAMILLE', label: 'Famille' },
    { value: 'ASILE', label: 'Asile' },
    { value: 'AUTRE', label: 'Autre' },
  ];

  readonly durees = [
    { value: 'COURT_SEJOUR', label: 'Court séjour (< 1 an)' },
    { value: 'LONG_SEJOUR', label: 'Long séjour (≥ 1 an)' },
  ];

  readonly situations = [
    { value: 'CELIBATAIRE', label: 'Célibataire' },
    { value: 'MARIE', label: 'Marié(e)' },
    { value: 'PACS_COHABITATION', label: 'PACS / Cohabitation légale' },
  ];

  constructor(
    private decisionService: ImmigrationTitleDecisionService,
    private snackBar: MatSnackBar,
  ) {}

  coherenceAlerts = computed<Partial<Record<IM05AlertField, IM05CoherenceAlert>>>(() => {
    if (!this.showForm() || this.decision()) return {};
    const alerts: Partial<Record<IM05AlertField, IM05CoherenceAlert>> = {};
    const motifAlert = this.buildMotifAlert();
    if (motifAlert) alerts.MOTIF = motifAlert;
    const natAlert = this.buildNationaliteAlert();
    if (natAlert) alerts.NATIONALITE_UE = natAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  ngOnInit(): void {
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.loadExisting();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['aiData'] && this.showForm() && !this.decision()) {
      this.prefillFromAi();
    }
  }

  alertTooltip(alert: IM05CoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: IM05CoherenceAlert): string {
    const prefix = (() => {
      switch (alert.source) {
        case 'F96': return 'Incohérence F-96';
        case 'QUESTION_IA': return 'Incohérence Question IA';
        case 'IA': return 'Incohérence IA';
        case 'MULTI': return 'Incohérence multiple';
      }
    })();
    return `${prefix} (${alert.expectedDisplay})`;
  }

  private buildMotifAlert(): IM05CoherenceAlert | null {
    const user = this.motif();
    if (!user) return null;

    const contributors: IM05AlertSource[] = [];
    const reasons: string[] = [];
    let expected: string | null = null;

    // F-96 VERIFIED
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'IM05_MOTIF') continue;
      if (chk.statut !== 'VERIFIED') continue;
      const ev = chk.expectedValue?.toUpperCase();
      if (!ev || !MOTIFS_ENUM.has(ev)) continue;
      if (ev !== user && !expected) {
        expected = ev;
        contributors.push('F96');
        reasons.push(`Checklist procédurale : motif ${ev}${chk.raison ? ' (' + chk.raison + ')' : ''}`);
      }
      break;
    }

    // Question IA "oui"
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'IM05_MOTIF') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ') || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue?.toUpperCase();
      if (!ev || !MOTIFS_ENUM.has(ev)) continue;
      if (ev === user) continue;
      if (!expected) {
        expected = ev;
        contributors.push('QUESTION_IA');
        reasons.push(`Question IA : "${q.questionText}" → "${q.answerText}"`);
      } else if (ev === expected) {
        contributors.push('QUESTION_IA');
        reasons.push(`Question IA : "${q.questionText}" → "${q.answerText}"`);
      }
      break;
    }

    // IA typeTitreSejourCode via CODE_TO_MOTIF
    const iaCode = this.aiDataSignal()?.typeTitreSejourCode?.toUpperCase();
    if (iaCode && CODE_TO_MOTIF[iaCode]) {
      const iaMotif = CODE_TO_MOTIF[iaCode];
      if (iaMotif !== user) {
        if (!expected) {
          expected = iaMotif;
          contributors.push('IA');
          reasons.push(`Analyse IA : code ${iaCode} → motif ${iaMotif}`);
        } else if (iaMotif === expected) {
          contributors.push('IA');
          reasons.push(`Analyse IA : ${iaMotif}`);
        }
      }
    }

    if (!expected) return null;
    return {
      field: 'MOTIF',
      source: contributors.length > 1 ? 'MULTI' : contributors[0],
      contributors,
      expectedDisplay: expected,
      reason: reasons.join(' ET '),
    };
  }

  private buildNationaliteAlert(): IM05CoherenceAlert | null {
    const iaNat = this.aiDataSignal()?.nationaliteUe;
    if (typeof iaNat !== 'boolean') return null;
    if (iaNat === this.nationaliteUe()) return null;
    return {
      field: 'NATIONALITE_UE',
      source: 'IA',
      contributors: ['IA'],
      expectedDisplay: iaNat ? 'Ressortissant UE' : 'Pays tiers',
      reason: `Analyse IA : ${iaNat ? 'nationalité UE/EEE/Suisse' : 'pays tiers'}`,
    };
  }

  toggleCollapsed(): void {
    this.collapsed.update(v => !v);
  }

  onMotifChange(): void {
    if (this.motif() !== 'FAMILLE') {
      this.situationFamiliale.set(null);
    }
  }

  loadExisting(): void {
    this.loading.set(true);
    this.decisionService.get(this.caseFileId).subscribe({
      next: resp => {
        this.decision.set(resp);
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

  private prefillFromAi(): void {
    if (!this.aiData) return;

    // 1. Nationalité UE depuis l'IA
    if (typeof this.aiData.nationaliteUe === 'boolean') {
      this.nationaliteUe.set(this.aiData.nationaliteUe);
    }

    // 2. Motif : priorité au code normalisé, fallback heuristique texte libre
    const code = this.aiData.typeTitreSejourCode?.toUpperCase();
    if (code && CODE_TO_MOTIF[code]) {
      this.motif.set(CODE_TO_MOTIF[code]);
      return;
    }
    if (this.aiData.typeTitreSejour) {
      const type = this.aiData.typeTitreSejour
        .toUpperCase()
        .normalize('NFD').replace(/[\u0300-\u036f]/g, ''); // strip accents
      if (type.includes('ETUDIANT') || type.includes('STUDENT')) this.motif.set('ETUDES');
      else if (type.includes('SALARIE') || type.includes('TRAVAIL')) this.motif.set('TRAVAIL');
      else if (type.includes('FAMILLE') || type.includes('VPF')) this.motif.set('FAMILLE');
      else if (type.includes('ASILE') || type.includes('REFUGIE')) this.motif.set('ASILE');
    }
  }

  resolve(): void {
    this.resolving.set(true);
    this.decisionService.resolve(this.caseFileId, {
      country: this.country(),
      nationaliteUe: this.nationaliteUe(),
      motif: this.motif(),
      duree: this.duree(),
      situationFamiliale: this.motif() === 'FAMILLE' ? this.situationFamiliale() : null,
    }).subscribe({
      next: resp => {
        this.decision.set(resp);
        this.showForm.set(false);
        this.resolving.set(false);
      },
      error: () => {
        this.resolving.set(false);
        this.snackBar.open('Erreur lors de l\'analyse', 'Fermer', { duration: 4000 });
      },
    });
  }

  editCriteria(): void {
    const d = this.decision();
    if (d) this.prefillForm(d);
    this.showForm.set(true);
  }

  private prefillForm(resp: TitleDecisionResponse): void {
    this.country.set(resp.country);
    this.nationaliteUe.set(resp.nationaliteUe);
    this.motif.set(resp.motif);
    this.duree.set(resp.duree);
    this.situationFamiliale.set(resp.situationFamiliale);
  }
}
