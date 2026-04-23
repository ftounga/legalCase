import { Component, Input, OnInit, OnChanges, SimpleChanges, Optional, signal, computed } from '@angular/core';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData, ImmigrationTriggerEvent, PieceManquanteEntry } from '../../core/models/case-analysis.model';
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
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';

const MOTIFS_ENUM = new Set(['TRAVAIL', 'ETUDES', 'FAMILLE', 'ASILE', 'AUTRE']);

export type IM05AlertField = 'MOTIF' | 'NATIONALITE_UE';
export type IM05AlertSource = 'F96' | 'QUESTION_IA' | 'IA' | 'PIECE_MANQUANTE' | 'MULTI';

export interface IM05CoherenceAlert {
  field: IM05AlertField;
  source: IM05AlertSource;
  contributors: IM05AlertSource[];
  expectedDisplay: string;
  reason: string;
  pieceTexte?: string | null;
}

const CODE_TO_MOTIF: Record<string, string> = {
  VLS_TS_ETUDIANT: 'ETUDES',
  CARTE_A_ETUDES: 'ETUDES',
  CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE: 'ETUDES',
  VLS_TS_SALARIE: 'TRAVAIL',
  CST_SALARIE: 'TRAVAIL',
  CARTE_PLURIANNUELLE: 'TRAVAIL',
  CARTE_PLURIANNUELLE_SALARIE: 'TRAVAIL',
  CARTE_PLURIANNUELLE_PASSEPORT_TALENT: 'TRAVAIL',
  APS: 'TRAVAIL',
  CARTE_A_TRAVAIL: 'TRAVAIL',
  PERMIS_UNIQUE: 'TRAVAIL',
  CST_VPF: 'FAMILLE',
  CST_VPF_CONJOINT_FR: 'FAMILLE',
  CARTE_PLURIANNUELLE_VPF: 'FAMILLE',
  CARTE_A_FAMILLE: 'FAMILLE',
  RECEPISSE_ASILE: 'ASILE',
  ATTESTATION_IMMATRICULATION: 'ASILE',
  ANNEXE_15: 'ASILE',
  // CARTE_RESIDENT, CARTE_B, CARTE_C : titres génériques stables, pas de mapping motif
};

/**
 * SF-IM-05-04 : mapping trigger_event → (motif, situationFamiliale).
 * Prioritaire sur CODE_TO_MOTIF car décrit la voie JURIDIQUE CIBLE (ex: mariage
 * avec Français → FAMILLE/MARIE), tandis que le code titre reflète la situation
 * actuelle (ex: pluriannuelle Étudiant-Recherche → ETUDES).
 */
