import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { ReferentialEntry } from '../../core/models/referential.model';

export interface ReferentialEditDialogData {
  entry: ReferentialEntry;
  sectionType: string;
}

export interface ReferentialEditDialogResult {
  label: string;
  valueJson: string;
  force: boolean;
}

const PENSION_ROWS = [
  '1 enfant',
  '2 enfants',
  '3 enfants',
  '4 enfants',
  '5 enfants et +',
];

@Component({
  selector: 'app-referential-edit-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule,
    MatSlideToggleModule,
  ],
  templateUrl: './referential-edit-dialog.component.html',
  styleUrl: './referential-edit-dialog.component.scss',
})
export class ReferentialEditDialogComponent {
  form: FormGroup;
  readonly pensionRows = PENSION_ROWS.map(label => ({ label }));

  constructor(
    private fb: FormBuilder,
    public dialogRef: MatDialogRef<ReferentialEditDialogComponent, ReferentialEditDialogResult>,
    @Inject(MAT_DIALOG_DATA) public data: ReferentialEditDialogData
  ) {
    this.form = this.buildForm(data);
  }

  private buildForm(data: ReferentialEditDialogData): FormGroup {
    const base = { label: [{ value: data.entry.label, disabled: true }] };

    let parsed: any = null;
    try { parsed = JSON.parse(data.entry.valueJson); } catch { /* keep null */ }

    switch (data.sectionType) {
      case 'LITIGATION_TYPE':
        return this.fb.group({
          ...base,
          litigYears:   [parsed?.years   ?? null, [Validators.required, Validators.min(1), Validators.max(30)]],
          litigArticle: [parsed?.article ?? '',   [Validators.required, Validators.maxLength(200)]],
        });

      case 'BAREME_MACRON':
        return this.fb.group({
          ...base,
          baremeSupported: [parsed?.supported ?? true],
        });

      case 'PENSION_TAUX': {
        const matrix: number[][] = Array.isArray(parsed) ? parsed : [];
        const pensionControls: Record<string, any> = { ...base };
        for (let i = 0; i < 5; i++) {
          const exclusive = matrix[i]?.[0] != null ? +(matrix[i][0] * 100).toFixed(2) : null;
          const alternate = matrix[i]?.[1] != null ? +(matrix[i][1] * 100).toFixed(2) : null;
          pensionControls[`pension_${i}_0`] = [exclusive, [Validators.required, Validators.min(0), Validators.max(100)]];
          pensionControls[`pension_${i}_1`] = [alternate,  [Validators.required, Validators.min(0), Validators.max(100)]];
        }
        return this.fb.group(pensionControls);
      }

      case 'PRESTATION_COEFF':
        return this.fb.group({
          ...base,
          prestCoeff: [parsed?.coeff != null ? +(parsed.coeff * 100).toFixed(2) : null,
            [Validators.required, Validators.min(0), Validators.max(100)]],
          prestDuree: [parsed?.dureeReferenceAns ?? null,
            [Validators.required, Validators.min(1)]],
        });

      case 'IMMIGRATION_PIECES': {
        const lines = Array.isArray(parsed) ? (parsed as string[]).join('\n') : data.entry.valueJson;
        return this.fb.group({
          ...base,
          piecesText: [lines, Validators.required],
        });
      }

      default:
        return this.fb.group({
          ...base,
          valueJson: [data.entry.valueJson, [Validators.required, this.jsonValidator]],
        });
    }
  }

  private jsonValidator(control: { value: string }) {
    try { JSON.parse(control.value); return null; }
    catch { return { invalidJson: true }; }
  }

  private serializeValueJson(): string {
    const v = this.form.value;
    switch (this.data.sectionType) {
      case 'LITIGATION_TYPE':
        return JSON.stringify({ years: Number(v.litigYears), article: v.litigArticle });

      case 'BAREME_MACRON':
        return JSON.stringify({ supported: !!v.baremeSupported });

      case 'PENSION_TAUX': {
        const matrix = [];
        for (let i = 0; i < 5; i++) {
          matrix.push([
            +(Number(v[`pension_${i}_0`]) / 100).toFixed(4),
            +(Number(v[`pension_${i}_1`]) / 100).toFixed(4),
          ]);
        }
        return JSON.stringify(matrix);
      }

      case 'PRESTATION_COEFF':
        return JSON.stringify({
          coeff: +(Number(v.prestCoeff) / 100).toFixed(4),
          dureeReferenceAns: Number(v.prestDuree),
        });

      case 'IMMIGRATION_PIECES': {
        const pieces = (v.piecesText as string)
          .split('\n')
          .map((l: string) => l.trim())
          .filter((l: string) => l.length > 0);
        return JSON.stringify(pieces);
      }

      default:
        return v.valueJson;
    }
  }

  submit(): void {
    if (this.form.invalid) return;
    this.dialogRef.close({
      label:     this.form.getRawValue().label,
      valueJson: this.serializeValueJson(),
      force:     false,
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
