import { Component, Input, OnInit, OnChanges, SimpleChanges, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { ImmigrationRecoursService } from '../../core/services/immigration-recours.service';
import { RecoursResponse } from '../../core/models/immigration-recours.model';
import { PdfExportService } from '../../core/services/pdf-export.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

const VALID_RECOURS_CODES = new Set([
  'RECOURS_GRACIEUX_PREFET', 'RECOURS_CONTENTIEUX_TA', 'RECOURS_CNDA',
  'RECOURS_CGRA', 'RECOURS_CCE', 'RECOURS_CE_BELGIQUE',
]);

@Component({
  selector: 'app-immigration-recours-section',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule, MatIconModule,
    MatSelectModule, MatFormFieldModule,
    MatInputModule, MatProgressSpinnerModule,
  ],
  templateUrl: './immigration-recours-section.component.html',
  styleUrl: './immigration-recours-section.component.scss'
})
export class ImmigrationRecoursSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() caseFileTitle: string = '';
  @Input() aiData?: ImmigrationExtractedData | null;

  collapsed = signal(true);
  loading = signal(false);
  generating = signal(false);
  showForm = signal(true);
  recours = signal<RecoursResponse | null>(null);

  // Form fields
  recoursType = signal('RECOURS_GRACIEUX_PREFET');
  dateNotification = signal('');

  // Provenance notes (SF-IM-06-04)
  provenanceRecoursType = signal<'IA' | null>(null);
  provenanceDateNotification = signal<'IA' | null>(null);
  nom = signal('');
  prenom = signal('');
  nationalite = signal('');
  adresse = signal('');
  autorite = signal('');
  dateDecision = signal('');
  reference = signal('');
  exposeFaits = signal('');

  readonly recoursTypes = [
    { value: 'RECOURS_GRACIEUX_PREFET', label: 'Recours gracieux (Préfet)', country: 'FR' },
    { value: 'RECOURS_CONTENTIEUX_TA', label: 'Recours contentieux (TA)', country: 'FR' },
    { value: 'RECOURS_CNDA', label: 'Recours CNDA (asile)', country: 'FR' },
    { value: 'RECOURS_CGRA', label: 'Recours CGRA', country: 'BE' },
    { value: 'RECOURS_CCE', label: 'Recours CCE', country: 'BE' },
    { value: 'RECOURS_CE_BELGIQUE', label: 'Recours Conseil d\'État', country: 'BE' },
  ];

  constructor(
    private recoursService: ImmigrationRecoursService,
    private snackBar: MatSnackBar,
    private pdfExportService: PdfExportService,
  ) {}

  ngOnInit(): void {
    this.loadExisting();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['aiData'] && this.showForm() && !this.recours()) {
      this.prefillFromAi();
    }
  }

  toggleCollapsed(): void {
    this.collapsed.update(v => !v);
  }

  loadExisting(): void {
    this.loading.set(true);
    this.recoursService.get(this.caseFileId).subscribe({
      next: resp => {
        this.recours.set(resp);
        this.prefillForm(resp);
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

  private prefillFromAi(): void {
    if (!this.aiData) return;
    const code = this.aiData.typeRecoursCode?.toUpperCase();
    if (code && VALID_RECOURS_CODES.has(code)) {
      this.recoursType.set(code);
      this.provenanceRecoursType.set('IA');
    }
    const date = this.aiData.dateNotificationDecisionContestee;
    if (date && /^\d{4}-\d{2}-\d{2}$/.test(date)) {
      this.dateNotification.set(date);
      this.provenanceDateNotification.set('IA');
    }
  }

  onRecoursTypeChange(): void { this.provenanceRecoursType.set(null); }
  onDateNotificationChange(): void { this.provenanceDateNotification.set(null); }

  generate(): void {
    this.generating.set(true);
    this.recoursService.generate(this.caseFileId, {
      recoursType: this.recoursType(),
      dateNotification: this.dateNotification(),
      requerant: {
        nom: this.nom(),
        prenom: this.prenom(),
        nationalite: this.nationalite(),
        adresse: this.adresse(),
      },
      decisionContestee: {
        autorite: this.autorite(),
        date: this.dateDecision(),
        reference: this.reference() || null,
      },
      exposeFaits: this.exposeFaits() || null,
    }).subscribe({
      next: resp => {
        this.recours.set(resp);
        this.showForm.set(false);
        this.generating.set(false);
      },
      error: () => {
        this.generating.set(false);
        this.snackBar.open('Erreur lors de la génération du recours', 'Fermer', { duration: 4000 });
      },
    });
  }

  editForm(): void {
    const r = this.recours();
    if (r) this.prefillForm(r);
    this.showForm.set(true);
  }

  exportPdf(): void {
    const r = this.recours();
    if (!r) return;
    this.pdfExportService.exportRecoursImmigration(r, this.caseFileTitle);
  }

  private prefillForm(resp: RecoursResponse): void {
    this.recoursType.set(resp.recoursType);
    this.dateNotification.set(resp.dateNotification);
    this.nom.set(resp.requerant.nom);
    this.prenom.set(resp.requerant.prenom);
    this.nationalite.set(resp.requerant.nationalite);
    this.adresse.set(resp.requerant.adresse);
    this.autorite.set(resp.decisionContestee.autorite);
    this.dateDecision.set(resp.decisionContestee.date);
    this.reference.set(resp.decisionContestee.reference ?? '');
    this.exposeFaits.set(resp.exposeFaits ?? '');
  }
}
