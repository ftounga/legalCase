import { Component, computed, Input, OnChanges, OnInit, signal, SimpleChanges } from '@angular/core';
import { LowerCasePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { ImmigrationChecklistService } from '../../core/services/immigration-checklist.service';
import { ImmigrationChecklist, ImmigrationPieceItem, ImmigrationStatut } from '../../core/models/immigration-checklist.model';
import { PdfExportService } from '../../core/services/pdf-export.service';

const STATUT_CYCLE: Record<ImmigrationStatut, ImmigrationStatut> = {
  INCONNU: 'PRESENT',
  PRESENT: 'ABSENT',
  ABSENT:  'INCONNU',
};

@Component({
  selector: 'app-immigration-checklist-section',
  standalone: true,
  imports: [
    FormsModule,
    LowerCasePipe,
    MatButtonModule, MatIconModule,
    MatSelectModule, MatFormFieldModule,
  ],
  templateUrl: './immigration-checklist-section.component.html',
  styleUrl: './immigration-checklist-section.component.scss'
})
export class ImmigrationChecklistSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'CHECKLIST PIÈCES IMMIGRATION';
  static readonly TOOL_ICON = 'assignment_turned_in';

  @Input() caseFileId!: string;
  @Input() caseFileTitle: string = '';
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;

  /** SF-IM-01-04 : type pré-sélectionné via analyse IA (F-IM-01 auto-prefill). */
  private _inferredChecklistType: string | null = null;
  @Input() set inferredChecklistType(v: string | null | undefined) {
    this._inferredChecklistType = v ?? null;
    if (v && this.titreTypes.some(t => t.value === v)) {
      this.titreType.set(v);
      this.aiPreselected.set(true);
    }
  }
  get inferredChecklistType(): string | null { return this._inferredChecklistType; }

  collapsed = signal(true);
  saving = signal(false);
  checklist = signal<ImmigrationChecklist | null>(null);

  titreType = signal('VISA_ETUDIANT');
  country = signal('FRANCE');
  /** SF-IM-01-04 : true si le type courant provient de l'analyse IA. */
  aiPreselected = signal(false);

  presentCount = computed(() =>
    this.checklist()?.pieces.filter(p => p.statut === 'PRESENT').length ?? 0
  );

  /** SF-IM-01-04 : liste exhaustive des 13 régimes juridiques (art. CESEDA). */
  readonly titreTypes = [
    { value: 'VISA_ETUDIANT',                 label: 'Visa étudiant (L.422-1)' },
    { value: 'APS_POST_ETUDES',               label: 'APS post-études (L.422-14)' },
    { value: 'TITRE_SALARIE',                 label: 'Titre salarié (L.421-1)' },
    { value: 'PASSEPORT_TALENT',              label: 'Passeport talent (L.421-9 à L.421-14)' },
    { value: 'CST_VPF_CONJOINT_FR',           label: 'VPF — Conjoint de Français (L.423-1)' },
    { value: 'CST_VPF_PARENT_ENFANT_FR',      label: 'VPF — Parent d\'enfant français (L.423-7)' },
    { value: 'CST_VPF_LIENS_PERSONNELS',      label: 'VPF — Liens personnels et familiaux (L.423-23)' },
    { value: 'REGROUPEMENT_FAMILIAL',         label: 'Regroupement familial (L.434-*)' },
    { value: 'ADMISSION_EXCEPTIONNELLE_AES',  label: 'Admission exceptionnelle au séjour (L.435-1)' },
    { value: 'ASILE_OFPRA',                   label: 'Demande d\'asile (OFPRA / CGRA)' },
    { value: 'PROTECTION_SUBSIDIAIRE',        label: 'Protection subsidiaire (L.712-*)' },
    { value: 'CARTE_RESIDENT_10ANS',          label: 'Carte de résident 10 ans (L.426-1)' },
    { value: 'NATURALISATION',                label: 'Naturalisation (art. 21-15 Cciv)' },
  ];

  readonly countries = [
    { value: 'FRANCE',   label: 'France' },
    { value: 'BELGIQUE', label: 'Belgique' },
  ];

  constructor(
    private checklistService: ImmigrationChecklistService,
    private snackBar: MatSnackBar,
    private pdfExportService: PdfExportService,
  ) {}

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);
    this.load();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03b : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
  }

  toggleCollapsed(): void {
    this.collapsed.update(v => !v);
  }

  onSelectorChange(): void {
    // L'utilisateur modifie manuellement → on retire le flag de pré-sélection IA.
    this.aiPreselected.set(false);
    this.load();
  }

  load(): void {
    this.checklistService.get(this.caseFileId, this.titreType(), this.country()).subscribe({
      next: cl => this.checklist.set(cl),
      error: () => this.snackBar.open('Erreur lors du chargement de la checklist', 'Fermer', { duration: 4000 }),
    });
  }

  cycleStatut(piece: ImmigrationPieceItem): void {
    piece.statut = STATUT_CYCLE[piece.statut];
    this.checklist.update(cl => cl ? { ...cl, pieces: [...cl.pieces] } : cl);
  }

  exportPdf(): void {
    const cl = this.checklist();
    if (!cl) return;
    this.pdfExportService.exportImmigrationChecklist(cl, this.caseFileTitle);
  }

  save(): void {
    const cl = this.checklist();
    if (!cl) return;
    this.saving.set(true);
    this.checklistService.upsert(this.caseFileId, {
      titreType: cl.titreType,
      country: cl.country,
      pieces: cl.pieces.map(p => ({ label: p.label, statut: p.statut })),
    }).subscribe({
      next: updated => {
        this.checklist.set(updated);
        this.saving.set(false);
        this.snackBar.open('Checklist enregistrée', undefined, { duration: 3000 });
      },
      error: () => {
        this.saving.set(false);
        this.snackBar.open('Erreur lors de l\'enregistrement', 'Fermer', { duration: 4000 });
      },
    });
  }
}
