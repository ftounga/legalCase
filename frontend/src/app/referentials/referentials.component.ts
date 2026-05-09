import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ReferentialService } from '../core/services/referential.service';
import { WorkspaceService } from '../core/services/workspace.service';
import { WorkspaceMemberService } from '../core/services/workspace-member.service';
import { AuthService } from '../core/services/auth.service';
import { ReferentialEntry, ReferentialResponse } from '../core/models/referential.model';
import { fadeInUp } from '../shared/animations';
import {
  ReferentialEditDialogComponent,
  ReferentialEditDialogData,
  ReferentialEditDialogResult,
} from './referential-edit-dialog/referential-edit-dialog.component';
import {
  ReferentialReportDialogComponent,
  ReferentialReportDialogData,
  ReferentialReportDialogResult,
} from './referential-report-dialog/referential-report-dialog.component';
import {
  ReferentialWarningDialogComponent,
  ReferentialWarningDialogData,
} from './referential-warning-dialog/referential-warning-dialog.component';
import {
  ReferentialHelpDialogComponent,
  ReferentialHelpDialogData,
} from './referential-help-dialog/referential-help-dialog.component';
import { SECTION_DOCS } from './section-docs';

interface EntryWithAlert extends ReferentialEntry {
  alertId?: string;
  alertProposedValueJson?: string;
  alertAiMessage?: string | null;
}

interface SectionDisplay {
  type: string;
  title: string;
  entries: EntryWithAlert[];
}

const SECTION_LABELS: Record<string, string> = {
  LITIGATION_TYPE:        'Types de litiges',
  BAREME_MACRON:          'Barème Macron',
  IMMIGRATION_JALONS:     'Jalons procéduraux',
  IMMIGRATION_PIECES:     'Pièces requises',
  PENSION_TAUX:           'Barème pension alimentaire',
  PRESTATION_COEFF:       'Prestation compensatoire',
  IMMIGRATION_TITLES:     'Titres de séjour',
  IMMIGRATION_RECOURS:    'Types de recours',
  IMMIGRATION_WORK_RIGHTS:'Droits au travail',
  CONVENTION_BAREMES:     'Conventions collectives',
  LICENCIEMENT_CRITERES:  'Critères de licenciement',
  RUPTURE_CONV_CRITERES:  'Critères de rupture conventionnelle',
  INDEMNITE_BAREMES:      'Barèmes indemnités',
  GARDE_MODES:            'Modes de garde',
  DIVORCE_ETAPES:         'Étapes divorce',
  DIVORCE_PIECES:         'Pièces divorce',
  // SF-225-01 : 5 types orphelins UX
  CONVENTION_PREAVIS:           'Convention collective — Préavis',
  TRAVAIL_PROCEDURE_JALONS:     'Droit du travail — Jalons procéduraux',
  FAMILLE_PROCEDURE_JALONS:     'Droit de la famille — Jalons procéduraux',
  MAJEURS_PROTEGES_REGIMES:     'Majeurs protégés — Régimes',
  IM21_VALIDITY_CRITERES:       'Immigration — Critères de validité (F-IM-21)',
};

@Component({
  selector: 'app-referentials',
  standalone: true,
  imports: [
    FormsModule,
    MatExpansionModule, MatIconModule, MatProgressSpinnerModule,
    MatChipsModule, MatButtonModule, MatTooltipModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonToggleModule,
  ],
  templateUrl: './referentials.component.html',
  styleUrl: './referentials.component.scss',
  animations: [fadeInUp],
  host: { '[@fadeInUp]': '' },
})
export class ReferentialsComponent implements OnInit {
  loading = signal(true);
  error = signal(false);
  sections = signal<SectionDisplay[]>([]);
  domainLabel = signal('');
  canEdit = signal(false);
  canReport = signal(false);
  pendingAlertsCount = signal(0);

  // SF-137-02/03 : filtres UX
  searchQuery = signal('');
  typeFilter = signal<string>('');           // vide = tous les types
  scopeFilter = signal<'ALL' | 'SYSTEM' | 'CUSTOM'>('ALL');

