import { Component, Inject, computed, signal } from '@angular/core';
import { AbstractControl, FormArray, FormBuilder, FormGroup, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { ReferentialEntry } from '../../core/models/referential.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';

/**
 * SF-225-02 : fields audités côté dialog référentiels pour la validation IA
 * live (pattern F-IA-03). Liste union plutôt qu'un enum pour rester souple
 * face aux multiples sectionTypes (chaque type ne consomme qu'une partie de
 * cette union).
 */
export type ReferentialDialogAlertField =
  | 'LITIG_YEARS'
  | 'TITLE_DELAI'
  | 'RECOURS_DELAI'
  | 'MP_DELAI_PROCEDURE'
  | 'IM21_DESCRIPTION'
  | 'CONV_CONGES'
  | 'CRIT_POIDS'
  | 'ETAPE_ORDRE';

export type ReferentialDialogCoherenceAlert = CoherenceAlert<ReferentialDialogAlertField>;

export interface ReferentialEditDialogData {
  entry: ReferentialEntry;
  sectionType: string;
}

export interface ReferentialEditDialogResult {
  label: string;
  valueJson: string;
  force: boolean;
  /** SF-140-03 : description métier éditable par l'admin (empty → pas mise à jour). */
  description: string;
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
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './referential-edit-dialog.component.html',
  styleUrl: './referential-edit-dialog.component.scss',
})
export class ReferentialEditDialogComponent {
  form: FormGroup;
  readonly pensionRows = PENSION_ROWS.map(label => ({ label }));
  readonly gardeOptions = GARDE_REPARTITION_OPTIONS;

  // SF-225-02 : signal qui suit la valeur courante du form pour alimenter
  // le computed `coherenceAlerts` (recalcul live à chaque modification).
  private readonly formValue = signal<Record<string, unknown>>({});

  constructor(
    private fb: FormBuilder,
    public dialogRef: MatDialogRef<ReferentialEditDialogComponent, ReferentialEditDialogResult>,
    @Inject(MAT_DIALOG_DATA) public data: ReferentialEditDialogData
  ) {
    this.form = this.buildForm(data);
    // SF-140-03 : champ description commun à tous les types de référentiel.
    this.form.addControl(
      'description',
      this.fb.control(data.entry.description ?? '', [Validators.maxLength(2000)])
    );
    // SF-225-02 : initialise + suit la valeur du form pour les alertes live.
    this.formValue.set(this.form.getRawValue());
    this.form.valueChanges.subscribe(() => {
      this.formValue.set(this.form.getRawValue());
    });
  }

  // ---- SF-225-02 : alertes de cohérence F-IA-03 ---------------------------

