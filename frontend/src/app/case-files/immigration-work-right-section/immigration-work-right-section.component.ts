import { Component, Input, OnInit, OnChanges, SimpleChanges, Optional, signal, computed } from '@angular/core';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { ImmigrationWorkRightService } from '../../core/services/immigration-work-right.service';
import { WorkRightResponse } from '../../core/models/immigration-work-right.model';
import { ImmigrationExtractedData, PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';

const FR_TITRE_CODES = new Set([
  'VLS_TS_ETUDIANT', 'VLS_TS_SALARIE', 'CST_SALARIE', 'CARTE_PLURIANNUELLE',
  // SF-IM-07-04 : sous-types explicites de la carte pluriannuelle (droit au
  // travail différent selon le motif) + code conjoint de Français L.423-1.
  'CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE', 'CARTE_PLURIANNUELLE_SALARIE',
  'CARTE_PLURIANNUELLE_PASSEPORT_TALENT', 'CARTE_PLURIANNUELLE_VPF',
  'CARTE_RESIDENT', 'APS', 'CST_VPF', 'CST_VPF_CONJOINT_FR', 'RECEPISSE_ASILE',
]);
const BE_TITRE_CODES = new Set([
  'CARTE_A_TRAVAIL', 'CARTE_A_ETUDES', 'CARTE_A_FAMILLE', 'CARTE_B', 'CARTE_C',
  'PERMIS_UNIQUE', 'ANNEXE_15', 'ATTESTATION_IMMATRICULATION',
]);
const ALL_TITRE_CODES = new Set([...FR_TITRE_CODES, ...BE_TITRE_CODES]);

export type IM07AlertSource = 'F96' | 'QUESTION_IA' | 'IA' | 'PIECE_MANQUANTE' | 'MULTI';

export interface IM07CoherenceAlert {
  source: IM07AlertSource;
  contributors: IM07AlertSource[];
  expectedDisplay: string;
  reason: string;
  pieceTexte?: string | null;
}

@Component({
  selector: 'app-immigration-work-right-section',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule, MatIconModule,
    MatSelectModule, MatFormFieldModule, MatInputModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './immigration-work-right-section.component.html',
  styleUrl: './immigration-work-right-section.component.scss'
})
export class ImmigrationWorkRightSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() workspaceCountry: string = 'FRANCE';
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  private aiDataSignal = signal<ImmigrationExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  resolving = signal(false);
  showForm = signal(true);
  result = signal<WorkRightResponse | null>(null);

  titreType = signal('VLS_TS_SALARIE');
  country = signal('FRANCE');
  provenanceTitreType = signal<'IA' | null>(null);

  readonly titresFrance = [
    { value: 'VLS_TS_ETUDIANT', label: 'VLS-TS Étudiant' },
    { value: 'VLS_TS_SALARIE', label: 'VLS-TS Salarié' },
    { value: 'CST_SALARIE', label: 'CST Salarié' },
    // SF-IM-07-04 : le générique reste pour rétro-compat mais renvoie un
    // message CONDITIONNEL invitant à choisir un sous-type.
    { value: 'CARTE_PLURIANNUELLE', label: 'Carte pluriannuelle (générique — préciser le motif)' },
    { value: 'CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE', label: 'Carte pluriannuelle — Étudiant / Étudiant-Recherche' },
    { value: 'CARTE_PLURIANNUELLE_SALARIE', label: 'Carte pluriannuelle — Salarié' },
    { value: 'CARTE_PLURIANNUELLE_PASSEPORT_TALENT', label: 'Carte pluriannuelle — Passeport Talent' },
    { value: 'CARTE_PLURIANNUELLE_VPF', label: 'Carte pluriannuelle — Vie privée et familiale' },
    { value: 'CARTE_RESIDENT', label: 'Carte de résident' },
    { value: 'APS', label: 'APS' },
    { value: 'CST_VPF', label: 'Carte vie privée et familiale (générique)' },
    { value: 'CST_VPF_CONJOINT_FR', label: 'CST VPF — Conjoint de Français (L.423-1)' },
    { value: 'RECEPISSE_ASILE', label: 'Récépissé demande d\'asile' },
  ];

  readonly titresBelgique = [
    { value: 'CARTE_A_TRAVAIL', label: 'Carte A — Travail' },
    { value: 'CARTE_A_ETUDES', label: 'Carte A — Études' },
    { value: 'CARTE_A_FAMILLE', label: 'Carte A — Famille' },
    { value: 'CARTE_B', label: 'Carte B' },
    { value: 'CARTE_C', label: 'Carte C' },
    { value: 'PERMIS_UNIQUE', label: 'Permis unique' },
    { value: 'ANNEXE_15', label: 'Annexe 15' },
    { value: 'ATTESTATION_IMMATRICULATION', label: 'Attestation d\'immatriculation' },
  ];

  get titresForCountry() {
    return this.country() === 'BELGIQUE' ? this.titresBelgique : this.titresFrance;
  }

  coherenceAlert = computed<IM07CoherenceAlert | null>(() => {
    if (!this.showForm()) return null;
    const user = this.titreType();
    if (!user) return null;

    const piecesIndex = this.buildPiecesIndex(this.piecesManquantesSignal());
    const pieceTexte = piecesIndex['IM07_TITRE_TYPE'] ?? null;
    const contributors: IM07AlertSource[] = [];
    const reasons: string[] = [];
    let expected: string | null = null;

    // F-96 VERIFIED
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'IM07_TITRE_TYPE') continue;
      if (chk.statut !== 'VERIFIED') continue;
      const ev = chk.expectedValue?.toUpperCase();
      if (!ev || !ALL_TITRE_CODES.has(ev)) continue;
      if (ev !== user && !expected) {
        expected = ev;
        contributors.push('F96');
        reasons.push(`Checklist procédurale : ${ev}${chk.raison ? ' (' + chk.raison + ')' : ''}`);
      }
      break;
    }

    // Question IA "oui"
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'IM07_TITRE_TYPE') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ') || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue?.toUpperCase();
      if (!ev || !ALL_TITRE_CODES.has(ev)) continue;
      if (ev === user) continue;
      if (!expected) {
        expected = ev;
        contributors.push('QUESTION_IA');
        reasons.push(`Question complémentaire : "${q.questionText}" → "${q.answerText}"`);
      } else if (ev === expected) {
        contributors.push('QUESTION_IA');
        reasons.push(`Question complémentaire : "${q.questionText}" → "${q.answerText}"`);
      }
      break;
    }

    // IA typeTitreSejourCode
    const iaCode = this.aiDataSignal()?.typeTitreSejourCode?.toUpperCase();
    if (iaCode && ALL_TITRE_CODES.has(iaCode) && iaCode !== user) {
      if (!expected) {
        expected = iaCode;
        contributors.push('IA');
        reasons.push(`Analyse du dossier : ${iaCode}`);
      } else if (iaCode === expected) {
        contributors.push('IA');
        reasons.push(`Analyse du dossier : ${iaCode}`);
      }
    }

    if (!expected) return null;
    if (pieceTexte) {
      contributors.push('PIECE_MANQUANTE');
      reasons.push(`Pièce manquante : ${pieceTexte}`);
    }
    return {
      source: contributors.length > 1 ? 'MULTI' : contributors[0],
      contributors,
      expectedDisplay: expected,
      reason: reasons.join(' ET '),
      pieceTexte,
    };
  });

  alertsSummary = computed(() => ({ total: this.coherenceAlert() ? 1 : 0, blockers: 0 }));

  // SF-IA-03-15c — map {sourceKey → explanation}
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  constructor(
    private workRightService: ImmigrationWorkRightService,
    private sourceExplanationService: SourceExplanationService,
    private snackBar: MatSnackBar,
    @Optional() private refreshService: CaseDashboardRefreshService | null,
  ) {}

  private loadSourceExplanations(): void {
    if (!this.caseFileId) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: map => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  explanationFor(): SourceExplanation[] {
    return this.sourceExplanations().get('IM07_TITRE_TYPE') ?? [];
  }

  ngOnInit(): void {
    this.country.set(this.workspaceCountry);
    if (this.workspaceCountry === 'BELGIQUE') {
      this.titreType.set(this.titresBelgique[0].value);
    }
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    // SF-IM-07-05 : garde-fou — si aiData est présent au mount sans qu'un
    // ngOnChanges supplémentaire ne se déclenche (pipeline déjà tourné),
    // pré-remplit le sélecteur depuis la détection IA. ngOnChanges fait le
    // même travail pour les mises à jour ultérieures (nouvelle analyse).
    this.prefillFromAi();
    this.loadSourceExplanations();
    this.loadExisting();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    if (changes['aiData'] && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  private buildPiecesIndex(pieces: PieceManquanteEntry[]): Record<string, string> {
    const index: Record<string, string> = {};
    if (!pieces) return index;
    for (const p of pieces) {
      const code = p.critereCode?.toUpperCase();
      if (!code) continue;
      if (code !== 'IM07_TITRE_TYPE') continue;
      if (!index[code]) index[code] = p.texte;
    }
    return index;
  }

  toggleCollapsed(): void {
    this.collapsed.update(v => !v);
  }

  onTitreTypeChange(): void { this.provenanceTitreType.set(null); }

  private prefillFromAi(): void {
    if (!this.aiData) return;
    const code = this.aiData.typeTitreSejourCode?.toUpperCase();
    if (!code) return;
    const isFR = FR_TITRE_CODES.has(code);
    const isBE = BE_TITRE_CODES.has(code);
    if (!isFR && !isBE) return;
    // Only prefill if code is compatible with current workspace country
    if ((this.country() === 'FRANCE' && isFR) || (this.country() === 'BELGIQUE' && isBE)) {
      this.titreType.set(code);
      this.provenanceTitreType.set('IA');
    }
  }

  alertTooltip(alert: IM07CoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: IM07CoherenceAlert): string {
    const prefix = (() => {
      switch (alert.source) {
        case 'F96': return 'Incohérence Checklist procédurale';
        case 'QUESTION_IA': return 'Incohérence Question complémentaire';
        case 'IA': return 'Incohérence détectée';
        case 'PIECE_MANQUANTE': return 'Pièce manquante';
        case 'MULTI': return 'Incohérence multiple';
      }
    })();
    return `${prefix} (${alert.expectedDisplay})`;
  }

  loadExisting(): void {
    this.loading.set(true);
    this.workRightService.get(this.caseFileId).subscribe({
      next: resp => {
        this.result.set(resp);
        this.country.set(resp.country);
        this.titreType.set(resp.titreType);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        this.showForm.set(true);
        this.loading.set(false);
        this.prefillFromAi();
      },
    });
  }

  resolve(): void {
    this.resolving.set(true);
    this.workRightService.resolve(this.caseFileId, {
      titreType: this.titreType(),
      country: this.country(),
    }).subscribe({
      next: resp => {
        this.result.set(resp);
        this.showForm.set(false);
        this.resolving.set(false);
        this.refreshService?.triggerRefresh();
      },
      error: () => {
        this.resolving.set(false);
        this.snackBar.open('Erreur lors de l\'analyse', 'Fermer', { duration: 4000 });
      },
    });
  }

  editForm(): void {
    const r = this.result();
    if (r) {
      this.country.set(r.country);
      this.titreType.set(r.titreType);
    }
    this.showForm.set(true);
  }
}
