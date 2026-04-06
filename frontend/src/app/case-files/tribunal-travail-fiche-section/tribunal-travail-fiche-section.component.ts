import { Component, Input, OnInit, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TribunalTravailFicheService } from '../../core/services/tribunal-travail-fiche.service';
import { PdfExportService } from '../../core/services/pdf-export.service';
import { TribunalTravailFiche, TribunalPiece } from '../../core/models/tribunal-travail-fiche.model';

@Component({
  selector: 'app-tribunal-travail-fiche-section',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule
  ],
  templateUrl: './tribunal-travail-fiche-section.component.html',
  styleUrl: './tribunal-travail-fiche-section.component.scss'
})
export class TribunalTravailFicheSectionComponent implements OnInit {
  @Input() caseFileId!: string;
  @Input() caseFileTitle: string = '';

  collapsed = signal(true);
  saving = signal(false);
  pieces = signal<TribunalPiece[]>([]);

  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private ficheService: TribunalTravailFicheService,
    private pdfExportService: PdfExportService,
    private snackBar: MatSnackBar
  ) {
    this.form = this.fb.group({
      requerant: this.fb.group({
        nom: ['', Validators.required],
        prenom: [''],
        domicile: [''],
        registreNational: ['']
      }),
      defendeur: this.fb.group({
        nom: [''],
        siegeSocial: [''],
        numeroBce: [''],
        representant: ['']
      }),
      procedureInfo: this.fb.group({
        tribunal: [''],
        division: [''],
        langue: ['FR'],
        commissionParitaire: ['']
      }),
      contratInfo: this.fb.group({
        typeContrat: [''],
        dateDebut: [''],
        dateFin: [''],
        motifRupture: ['']
      }),
      demandes: this.fb.array([]),
      exposeDesMoyens: ['']
    });
  }

  ngOnInit(): void {
    this.ficheService.get(this.caseFileId).subscribe({
      next: fiche => this.patchForm(fiche),
      error: () => { /* fail-open : formulaire vide */ }
    });
  }

  get demandesArray(): FormArray {
    return this.form.get('demandes') as FormArray;
  }

  toggleCollapsed(): void {
    this.collapsed.update(v => !v);
  }

  addDemande(): void {
    this.demandesArray.push(
      this.fb.group({
        label: ['', Validators.required],
        montant: [null, [Validators.min(0)]]
      })
    );
  }

  removeDemande(index: number): void {
    this.demandesArray.removeAt(index);
  }

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    const value = this.form.getRawValue();
    this.ficheService.save(this.caseFileId, value).subscribe({
      next: fiche => {
        this.saving.set(false);
        this.pieces.set(fiche.piecesList);
        this.snackBar.open('Requête enregistrée', 'Fermer', {
          duration: 3000, panelClass: ['snack-success']
        });
      },
      error: () => {
        this.saving.set(false);
        this.snackBar.open('Erreur lors de la sauvegarde', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        });
      }
    });
  }

  exportPdf(): void {
    const value = this.form.getRawValue();
    const fiche: TribunalTravailFiche = {
      id: null,
      requerant: value.requerant,
      defendeur: value.defendeur,
      procedureInfo: value.procedureInfo,
      contratInfo: value.contratInfo,
      demandes: value.demandes,
      exposeDesMoyens: value.exposeDesMoyens || null,
      piecesList: this.pieces(),
      updatedAt: null,
    };
    try {
      this.pdfExportService.exportTribunalTravailFiche(fiche, this.caseFileTitle);
    } catch {
      this.snackBar.open('Erreur lors de la génération du PDF', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      });
    }
  }

  private patchForm(fiche: TribunalTravailFiche): void {
    this.form.patchValue({
      requerant: fiche.requerant,
      defendeur: fiche.defendeur,
      procedureInfo: fiche.procedureInfo,
      contratInfo: fiche.contratInfo,
      exposeDesMoyens: fiche.exposeDesMoyens ?? ''
    });
    this.demandesArray.clear();
    for (const d of fiche.demandes ?? []) {
      this.demandesArray.push(
        this.fb.group({
          label: [d.label, Validators.required],
          montant: [d.montant, [Validators.min(0)]]
        })
      );
    }
    this.pieces.set(fiche.piecesList ?? []);
  }
}