  /**
   * Computed signal qui produit une alerte par field clé du dialog quand un
   * seuil simple est franchi. Recalculé à chaque `formValue` change.
   *
   * Convention : `source = 'IA'` car c'est une analyse de seuil locale, et
   * c'est la sémantique la plus proche disponible dans `CoherenceAlertSource`
   * pour ce pattern de validation côté formulaire admin.
   */
  coherenceAlerts = computed<Partial<Record<ReferentialDialogAlertField, ReferentialDialogCoherenceAlert>>>(() => {
    const alerts: Partial<Record<ReferentialDialogAlertField, ReferentialDialogCoherenceAlert>> = {};
    const v = this.formValue();
    const sectionType = this.data.sectionType;

    // LITIGATION_TYPE : prescription au-delà des durées usuelles (> 10 ans).
    // Note : les Validators bornent déjà à 1-30 ; cette alerte pointe les
    // valeurs valides mais inhabituelles (la majorité des prescriptions
    // sociales sont 1-5 ans, la décennale étant l'extrême usuel).
    if (sectionType === 'LITIGATION_TYPE') {
      const years = Number((v as { litigYears?: unknown }).litigYears);
      if (Number.isFinite(years) && years > 10) {
        const a = CoherenceAlertBuilder
          .forField<ReferentialDialogAlertField>('LITIG_YEARS')
          .withSeverity('WARNING')
          .addSource('IA', {
            expectedDisplay: '≤ 10 ans',
            reason: `Prescription saisie : ${years} ans. Les prescriptions usuelles en droit social sont 1 à 5 ans (10 ans pour les actions personnelles ou réelles immobilières).`,
          })
          .build();
        if (a) alerts.LITIG_YEARS = a;
      }
    }

    // IMMIGRATION_TITLES : délai instruction extrême (> 999 j ≈ 33 mois)
    if (sectionType === 'IMMIGRATION_TITLES') {
      const delai = Number((v as { titleDelai?: unknown }).titleDelai);
      if (Number.isFinite(delai) && delai > 999) {
        const a = CoherenceAlertBuilder
          .forField<ReferentialDialogAlertField>('TITLE_DELAI')
          .withSeverity('WARNING')
          .addSource('IA', {
            expectedDisplay: '≤ 999 jours',
            reason: `Délai moyen saisi : ${delai} jours (≈ ${Math.round(delai / 30)} mois). Cela dépasse les ordres de grandeur connus pour un titre de séjour standard.`,
          })
          .build();
        if (a) alerts.TITLE_DELAI = a;
      }
    }

    // IMMIGRATION_RECOURS : délai recours hors fourchette usuelle (> 90 j ou < 7 j)
    // Note : Validators borne min(1) ; les délais usuels sont 48h (OQTF) à 60j (2 mois).
    if (sectionType === 'IMMIGRATION_RECOURS') {
      const delai = Number((v as { recoursDelai?: unknown }).recoursDelai);
      if (Number.isFinite(delai) && delai >= 1 && (delai < 2 || delai > 90)) {
        const a = CoherenceAlertBuilder
          .forField<ReferentialDialogAlertField>('RECOURS_DELAI')
          .withSeverity('WARNING')
          .addSource('IA', {
            expectedDisplay: '2–90 jours',
            reason: `Délai recours saisi : ${delai} jours. Les délais usuels en contentieux des étrangers sont entre 48 h (OQTF) et 60 j (2 mois recours général).`,
          })
          .build();
        if (a) alerts.RECOURS_DELAI = a;
      }
    }

    // MAJEURS_PROTEGES_REGIMES : délai procédure au-delà des durées usuelles (> 12 mois).
    // Note : Validators borne 0-60 ; les délais d'instruction usuels sont < 12 mois.
    if (sectionType === 'MAJEURS_PROTEGES_REGIMES') {
      const delai = Number((v as { mpDelaiProcedure?: unknown }).mpDelaiProcedure);
      if (Number.isFinite(delai) && delai > 12) {
        const a = CoherenceAlertBuilder
          .forField<ReferentialDialogAlertField>('MP_DELAI_PROCEDURE')
          .withSeverity('WARNING')
          .addSource('IA', {
            expectedDisplay: '≤ 12 mois',
            reason: `Délai procédure saisi : ${delai} mois. Les délais d'instruction d'une mesure de protection juridique sont normalement ≤ 12 mois.`,
          })
          .build();
        if (a) alerts.MP_DELAI_PROCEDURE = a;
      }
    }

    // IM21_VALIDITY_CRITERES : description longue (> 800 char) — peu lisible
    // (maxlength HTML est 1000 ; on déclenche l'info bien avant la limite)
    if (sectionType === 'IM21_VALIDITY_CRITERES') {
      const desc = String((v as { im21Description?: unknown }).im21Description ?? '');
      if (desc.length > 800) {
        const a = CoherenceAlertBuilder
          .forField<ReferentialDialogAlertField>('IM21_DESCRIPTION')
          .withSeverity('INFO')
          .addSource('IA', {
            expectedDisplay: '≤ 800 char',
            reason: `Description : ${desc.length} caractères. Au-delà de 800 caractères, le critère devient peu lisible — envisager de scinder en plusieurs critères.`,
          })
          .build();
        if (a) alerts.IM21_DESCRIPTION = a;
      }
    }

    // CONVENTION_BAREMES : congés légaux à 0 — suspect (au moins 25 jours en FR)
    if (sectionType === 'CONVENTION_BAREMES') {
      const conges = Number((v as { convConges?: unknown }).convConges);
      if (Number.isFinite(conges) && conges < 20) {
        const a = CoherenceAlertBuilder
          .forField<ReferentialDialogAlertField>('CONV_CONGES')
          .withSeverity('WARNING')
          .addSource('IA', {
            expectedDisplay: '≥ 20 jours',
            reason: `Congés légaux saisis : ${conges} jours. Le minimum légal est 25 jours ouvrables en FR (≈ 20 jours ouvrés). Une convention ne peut être inférieure.`,
          })
          .build();
        if (a) alerts.CONV_CONGES = a;
      }
    }

    // LICENCIEMENT_CRITERES : poids élevé (≥ 30) — vérifier l'intention.
    // Note : Validators borne 1-50 ; un poids ≥ 30 écrase la plupart des
    // autres critères et devrait probablement être un toggle "bloquant".
    if (sectionType === 'LICENCIEMENT_CRITERES') {
      const poids = Number((v as { critPoids?: unknown }).critPoids);
      if (Number.isFinite(poids) && poids >= 30) {
        const a = CoherenceAlertBuilder
          .forField<ReferentialDialogAlertField>('CRIT_POIDS')
          .withSeverity('WARNING')
          .addSource('IA', {
            expectedDisplay: '< 30',
            reason: `Poids saisi : ${poids}. Un poids ≥ 30 écrase la majorité des autres critères — vérifier l'intention ou utiliser le toggle "bloquant".`,
          })
          .build();
        if (a) alerts.CRIT_POIDS = a;
      }
    }

    // DIVORCE_ETAPES : ordre élevé (> 10) — typiquement 1–10 étapes.
    // Note : Validators borne 1-20 ; un ordre > 10 reste valide mais
    // inhabituel (procédure morcelée).
    if (sectionType === 'DIVORCE_ETAPES') {
      const ordre = Number((v as { etapeOrdre?: unknown }).etapeOrdre);
      if (Number.isFinite(ordre) && ordre > 10) {
        const a = CoherenceAlertBuilder
          .forField<ReferentialDialogAlertField>('ETAPE_ORDRE')
          .withSeverity('INFO')
          .addSource('IA', {
            expectedDisplay: '≤ 10',
            reason: `Ordre saisi : ${ordre}. Une procédure de divorce comporte usuellement 5 à 10 étapes.`,
          })
          .build();
        if (a) alerts.ETAPE_ORDRE = a;
      }
    }

    return alerts;
  });