  /** SF-137-02/03 : sections affichables après application des filtres. */
  filteredSections = computed<SectionDisplay[]>(() => {
    const q = this.searchQuery().trim().toLowerCase();
    const type = this.typeFilter();
    const scope = this.scopeFilter();
    return this.sections()
        .filter(s => !type || s.type === type)
        .map(s => ({
          ...s,
          entries: s.entries.filter(e => this.entryMatches(e, q, scope)),
        }))
        .filter(s => s.entries.length > 0);
  });

  /** SF-137-04 : compteur global pour feedback utilisateur. */
  filteredEntryCount = computed(() =>
      this.filteredSections().reduce((acc, s) => acc + s.entries.length, 0));
  totalEntryCount = computed(() =>
      this.sections().reduce((acc, s) => acc + s.entries.length, 0));

  availableTypes = computed<{ type: string; label: string }[]>(() =>
      this.sections().map(s => ({ type: s.type, label: s.title })));

  private entryMatches(e: EntryWithAlert, q: string, scope: 'ALL' | 'SYSTEM' | 'CUSTOM'): boolean {
    if (scope === 'SYSTEM' && !e.isSystem) return false;
    if (scope === 'CUSTOM' && e.isSystem) return false;
    if (!q) return true;
    const haystack = [e.key, e.label, e.sourceRef ?? '']
        .join(' ')
        .toLowerCase();
    return haystack.includes(q);
  }

  clearFilters(): void {
    this.searchQuery.set('');
    this.typeFilter.set('');
    this.scopeFilter.set('ALL');
  }

  hasActiveFilters(): boolean {
    return this.searchQuery().trim() !== ''
        || this.typeFilter() !== ''
        || this.scopeFilter() !== 'ALL';
  }

  private domain = '';
  private alertsById = new Map<string, { alertId: string; proposedValueJson: string; aiMessage?: string | null }>();

  constructor(
    private referentialService: ReferentialService,
    private workspaceService: WorkspaceService,
    private workspaceMemberService: WorkspaceMemberService,
    private authService: AuthService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.workspaceService.getCurrentWorkspace().subscribe({
      next: ws => {
        const domain = ws.legalDomain ?? '';
        this.domain = domain;
        this.domainLabel.set(this.formatDomain(domain));

        // Check role
        this.workspaceMemberService.getMembers().subscribe({
          next: members => {
            const currentUserId = this.authService.currentUser()?.id;
            const me = members.find(m => m.userId === currentUserId);
            this.canEdit.set(me?.memberRole === 'OWNER' || me?.memberRole === 'ADMIN');
            this.canReport.set(me?.memberRole === 'MEMBER');
          }
        });

        if (!domain) {
          this.loading.set(false);
          return;
        }
        this.loadReferentials();
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      }
    });
  }

  private loadReferentials(): void {
    this.referentialService.getReferentials(this.domain).subscribe({
      next: (response: ReferentialResponse) => {
        this.sections.set(this.buildSections(response));
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      }
    });
    this.referentialService.getPendingAlertsCount().subscribe({
      next: res => this.pendingAlertsCount.set(res.count),
      error: () => {}
    });
  }