const TRIGGER_TO_CRITERIA: Record<string, { motif: string; situationFamiliale?: string }> = {
  MARIAGE_RESSORTISSANT_FR: { motif: 'FAMILLE', situationFamiliale: 'MARIE' },
  PACS_RESSORTISSANT_FR: { motif: 'FAMILLE', situationFamiliale: 'PACS_COHABITATION' },
  NAISSANCE_ENFANT_FR: { motif: 'FAMILLE' },
  REGROUPEMENT_FAMILIAL_AUTORISE: { motif: 'FAMILLE' },
  VIOLENCES_CONJUGALES_CONSTATEES: { motif: 'FAMILLE' },
  CDI_OBTENU_SALARIE: { motif: 'TRAVAIL' },
  DOCTORAT_OBTENU: { motif: 'TRAVAIL' }, // chercheur/post-doc → Passeport Talent
  DEMANDE_ASILE_ACCORDEE_OFPRA: { motif: 'ASILE' },
  ENFANT_NE_FR_13ANS_PRESENCE: { motif: 'FAMILLE' },
  // ENTREE_LEGALE_10ANS : pas de motif unique, peut être TRAVAIL ou FAMILLE → pas de mapping
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
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './immigration-title-decision-section.component.html',
  styleUrl: './immigration-title-decision-section.component.scss'
})
export class ImmigrationTitleDecisionSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() aiData?: ImmigrationExtractedData | null;
  /** SF-IM-05-04 : événements déclencheurs F-150 — priorité sur aiData pour déduire motif+situation. */
  @Input() triggerEvents?: ImmigrationTriggerEvent[] | null;
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
  decision = signal<TitleDecisionResponse | null>(null);

  country = signal('FRANCE');
  nationaliteUe = signal(false);
  motif = signal('TRAVAIL');
  duree = signal('LONG_SEJOUR');
  situationFamiliale = signal<string | null>(null);

  // SF-IM-05-04 : provenance IA par champ (badge "Pré-rempli depuis l'analyse").
  // Effacé dès que l'avocat modifie manuellement un champ (onXxxChange).
  provenanceMotif = signal<'IA' | null>(null);
  provenanceSituationFamiliale = signal<'IA' | null>(null);
  provenanceNationaliteUe = signal<'IA' | null>(null);

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

  // SF-IA-03-15c — map {sourceKey → explanation}
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  constructor(
    private decisionService: ImmigrationTitleDecisionService,
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

  /** SF-IA-03-15c : mapping vers sourceKey (F96 pour MOTIF, générique pour NATIONALITE_UE). */
  explanationFor(field: IM05AlertField): SourceExplanation[] {
    const key = field === 'MOTIF' ? 'IM05_MOTIF' : 'nationalite_ue';
    return this.sourceExplanations().get(key) ?? [];
  }

  coherenceAlerts = computed<Partial<Record<IM05AlertField, IM05CoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
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
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    // SF-IM-05-04 : garde-fou — pré-remplit au mount si aiData/triggerEvents
    // sont déjà disponibles (cas : pipeline déjà tourné avant ouverture du dossier).
    this.prefillFromAi();
    this.loadExisting();
    this.loadSourceExplanations();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    // SF-IM-05-04 : ré-applique le prefill dès que aiData OU triggerEvents change
    // (les 2 sont maintenant pris en compte — les triggers sont prioritaires).
    if ((changes['aiData'] || changes['triggerEvents']) && this.showForm() && !this.decision()) {
      this.prefillFromAi();
    }
  }

  private buildPiecesIndex(pieces: PieceManquanteEntry[]): Record<string, string> {
    const index: Record<string, string> = {};
    if (!pieces) return index;
    for (const p of pieces) {
      const code = p.critereCode?.toUpperCase();
      if (!code) continue;
      if (code !== 'IM05_MOTIF') continue;
      if (!index[code]) index[code] = p.texte;
    }
    return index;
  }

  alertTooltip(alert: IM05CoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: IM05CoherenceAlert): string {
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

  private buildMotifAlert(): IM05CoherenceAlert | null {
    const user = this.motif();
    if (!user) return null;

    const piecesIndex = this.buildPiecesIndex(this.piecesManquantesSignal());
    const pieceTexte = piecesIndex['IM05_MOTIF'] ?? null;
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
        reasons.push(`Question complémentaire : "${q.questionText}" → "${q.answerText}"`);
      } else if (ev === expected) {
        contributors.push('QUESTION_IA');
        reasons.push(`Question complémentaire : "${q.questionText}" → "${q.answerText}"`);
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
          reasons.push(`Analyse du dossier : code ${iaCode} → motif ${iaMotif}`);
        } else if (iaMotif === expected) {
          contributors.push('IA');
          reasons.push(`Analyse du dossier : ${iaMotif}`);
        }
      }
    }

    if (!expected) return null;
    if (pieceTexte) {
      contributors.push('PIECE_MANQUANTE');
      reasons.push(`Pièce manquante : ${pieceTexte}`);
    }
    return {
      field: 'MOTIF',
      source: contributors.length > 1 ? 'MULTI' : contributors[0],
      contributors,
      expectedDisplay: expected,
      reason: reasons.join(' ET '),
      pieceTexte,
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
      reason: `Analyse du dossier : ${iaNat ? 'nationalité UE/EEE/Suisse' : 'pays tiers'}`,
    };
  }

  toggleCollapsed(): void {
    this.collapsed.update(v => !v);
  }

  onMotifChange(): void {
    this.provenanceMotif.set(null);
    if (this.motif() !== 'FAMILLE') {
      this.situationFamiliale.set(null);
      this.provenanceSituationFamiliale.set(null);
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
    if (!this.aiData && !this.triggerEvents?.length) return;

    // 1. Nationalité UE depuis l'IA
    if (this.aiData && typeof this.aiData.nationaliteUe === 'boolean') {
      this.nationaliteUe.set(this.aiData.nationaliteUe);
      this.provenanceNationaliteUe.set('IA');
    }

    // 2. Motif + situationFamiliale : SF-IM-05-04 — priorité aux trigger_events
    //    (décrivent la voie juridique cible : mariage → FAMILLE/MARIE), puis
    //    fallback sur le code titre actuel, puis heuristique texte libre.
    const firstTrigger = this.triggerEvents?.[0]?.eventCode;
    if (firstTrigger && TRIGGER_TO_CRITERIA[firstTrigger]) {
      const criteria = TRIGGER_TO_CRITERIA[firstTrigger];
      this.motif.set(criteria.motif);
      this.provenanceMotif.set('IA');
      if (criteria.situationFamiliale) {
        this.situationFamiliale.set(criteria.situationFamiliale);
        this.provenanceSituationFamiliale.set('IA');
      }
      return;
    }

    if (!this.aiData) return;

    const code = this.aiData.typeTitreSejourCode?.toUpperCase();
    if (code && CODE_TO_MOTIF[code]) {
      this.motif.set(CODE_TO_MOTIF[code]);
      this.provenanceMotif.set('IA');
      return;
    }
    if (this.aiData.typeTitreSejour) {
      const type = this.aiData.typeTitreSejour
        .toUpperCase()
        .normalize('NFD').replace(/[\u0300-\u036f]/g, ''); // strip accents
      let detected: string | null = null;
      if (type.includes('ETUDIANT') || type.includes('STUDENT')) detected = 'ETUDES';
      else if (type.includes('SALARIE') || type.includes('TRAVAIL')) detected = 'TRAVAIL';
      else if (type.includes('FAMILLE') || type.includes('VPF')) detected = 'FAMILLE';
      else if (type.includes('ASILE') || type.includes('REFUGIE')) detected = 'ASILE';
      if (detected) {
        this.motif.set(detected);
        this.provenanceMotif.set('IA');
      }
    }
  }

  /** SF-IM-05-04 : effacer les badges IA quand l'avocat modifie manuellement. */
  onSituationFamilialeChange(): void {
    this.provenanceSituationFamiliale.set(null);
  }

  onNationaliteUeChange(): void {
    this.provenanceNationaliteUe.set(null);
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
        this.refreshService?.triggerRefresh();
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
