import { Component, Input, OnInit, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PrudhomeFicheService } from '../../core/services/prudhome-fiche.service';
import { PrudhomeFiche, PrudhomePiece } from '../../core/models/prudhome-fiche.model';

@Component({
  selector: 'app-prudhome-fiche-section',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule
  ],
  templateUrl: './prudhome-fiche-section.component.html',
  styleUrl: './prudhome-fiche-section.component.scss'
})
export class PrudhomeFicheSectionComponent implements OnInit {
  @Input() caseFileId!: string;

  collapsed = signal(true);
  saving = signal(false);
  pieces = signal<PrudhomePiece[]>([]);

  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private ficheService: PrudhomeFicheService,
    private snackBar: MatSnackBar
  ) {
    this.form = this.fb.group({
      demandeur: this.fb.group({
        nom: ['', Validators.required],
        prenom: [''],
        adresse: [''],
        telephone: [''],
        email: [''],
        profession: ['']
      }),
      defendeur: this.fb.group({
        nom: [''],
        adresse: [''],
        siret: [''],
        representant: ['']
      }),
      demandes: this.fb.array([]),
      faitsTexte: [''],
      moyensDroitTexte: ['']
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
        this.snackBar.open('Fiche enregistrée', 'Fermer', {
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

  private patchForm(fiche: PrudhomeFiche): void {
    this.form.patchValue({
      demandeur: fiche.demandeur,
      defendeur: fiche.defendeur,
      faitsTexte: fiche.faitsTexte ?? '',
      moyensDroitTexte: fiche.moyensDroitTexte ?? ''
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
