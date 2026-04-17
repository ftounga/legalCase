import { Component, Inject } from '@angular/core';
import { AbstractControl, FormArray, FormBuilder, FormGroup, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
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

const GARDE_REPARTITION_OPTIONS = [
  { value: 'ALTERNEE_1_SUR_2', label: 'Alternée (1 semaine / 2)' },
  { value: 'DVH_CLASSIQUE',    label: 'DVH classique' },
  { value: 'DVH_ELARGI',       label: 'DVH élargi' },
];

@Component({
  selector: 'app-referential-edit-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule,
    MatIconModule, MatSelectModule, MatSlideToggleModule,
  ],
  templateUrl: './referential-edit-dialog.component.html',
  styleUrl: './referential-edit-dialog.component.scss',
})
export class ReferentialEditDialogComponent {
  form: FormGroup;
  readonly pensionRows = PENSION_ROWS.map(label => ({ label }));
  readonly gardeOptions = GARDE_REPARTITION_OPTIONS;

  constructor(
    private fb: FormBuilder,
    public dialogRef: MatDialogRef<ReferentialEditDialogComponent, ReferentialEditDialogResult>,
    @Inject(MAT_DIALOG_DATA) public data: ReferentialEditDialogData
  ) {
    this.form = this.buildForm(data);
  }

  // ---- FormArray getters (pour le template) -------------------------------

  get macronEntries(): FormArray  { return this.form.get('macronEntries') as FormArray; }
  get convCongesSupp(): FormArray { return this.form.get('convCongesSupp') as FormArray; }
  get convPrimes(): FormArray     { return this.form.get('convPrimes') as FormArray; }
  get jalons(): FormArray         { return this.form.get('jalons') as FormArray; }

  // ---- Add / remove FormArray rows ---------------------------------------

  addMacronEntry(): void {
    const lastAn = this.macronEntries.length > 0
      ? Number((this.macronEntries.at(this.macronEntries.length - 1).get('an')?.value ?? 0)) + 1
      : 0;
    this.macronEntries.push(this.buildMacronEntryRow({ an: lastAn, min: 0, max: 0 }));
  }
  removeMacronEntry(i: number): void { this.macronEntries.removeAt(i); }

  addCongesSupp(): void {
    this.convCongesSupp.push(this.buildCongesSuppRow({ min: 1, jours: 0 }));
  }
  removeCongesSupp(i: number): void { this.convCongesSupp.removeAt(i); }

  addPrime(): void {
    this.convPrimes.push(this.buildPrimeRow({ min: 1, pct: 0 }));
  }
  removePrime(i: number): void { this.convPrimes.removeAt(i); }

  addJalon(): void {
    this.jalons.push(this.buildJalonRow({ label: '', offsetDays: 0 }));
  }
  removeJalon(i: number): void { this.jalons.removeAt(i); }

  // ---- Form building ------------------------------------------------------

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

      case 'IMMIGRATION_TITLES':
        return this.fb.group({
          ...base,
          titleMotif:      [parsed?.motif ?? '', Validators.required],
          titleConditions: [parsed?.conditions ?? '', Validators.required],
          titlePieces:     [Array.isArray(parsed?.pieces) ? parsed.pieces.join('\n') : '', Validators.required],
          titleDelai:      [parsed?.delaiMoyenJours ?? 0, [Validators.required, Validators.min(0)]],
        });

      case 'IMMIGRATION_RECOURS':
        return this.fb.group({
          ...base,
          recoursDelai:     [parsed?.delaiJours ?? 0, [Validators.required, Validators.min(1)]],
          recoursJuridiction:[parsed?.juridiction ?? '', Validators.required],
          recoursTextes:    [Array.isArray(parsed?.textesApplicables) ? parsed.textesApplicables.join('\n') : ''],
          recoursPieces:    [Array.isArray(parsed?.piecesStandard) ? parsed.piecesStandard.join('\n') : ''],
        });

      case 'IMMIGRATION_WORK_RIGHTS':
        return this.fb.group({
          ...base,
          wrDroit:       [parsed?.droitTravail ?? 'OUI', Validators.required],
          wrConditions:  [parsed?.conditions ?? ''],
          wrObligations: [Array.isArray(parsed?.obligationsEmployeur) ? parsed.obligationsEmployeur.join('\n') : ''],
        });

      case 'CONVENTION_BAREMES': {
        const supp  = Array.isArray(parsed?.congesSupp) ? parsed.congesSupp : [];
        const prims = Array.isArray(parsed?.primes) ? parsed.primes : [];
        return this.fb.group({
          ...base,
          convConges: [parsed?.congesLegauxJours ?? 25, [Validators.required, Validators.min(0), Validators.max(60)]],
          convCongesSupp: this.fb.array(supp.map((p: any) => this.buildCongesSuppRow(p))),
          convPrimes:     this.fb.array(prims.map((p: any) => this.buildPrimeRow(p))),
        });
      }