  applyAlert(entry: EntryWithAlert): void {
    if (!entry.alertId) return;
    this.referentialService.applyAlert(entry.alertId).subscribe({
      next: () => {
        this.snackBar.open('Valeur mise à jour depuis la suggestion IA.', 'Fermer', {
          duration: 4000, panelClass: ['snack-success']
        });
        this.loadReferentials();
      },
      error: () => {
        this.snackBar.open('Erreur lors de l\'application de la mise à jour.', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        });
      }
    });
  }

  dismissAlert(entry: EntryWithAlert): void {
    if (!entry.alertId) return;
    this.referentialService.dismissAlert(entry.alertId).subscribe({
      next: () => {
        this.snackBar.open('Alerte ignorée.', 'Fermer', { duration: 3000 });
        this.loadReferentials();
      },
      error: () => {
        this.snackBar.open('Erreur lors de l\'ignorance de l\'alerte.', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        });
      }
    });
  }

  /** SF-140-01 : popover d'aide pour le type de barème. */
  openSectionHelp(sectionType: string): void {
    this.dialog.open<ReferentialHelpDialogComponent, ReferentialHelpDialogData, void>(
        ReferentialHelpDialogComponent,
        { width: '640px', data: { sectionType } });
  }

  /** SF-140-01 : popover d'aide pour une entrée — extrait la description métier du JSON si présente. */
  openEntryHelp(entry: EntryWithAlert, sectionType: string): void {
    this.dialog.open<ReferentialHelpDialogComponent, ReferentialHelpDialogData, void>(
        ReferentialHelpDialogComponent,
        {
          width: '640px',
          data: {
            sectionType,
            entry: {
              key: entry.key,
              label: entry.label ?? entry.key,
              country: entry.country,
              sourceRef: entry.sourceRef,
              metierDescription: this.extractMetierDescription(entry, sectionType),
              rawJson: entry.valueJson,
            },
          },
        });
  }

  /** Retrouve la doc de section pour test/vérif. */
  hasSectionDoc(sectionType: string): boolean {
    return !!SECTION_DOCS[sectionType];
  }

  /**
   * SF-140-01 + SF-140-02 : retourne la meilleure description métier.
   * Priorité : description persistée en DB (seule source de vérité, pattern F-139)
   * → sinon fallback vers extraction depuis le JSON pour les 6 types à
   * description riche native.
   */
  private extractMetierDescription(entry: EntryWithAlert, sectionType: string): string | undefined {
    // SF-140-02 : description DB prime sur l'extraction JSON.
    if (entry.description && entry.description.trim().length > 0) {
      return entry.description;
    }
    try {
      const val = JSON.parse(entry.valueJson);
      switch (sectionType) {
        case 'LICENCIEMENT_CRITERES':
        case 'RUPTURE_CONV_CRITERES':
          return val.description;
        case 'IMMIGRATION_TITLES': {
          const motif = val.motif ? `Motif : ${val.motif}. ` : '';
          const conditions = val.conditions ? `${val.conditions}` : '';
          return (motif + conditions).trim() || undefined;
        }
        case 'IMMIGRATION_RECOURS': {
          const juridiction = val.juridiction ? `Juridiction : ${val.juridiction}. ` : '';
          const delai = val.delaiJours != null ? `Délai de recours : ${val.delaiJours} jours. ` : '';
          const textes = Array.isArray(val.textesApplicables) && val.textesApplicables.length > 0
              ? `Textes applicables : ${val.textesApplicables.join(' ; ')}.` : '';
          return (juridiction + delai + textes).trim() || undefined;
        }
        case 'IMMIGRATION_WORK_RIGHTS': {
          const droit = val.droitTravail ? `Droit au travail : ${val.droitTravail}. ` : '';
          const conditions = val.conditions ? `${val.conditions}` : '';
          return (droit + conditions).trim() || undefined;
        }
        case 'DIVORCE_ETAPES':
        case 'DIVORCE_PIECES':
          return val.description;
        case 'GARDE_MODES': {
          const rep = val.repartitionType ? `Répartition : ${val.repartitionType}. ` : '';
          const vac = val.vacances ? `Vacances : ${val.vacances}.` : '';
          return (rep + vac).trim() || undefined;
        }
        case 'LITIGATION_TYPE': {
          const years = val.years != null ? `Délai de prescription : ${val.years} an(s). ` : '';
          const article = val.article ? `Référence : ${val.article}.` : '';
          return (years + article).trim() || undefined;
        }
        // ----- SF-225-01 : fallback structuré pour les 5 types orphelins -----
        case 'CONVENTION_PREAVIS': {
          const article = val.article ? `Référence : ${val.article}.` : '';
          const fonctions = val?.fonctions && typeof val.fonctions === 'object'
            ? `Catégories couvertes : ${Object.keys(val.fonctions).join(', ')}. ` : '';
          return (fonctions + article).trim() || undefined;
        }
        case 'TRAVAIL_PROCEDURE_JALONS':
        case 'FAMILLE_PROCEDURE_JALONS': {
          if (!Array.isArray(val)) return undefined;
          return `Calendrier procédural — ${val.length} jalon${val.length > 1 ? 's' : ''}.`;
        }
        case 'MAJEURS_PROTEGES_REGIMES': {
          const delai = val.delaiProcedureMois != null ? `Délai procédure : ${val.delaiProcedureMois} mois. ` : '';
          const dureeMax = val.delaiInitialAnsMax != null ? `Durée initiale max : ${val.delaiInitialAnsMax} ans. ` : '';
          return (delai + dureeMax).trim() || undefined;
        }
        case 'IM21_VALIDITY_CRITERES':
          return val.description;
        default:
          return undefined;
      }
    } catch {
      return undefined;
    }
  }

  openEditDialog(entry: EntryWithAlert, sectionType: string): void {
    const dialogRef = this.dialog.open<
      ReferentialEditDialogComponent,
      ReferentialEditDialogData,
      ReferentialEditDialogResult
    >(ReferentialEditDialogComponent, {
      width: '600px',
      data: { entry, sectionType },
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.submitUpdate(entry, result);
      }
    });
  }

  private submitUpdate(entry: EntryWithAlert, result: ReferentialEditDialogResult): void {
    if (!entry.id) return;
    this.referentialService.updateReferential(entry.id, result.label, result.valueJson, result.force, result.description)
      .subscribe({
        next: response => {
          if (response.saved) {
            this.snackBar.open('Référentiel mis à jour.', 'Fermer', {
              duration: 4000, panelClass: ['snack-success']
            });
            this.loadReferentials();
          } else if (response.warning) {
            this.showWarningConfirmation(entry, result, response.warning);
          }
        },
        error: () => {
          this.snackBar.open('Erreur lors de la mise à jour.', 'Fermer', {
            duration: 4000, panelClass: ['snack-error']
          });
        }
      });
  }

  private showWarningConfirmation(entry: EntryWithAlert, result: ReferentialEditDialogResult, warning: string): void {
    const dialogRef = this.dialog.open<ReferentialWarningDialogComponent, ReferentialWarningDialogData, boolean>(
      ReferentialWarningDialogComponent,
      { width: '480px', data: { warning } }
    );
    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.submitUpdate(entry, { ...result, force: true });
      }
    });
  }

  openReportDialog(entry: EntryWithAlert): void {
    const dialogRef = this.dialog.open<
      ReferentialReportDialogComponent,
      ReferentialReportDialogData,
      ReferentialReportDialogResult
    >(ReferentialReportDialogComponent, {
      width: '500px',
      data: { entryLabel: entry.label ?? entry.key },
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result && entry.id) {
        this.referentialService.reportAnomaly(entry.id, result.comment).subscribe({
          next: () => {
            this.snackBar.open('Signalement envoyé.', 'Fermer', {
              duration: 4000, panelClass: ['snack-success']
            });
          },
          error: (err) => {
            const msg = err.status === 409
              ? 'Vous avez déjà signalé cette valeur.'
              : 'Erreur lors de l\'envoi du signalement.';
            this.snackBar.open(msg, 'Fermer', { duration: 4000, panelClass: ['snack-error'] });
          }
        });
      }
    });
  }

  private buildSections(response: ReferentialResponse): SectionDisplay[] {
    return Object.entries(response.sections).map(([type, entries]) => ({
      type,
      title: SECTION_LABELS[type] ?? type,
      entries: entries as EntryWithAlert[],
    }));
  }

  private formatDomain(domain?: string): string {
    switch (domain) {
      case 'DROIT_DU_TRAVAIL':  return 'Droit du travail';
      case 'DROIT_FAMILLE':     return 'Droit de la famille';
      case 'DROIT_IMMIGRATION': return 'Droit de l\'immigration';
      default:                  return domain ?? '';
    }
  }

  formatValue(entry: EntryWithAlert, sectionType: string): string {
    try {
      const val = JSON.parse(entry.valueJson);
      switch (sectionType) {
        case 'LITIGATION_TYPE':
          return `${val.years} an${val.years > 1 ? 's' : ''} — ${val.article}`;
        case 'IMMIGRATION_JALONS':
          return (val as { label: string; offsetDays: number }[])
            .map(j => `${j.label} (J+${j.offsetDays})`)
            .join('\n');
        case 'IMMIGRATION_PIECES':
          return (val as string[]).join('\n');
        case 'PRESTATION_COEFF':
          return `Coefficient : ${(val.coeff * 100).toFixed(0)} % — Durée de référence : ${val.dureeReferenceAns} ans`;
        case 'BAREME_MACRON':
          return val.supported ? 'Barème Macron applicable' : 'Non couvert par le barème Macron';
        case 'PENSION_TAUX': {
          const matrix = val as number[][];
          const labels = ['1 enfant', '2 enfants', '3 enfants', '4 enfants', '5 enfants et +'];
          return matrix.map((row, i) =>
            `${labels[i] ?? `${i + 1} enfants`} — garde exclusive : ${(row[0] * 100).toFixed(0)} %  |  garde alternée : ${(row[1] * 100).toFixed(0)} %`
          ).join('\n');
        }
        case 'IMMIGRATION_TITLES': {
          const motif = val.motif ?? '—';
          const conditions = val.conditions ?? '—';
          const nbPieces = Array.isArray(val.pieces) ? val.pieces.length : 0;
          const delai = val.delaiMoyenJours ?? '—';
          return `Motif : ${motif} · Conditions : ${conditions} · Délai moyen : ${delai} jours · ${nbPieces} pièce${nbPieces > 1 ? 's' : ''}`;
        }
        case 'IMMIGRATION_RECOURS': {
          const juridiction = val.juridiction ?? '—';
          const delai = val.delaiJours ?? '—';
          const nbTextes = Array.isArray(val.textesApplicables) ? val.textesApplicables.length : 0;
          const nbPieces = Array.isArray(val.piecesStandard) ? val.piecesStandard.length : 0;
          return `Juridiction : ${juridiction} · Délai : ${delai} jours · ${nbTextes} texte${nbTextes > 1 ? 's' : ''} · ${nbPieces} pièce${nbPieces > 1 ? 's' : ''}`;
        }
        case 'IMMIGRATION_WORK_RIGHTS': {
          const droit = val.droitTravail ?? '—';
          const conditions = val.conditions && val.conditions.trim() ? val.conditions : '—';
          const nbObligations = Array.isArray(val.obligationsEmployeur) ? val.obligationsEmployeur.length : 0;
          return `Droit au travail : ${droit} · Conditions : ${conditions} · ${nbObligations} obligation${nbObligations > 1 ? 's' : ''} employeur`;
        }
        case 'CONVENTION_BAREMES':
          return `Congés légaux : ${val.congesLegauxJours ?? '—'} jours`;
        case 'LICENCIEMENT_CRITERES':
        case 'RUPTURE_CONV_CRITERES': {
          const poids = val.poids ?? '—';
          const bloquant = val.bloquant ? 'Bloquant' : 'Non bloquant';
          const desc = val.description ?? '—';
          return `Poids : ${poids} · ${bloquant} — ${desc}`;
        }
        case 'INDEMNITE_BAREMES': {
          if (Array.isArray(val.entries) && val.entries.length > 0) {
            const minMois = Math.min(...val.entries.map((e: { min: number }) => e.min));
            const maxMois = Math.max(...val.entries.map((e: { max: number }) => e.max));
            return `Barème sur ${val.entries.length} années — ${minMois} à ${maxMois} mois de salaire`;
          }
          if (val.minSemaines != null && val.maxSemaines != null) {
            return `De ${val.minSemaines} à ${val.maxSemaines} semaines de salaire`;
          }
          return '—';
        }
        case 'GARDE_MODES': {
          const rep = val.repartitionType ?? '—';
          const jA = val.joursA ?? '—';
          const jB = val.joursB ?? '—';
          const vacances = val.vacances ?? '—';
          return `${rep} — Parent A : ${jA} j / Parent B : ${jB} j — Vacances : ${vacances}`;
        }
        case 'DIVORCE_ETAPES': {
          const prefix = val.obligatoire ? '⚠ ' : '';
          const ordre = val.ordre ?? '—';
          const desc = val.description ?? '—';
          const delai = val.delai ?? '—';
          return `${prefix}Étape ${ordre} · ${desc} · Délai : ${delai}`;
        }
        case 'DIVORCE_PIECES': {
          const prefix = val.obligatoire ? '⚠ ' : '';
          const desc = val.description ?? '—';
          return `${prefix}${desc}`;
        }
        // ----- SF-225-01 : 5 types orphelins -----
        case 'CONVENTION_PREAVIS': {
          const fonctions = val?.fonctions && typeof val.fonctions === 'object' ? Object.keys(val.fonctions) : [];
          const article = val?.article ?? '—';
          if (fonctions.length === 0) return `Préavis CCN · ${article}`;
          return `Préavis CCN — ${fonctions.length} catégorie${fonctions.length > 1 ? 's' : ''} (${fonctions.join(', ')}) · ${article}`;
        }
        case 'TRAVAIL_PROCEDURE_JALONS':
        case 'FAMILLE_PROCEDURE_JALONS': {
          if (!Array.isArray(val)) return '—';
          return (val as { label: string; offsetDays: number; articleRef?: string }[])
            .map(j => {
              const ref = j.articleRef ? ` [${j.articleRef}]` : '';
              return `${j.label} (J+${j.offsetDays})${ref}`;
            })
            .join('\n');
        }
        case 'MAJEURS_PROTEGES_REGIMES': {
          const articles = Array.isArray(val?.articles) ? val.articles.length : 0;
          const delai = val?.delaiProcedureMois != null ? `${val.delaiProcedureMois} mois` : '—';
          const dureeMax = val?.delaiInitialAnsMax != null ? `${val.delaiInitialAnsMax} ans` : '—';
          const renouv = val?.renouvelable ? 'renouvelable' : 'non renouvelable';
          const nbCriteres = Array.isArray(val?.criteresEligibilite) ? val.criteresEligibilite.length : 0;
          return `Délai procédure : ${delai} · Durée initiale max : ${dureeMax} (${renouv}) · ${articles} article${articles > 1 ? 's' : ''} · ${nbCriteres} critère${nbCriteres > 1 ? 's' : ''} d'éligibilité`;
        }
        case 'IM21_VALIDITY_CRITERES': {
          const binaire = val?.binaire ? 'Critère binaire (Oui/Non)' : 'Critère non binaire';
          const desc = val?.description ?? '—';
          return `${binaire} — ${desc}`;
        }
        default:
          return JSON.stringify(val, null, 2);
      }
    } catch {
      return entry.valueJson;
    }
  }

  sectionIcon(type: string): string {
    switch (type) {
      case 'LITIGATION_TYPE':         return 'gavel';
      case 'BAREME_MACRON':           return 'calculate';
      case 'IMMIGRATION_JALONS':      return 'timeline';
      case 'IMMIGRATION_PIECES':      return 'checklist';
      case 'PENSION_TAUX':            return 'child_care';
      case 'PRESTATION_COEFF':        return 'balance';
      case 'IMMIGRATION_TITLES':      return 'badge';
      case 'IMMIGRATION_RECOURS':     return 'gavel';
      case 'IMMIGRATION_WORK_RIGHTS': return 'work';
      case 'CONVENTION_BAREMES':      return 'groups';
      case 'LICENCIEMENT_CRITERES':   return 'rule';
      case 'RUPTURE_CONV_CRITERES':   return 'handshake';
      case 'INDEMNITE_BAREMES':       return 'payments';
      case 'GARDE_MODES':             return 'child_care';
      case 'DIVORCE_ETAPES':          return 'list_alt';
      case 'DIVORCE_PIECES':          return 'description';
      // ----- SF-225-01 : 5 types orphelins -----
      case 'CONVENTION_PREAVIS':           return 'description';
      case 'TRAVAIL_PROCEDURE_JALONS':     return 'event_note';
      case 'FAMILLE_PROCEDURE_JALONS':     return 'event_note';
      case 'MAJEURS_PROTEGES_REGIMES':     return 'accessibility';
      case 'IM21_VALIDITY_CRITERES':       return 'verified';
      default:                        return 'info';
    }
  }
}
