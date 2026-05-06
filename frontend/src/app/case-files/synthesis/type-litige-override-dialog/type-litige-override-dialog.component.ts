import { Component, Inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { TypeLitigeOverrideService } from '../../../core/services/type-litige-override.service';
import {
  TypeLitigeOverrideDomain,
  TypeLitigeOverrideResponse,
  TypeLitigeTravailFrCode,
  TypeProcedureImmigrationCode,
  TYPE_LITIGE_TRAVAIL_FR_LABELS,
  TYPE_PROCEDURE_IMMIGRATION_LABELS,
} from '../../../core/models/type-litige-override.model';

/**
 * F-197 SF-197-02 — Données passées au dialog par le composant parent
 * (`SynthesisComponent`).
 */
export interface TypeLitigeOverrideDialogData {
  caseFileId: string;
  /** Domaine cible (TRAVAIL_FR ou IMMIGRATION). */
  domain: TypeLitigeOverrideDomain;
  /** Override déjà posé (pré-sélectionne le MatSelect + le champ raison). */
  current?: TypeLitigeOverrideResponse | null;
  /** Type détecté par l'IA (affiché en référence pour l'avocat). */
  iaDetectedCode?: string | null;
}

/** Option d'un MatSelect (code enum + libellé FR avocat). */
interface SelectOption {
  value: TypeLitigeTravailFrCode | TypeProcedureImmigrationCode;
  label: string;
}

/**
 * F-197 SF-197-02 — MatDialog "Préciser le type de litige".
 *
 * <p>UI :</p>
 * <ul>
 *   <li>MatSelect des enums (7 valeurs Travail FR ou 10 valeurs Immigration
 *       selon {@code data.domain})</li>
 *   <li>Champ MatInput optionnel "Raison" (max 500 chars)</li>
 *   <li>Boutons Annuler / Sauvegarder</li>
 * </ul>
 *
 * <p>Au clic Sauvegarder : appel PUT, MatSnackBar succès → ferme avec
 * {@code TypeLitigeOverrideResponse} ; en cas d'erreur, snackbar erreur et le
 * dialog reste ouvert (CA-erreur SF-197-02).</p>
 *
 * <p>Cohérence F-176 stricte : aucun {@code triggerRefresh()} déclenché ici.
 * Le composant parent met à jour son signal local seulement.</p>
 */
@Component({
  selector: 'app-type-litige-override-dialog',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './type-litige-override-dialog.component.html',
  styleUrls: ['./type-litige-override-dialog.component.scss'],
})
export class TypeLitigeOverrideDialogComponent {
  /** Code sélectionné dans le MatSelect (single-value). */
  selectedCode = signal<TypeLitigeTravailFrCode | TypeProcedureImmigrationCode | null>(null);
  /** Texte libre saisi par l'avocat (optionnel). */
  raison = signal<string>('');
  /** PUT en cours — désactive le bouton "Sauvegarder" et affiche un spinner. */
  saving = signal<boolean>(false);

  /** Liste d'options du MatSelect, calculée une seule fois. */
  readonly options: readonly SelectOption[];

  /** Libellé FR du type IA détecté (rappel à l'avocat dans l'en-tête du dialog). */
  readonly iaDetectedLabel: string | null;

  /** Limite stricte côté frontend (le backend valide aussi à 500). */
  readonly RAISON_MAX_LENGTH = 500;

  constructor(
    private readonly dialogRef: MatDialogRef<TypeLitigeOverrideDialogComponent, TypeLitigeOverrideResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: TypeLitigeOverrideDialogData,
    private readonly service: TypeLitigeOverrideService,
    private readonly snackBar: MatSnackBar,
  ) {
    this.options = this.buildOptions(data.domain);
    this.iaDetectedLabel = this.lookupLabel(data.iaDetectedCode ?? null);

    // Pré-sélection si override déjà posé.
    const current = data.current;
    if (current) {
      const code = current.typeLitigeAvocat ?? current.typeProcedureAvocat ?? null;
      if (code) {
        this.selectedCode.set(code);
      }
      if (current.raison) {
        this.raison.set(current.raison);
      }
    }
  }

  /** Liste des options du MatSelect selon le domaine. */
  private buildOptions(domain: TypeLitigeOverrideDomain): readonly SelectOption[] {
    if (domain === 'TRAVAIL_FR') {
      return (Object.entries(TYPE_LITIGE_TRAVAIL_FR_LABELS) as [TypeLitigeTravailFrCode, string][])
        .map(([value, label]) => ({ value, label }));
    }
    return (Object.entries(TYPE_PROCEDURE_IMMIGRATION_LABELS) as [TypeProcedureImmigrationCode, string][])
      .map(([value, label]) => ({ value, label }));
  }

  /** Recherche le libellé FR d'un code (Travail FR ou Immigration). */
  private lookupLabel(code: string | null): string | null {
    if (!code) return null;
    const t = TYPE_LITIGE_TRAVAIL_FR_LABELS as Record<string, string>;
    if (t[code]) return t[code];
    const i = TYPE_PROCEDURE_IMMIGRATION_LABELS as Record<string, string>;
    if (i[code]) return i[code];
    return null;
  }

  /** Désactive le bouton Sauvegarder tant qu'aucune sélection. */
  canSave(): boolean {
    return this.selectedCode() !== null && !this.saving();
  }

  /** Annulation — ferme sans rien faire. */
  cancel(): void {
    this.dialogRef.close(undefined);
  }

  /**
   * Sauvegarde — appel PUT, snackbar succès/erreur, fermeture si succès.
   * <p>Cohérence F-176 stricte : aucun {@code triggerRefresh()} ici.</p>
   */
  save(): void {
    const code = this.selectedCode();
    if (!code) return;
    this.saving.set(true);
    const trimmed = this.raison().trim();
    this.service.update(this.data.caseFileId, {
      type: code,
      raison: trimmed.length > 0 ? trimmed : null,
    }).subscribe({
      next: response => {
        this.saving.set(false);
        this.snackBar.open('Type de litige enregistré', 'Fermer', {
          duration: 4000,
          panelClass: ['snack-success'],
        });
        this.dialogRef.close(response);
      },
      error: () => {
        this.saving.set(false);
        this.snackBar.open(
          'Impossible d\'enregistrer le type de litige. Veuillez réessayer.',
          'Fermer',
          { duration: 4000, panelClass: ['snack-error'] },
        );
        // Dialog reste ouvert volontairement (CA-erreur SF-197-02).
      },
    });
  }

  /** Met à jour le signal raison (binding two-way ngModel). */
  onRaisonInput(value: string): void {
    this.raison.set(value);
  }

  /** Met à jour le signal selectedCode (binding ngModelChange MatSelect). */
  onCodeChange(value: TypeLitigeTravailFrCode | TypeProcedureImmigrationCode | null): void {
    this.selectedCode.set(value);
  }
}