      case 'LICENCIEMENT_CRITERES':
        return this.fb.group({
          ...base,
          critPoids:     [parsed?.poids ?? 10, [Validators.required, Validators.min(1), Validators.max(50)]],
          critBloquant:  [parsed?.bloquant ?? false],
          critDesc:      [parsed?.description ?? '', Validators.required],
        });

      case 'INDEMNITE_BAREMES': {
        if (Array.isArray(parsed?.entries)) {
          return this.fb.group({
            ...base,
            baremeShape: ['MACRON'],
            macronEntries: this.fb.array(
              parsed.entries.map((e: any) => this.buildMacronEntryRow(e)),
              [Validators.required, minArrayLength(1)]
            ),
          });
        }
        if (parsed && typeof parsed.minSemaines === 'number' && typeof parsed.maxSemaines === 'number') {
          const group = this.fb.group({
            ...base,
            baremeShape: ['CCT109'],
            cct109Min: [parsed.minSemaines, [Validators.required, Validators.min(0), Validators.max(104)]],
            cct109Max: [parsed.maxSemaines, [Validators.required, Validators.min(0), Validators.max(104)]],
          }, { validators: minMaxPairValidator('cct109Min', 'cct109Max') });
          return group;
        }
        // Schéma inconnu → fallback JSON
        return this.fb.group({
          ...base,
          valueJson: [data.entry.valueJson, [Validators.required, this.jsonValidator]],
        });
      }

      case 'GARDE_MODES':
        return this.fb.group({
          ...base,
          gardeRepartition: [parsed?.repartitionType ?? 'ALTERNEE_1_SUR_2', Validators.required],
          gardePeriodesA:   [Array.isArray(parsed?.periodesA) ? parsed.periodesA.join('\n') : '', Validators.required],
          gardePeriodesB:   [Array.isArray(parsed?.periodesB) ? parsed.periodesB.join('\n') : '', Validators.required],
          gardeVacances:    [parsed?.vacances ?? '', Validators.required],
          gardeJoursA:      [parsed?.joursA ?? 0, [Validators.required, Validators.min(0), Validators.max(365)]],
          gardeJoursB:      [parsed?.joursB ?? 0, [Validators.required, Validators.min(0), Validators.max(365)]],
        });

      case 'DIVORCE_ETAPES':
        return this.fb.group({
          ...base,
          etapeOrdre:       [parsed?.ordre ?? 1, [Validators.required, Validators.min(1), Validators.max(20)]],
          etapeDescription: [parsed?.description ?? '', [Validators.required, Validators.maxLength(500)]],
          etapeDelai:       [parsed?.delai ?? '—', [Validators.required, Validators.maxLength(50)]],
          etapeObligatoire: [parsed?.obligatoire ?? true],
        });

      case 'DIVORCE_PIECES':
        return this.fb.group({
          ...base,
          pieceDescription: [parsed?.description ?? '', [Validators.required, Validators.maxLength(500)]],
          pieceObligatoire: [parsed?.obligatoire ?? true],
        });

      case 'IMMIGRATION_JALONS': {
        if (!Array.isArray(parsed)) {
          return this.fb.group({
            ...base,
            valueJson: [data.entry.valueJson, [Validators.required, this.jsonValidator]],
          });
        }
        return this.fb.group({
          ...base,
          jalons: this.fb.array(
            parsed.map((j: any) => this.buildJalonRow(j ?? {})),
            [Validators.required, minArrayLength(1)]
          ),
        });
      }