  /** SF-225-02 : tooltip lisible pour le popover (alignement F-IM-05). */
  alertTooltip(alert: ReferentialDialogCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  /** SF-225-02 : libellé court du badge (sévérité-aware). */
  alertBadgeLabel(alert: ReferentialDialogCoherenceAlert): string {
    switch (alert.severity) {
      case 'CRITICAL': return `Alerte critique : ${alert.expectedDisplay}`;
      case 'INFO':     return `Note : ${alert.expectedDisplay}`;
      case 'WARNING':
      default:         return `Valeur inhabituelle : ${alert.expectedDisplay}`;
    }
  }

  // ---- FormArray getters (pour le template) -------------------------------

  get macronEntries(): FormArray  { return this.form.get('macronEntries') as FormArray; }
  get convCongesSupp(): FormArray { return this.form.get('convCongesSupp') as FormArray; }
  get convPrimes(): FormArray     { return this.form.get('convPrimes') as FormArray; }
  get jalons(): FormArray         { return this.form.get('jalons') as FormArray; }
  // SF-225-01 : FormArray pour les jalons procéduraux étendus (label, offsetDays, articleRef)
  get procedureJalons(): FormArray { return this.form.get('procedureJalons') as FormArray; }

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

  // SF-225-01 : add/remove pour TRAVAIL_PROCEDURE_JALONS et FAMILLE_PROCEDURE_JALONS
  addProcedureJalon(): void {
    this.procedureJalons.push(this.buildProcedureJalonRow({ label: '', offsetDays: 0, articleRef: '' }));
  }
  removeProcedureJalon(i: number): void { this.procedureJalons.removeAt(i); }

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

      // ----- SF-225-01 : 5 types orphelins -----

      case 'CONVENTION_PREAVIS': {
        // Structure : {fonctions: {OUVRIER:[{min,max,mois}], ...}, article: "..."}
        // Édition de la matrice fonctions × tranches en JSON brut + champ article séparé
        // (la structure est trop imbriquée pour un FormArray exhaustif simple).
        const fonctionsJson = parsed?.fonctions
          ? JSON.stringify(parsed.fonctions, null, 2)
          : data.entry.valueJson;
        return this.fb.group({
          ...base,
          preavisArticle:  [parsed?.article ?? '', [Validators.required, Validators.maxLength(200)]],
          preavisFonctions: [fonctionsJson, [Validators.required, this.jsonValidator]],
        });
      }

      case 'TRAVAIL_PROCEDURE_JALONS':
      case 'FAMILLE_PROCEDURE_JALONS': {
        if (!Array.isArray(parsed)) {
          return this.fb.group({
            ...base,
            valueJson: [data.entry.valueJson, [Validators.required, this.jsonValidator]],
          });
        }
        return this.fb.group({
          ...base,
          procedureJalons: this.fb.array(
            parsed.map((j: any) => this.buildProcedureJalonRow(j ?? {})),
            [Validators.required, minArrayLength(1)]
          ),
        });
      }

      case 'MAJEURS_PROTEGES_REGIMES':
        return this.fb.group({
          ...base,
          mpDelaiProcedure:  [parsed?.delaiProcedureMois ?? 0, [Validators.required, Validators.min(0), Validators.max(60)]],
          mpDureeInitiale:   [parsed?.delaiInitialAnsMax ?? 5, [Validators.required, Validators.min(1), Validators.max(20)]],
          mpRenouvelable:    [parsed?.renouvelable ?? true],
          mpArticles:        [Array.isArray(parsed?.articles) ? parsed.articles.join('\n') : '', Validators.required],
          mpCriteres:        [Array.isArray(parsed?.criteresEligibilite) ? parsed.criteresEligibilite.join('\n') : '', Validators.required],
        });

      case 'IM21_VALIDITY_CRITERES':
        return this.fb.group({
          ...base,
          im21Binaire:     [parsed?.binaire ?? true],
          im21Description: [parsed?.description ?? '', [Validators.required, Validators.maxLength(1000)]],
        });

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

  /** SF-225-01 : row builder pour TRAVAIL_PROCEDURE_JALONS / FAMILLE_PROCEDURE_JALONS (label, offsetDays, articleRef). */
  private buildProcedureJalonRow(j: any): FormGroup {
    return this.fb.group({
      label:      [j?.label ?? '', [Validators.required, Validators.maxLength(200)]],
      offsetDays: [j?.offsetDays ?? 0, [Validators.required, Validators.min(0), Validators.max(1825)]],
      articleRef: [j?.articleRef ?? '', [Validators.maxLength(200)]],
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

      // ----- SF-225-01 : 5 types orphelins -----

      case 'CONVENTION_PREAVIS': {
        // On préserve les autres champs éventuels du JSON original (forward-compat).
        let origParsed: any = {};
        try { origParsed = JSON.parse(this.data.entry.valueJson); } catch { /* ignore */ }
        let fonctions: any = origParsed.fonctions ?? {};
        try { fonctions = JSON.parse(v.preavisFonctions as string); } catch { /* keep original */ }
        return JSON.stringify({
          ...origParsed,
          fonctions,
          article: v.preavisArticle,
        });
      }

      case 'TRAVAIL_PROCEDURE_JALONS':
      case 'FAMILLE_PROCEDURE_JALONS': {
        if (Array.isArray(v.procedureJalons)) {
          const jalons = (v.procedureJalons as any[]).map(j => {
            const obj: { label: string; offsetDays: number; articleRef?: string } = {
              label: j.label,
              offsetDays: Number(j.offsetDays),
            };
            const ref = (j.articleRef ?? '').toString().trim();
            if (ref.length > 0) obj.articleRef = ref;
            return obj;
          });
          return JSON.stringify(jalons);
        }
        return v.valueJson;
      }

      case 'MAJEURS_PROTEGES_REGIMES':
        return JSON.stringify({
          articles: splitLines(v.mpArticles as string),
          delaiProcedureMois: Number(v.mpDelaiProcedure),
          delaiInitialAnsMax: Number(v.mpDureeInitiale),
          renouvelable: !!v.mpRenouvelable,
          criteresEligibilite: splitLines(v.mpCriteres as string),
        });

      case 'IM21_VALIDITY_CRITERES':
        return JSON.stringify({
          binaire: !!v.im21Binaire,
          description: v.im21Description,
        });

      default:
        return v.valueJson;
    }
  }

  submit(): void {
    if (this.form.invalid) return;
    const raw = this.form.getRawValue();
    this.dialogRef.close({
      label:       raw.label,
      valueJson:   this.serializeValueJson(),
      force:       false,
      description: (raw.description ?? '').trim(),
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
