import { Component, computed, Input, OnInit, signal } from '@angular/core';
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
export class ImmigrationChecklistSectionComponent implements OnInit {
  @Input() caseFileId!: string;
  @Input() caseFileTitle: string = '';

  collapsed = signal(true);
  saving = signal(false);
  checklist = signal<ImmigrationChecklist | null>(null);

  titreType = signal('VISA_ETUDIANT');
  country = signal('FRANCE');

  presentCount = computed(() =>
    this.checklist()?.pieces.filter(p => p.statut === 'PRESENT').length ?? 0
  );

  readonly titreTypes = [
    { value: 'VISA_ETUDIANT',          label: 'Visa étudiant' },
    { value: 'TITRE_SALARIE',          label: 'Titre salarié' },
    { value: 'REGROUPEMENT_FAMILIAL',  label: 'Regroupement familial' },
    { value: 'NATURALISATION',         label: 'Naturalisation' },
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
    this.load();
  }

  toggleCollapsed(): void {
    this.collapsed.update(v => !v);
  }

  onSelectorChange(): void {
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