      default:
        return this.fb.group({
          ...base,
          valueJson: [data.entry.valueJson, [Validators.required, this.jsonValidator]],
        });
    }
  }

  // ---- Row builders pour les FormArray ------------------------------------

  private buildMacronEntryRow(e: any): FormGroup {
    return this.fb.group({
      an:  [e?.an  ?? 0, [Validators.required, Validators.min(0), Validators.max(49)]],
      min: [e?.min ?? 0, [Validators.required, Validators.min(0), Validators.max(50)]],
      max: [e?.max ?? 0, [Validators.required, Validators.min(0), Validators.max(50)]],
    }, { validators: minMaxPairValidator('min', 'max') });
  }

  private buildCongesSuppRow(p: any): FormGroup {
    return this.fb.group({
      min:   [p?.min   ?? 1, [Validators.required, Validators.min(1), Validators.max(50)]],
      jours: [p?.jours ?? 0, [Validators.required, Validators.min(0), Validators.max(30)]],
    });
  }

  private buildPrimeRow(p: any): FormGroup {
    return this.fb.group({
      min: [p?.min ?? 1, [Validators.required, Validators.min(1), Validators.max(50)]],
      pct: [p?.pct ?? 0, [Validators.required, Validators.min(0), Validators.max(100)]],
    });
  }

  private buildJalonRow(j: any): FormGroup {
    return this.fb.group({
      label:      [j?.label ?? '', [Validators.required, Validators.maxLength(200)]],
      offsetDays: [j?.offsetDays ?? 0, [Validators.required, Validators.min(0), Validators.max(1825)]],
    });
  }

  // ---- Validators ---------------------------------------------------------

  private jsonValidator(control: { value: string }) {
    try { JSON.parse(control.value); return null; }
    catch { return { invalidJson: true }; }
  }

  // ---- Serialization ------------------------------------------------------

  private serializeValueJson(): string {
    const v = this.form.getRawValue();
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
        const pieces = splitLines(v.piecesText as string);
        return JSON.stringify(pieces);
      }

      case 'IMMIGRATION_TITLES': {
        const origParsed = JSON.parse(this.data.entry.valueJson);
        return JSON.stringify({
          ...origParsed,
          motif: v.titleMotif,
          conditions: v.titleConditions,
          pieces: splitLines(v.titlePieces as string),
          delaiMoyenJours: Number(v.titleDelai),
        });
      }

      case 'IMMIGRATION_RECOURS':
        return JSON.stringify({
          delaiJours: Number(v.recoursDelai),
          juridiction: v.recoursJuridiction,
          textesApplicables: splitLines(v.recoursTextes as string),
          piecesStandard: splitLines(v.recoursPieces as string),
        });

      case 'IMMIGRATION_WORK_RIGHTS':
        return JSON.stringify({
          droitTravail: v.wrDroit,
          conditions: v.wrConditions,
          obligationsEmployeur: splitLines(v.wrObligations as string),
        });

      case 'CONVENTION_BAREMES': {
        const congesSupp = (v.convCongesSupp as any[])
          .map(p => ({ min: Number(p.min), jours: Number(p.jours) }))
          .sort((a, b) => a.min - b.min);
        const primes = (v.convPrimes as any[])
          .map(p => ({ min: Number(p.min), pct: Number(p.pct) }))
          .sort((a, b) => a.min - b.min);
        return JSON.stringify({
          congesLegauxJours: Number(v.convConges),
          congesSupp,
          primes,
        });
      }

      case 'LICENCIEMENT_CRITERES':
        return JSON.stringify({
          poids: Number(v.critPoids),
          bloquant: !!v.critBloquant,
          description: v.critDesc,
        });

      case 'INDEMNITE_BAREMES': {
        if (v.baremeShape === 'MACRON') {
          const entries = (v.macronEntries as any[])
            .map(e => ({ an: Number(e.an), min: Number(e.min), max: Number(e.max) }))
            .sort((a, b) => a.an - b.an);
          return JSON.stringify({ entries });
        }
        if (v.baremeShape === 'CCT109') {
          return JSON.stringify({
            minSemaines: Number(v.cct109Min),
            maxSemaines: Number(v.cct109Max),
          });
        }
        return v.valueJson;
      }

      case 'GARDE_MODES':
        return JSON.stringify({
          repartitionType: v.gardeRepartition,
          periodesA: splitLines(v.gardePeriodesA as string),
          periodesB: splitLines(v.gardePeriodesB as string),
          vacances: v.gardeVacances,
          joursA: Number(v.gardeJoursA),
          joursB: Number(v.gardeJoursB),
        });

      case 'DIVORCE_ETAPES':
        return JSON.stringify({
          ordre: Number(v.etapeOrdre),
          description: v.etapeDescription,
          delai: v.etapeDelai,
          obligatoire: !!v.etapeObligatoire,
        });

      case 'DIVORCE_PIECES':
        return JSON.stringify({
          description: v.pieceDescription,
          obligatoire: !!v.pieceObligatoire,
        });

      case 'IMMIGRATION_JALONS': {
        if (Array.isArray(v.jalons)) {
          const jalons = (v.jalons as any[]).map(j => ({
            label: j.label,
            offsetDays: Number(j.offsetDays),
          }));
          return JSON.stringify(jalons);
        }
        return v.valueJson;
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

// ---- Helpers (module-level) ----------------------------------------------

function splitLines(text: string): string[] {
  return (text ?? '')
    .split('\n')
    .map(l => l.trim())
    .filter(l => l.length > 0);
}

function minArrayLength(min: number): ValidatorFn {
  return (control: AbstractControl) => {
    const arr = control as FormArray;
    return arr.length >= min ? null : { minArrayLength: { requiredLength: min, actualLength: arr.length } };
  };
}

/** Cross-field validator: value of `minKey` must be ≤ value of `maxKey`. */
function minMaxPairValidator(minKey: string, maxKey: string): ValidatorFn {
  return (group: AbstractControl) => {
    const minCtrl = group.get(minKey);
    const maxCtrl = group.get(maxKey);
    if (!minCtrl || !maxCtrl) return null;
    const min = Number(minCtrl.value);
    const max = Number(maxCtrl.value);
    if (Number.isFinite(min) && Number.isFinite(max) && min > max) {
      return { minMaxInverted: true };
    }
    return null;
  };
}
