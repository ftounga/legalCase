import { Component, OnInit, signal, computed } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import {
  FormArray,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../core/services/auth.service';
import { SuperAdminService } from '../../core/services/super-admin.service';
import { SuperAdminMetrics } from '../../core/models/super-admin.model';
import { TractionOnePagerService } from './traction-onepager.service';
import {
  IncludedKpis,
  TractionOnePagerInput,
  Verbatim,
} from './traction-onepager.model';
import { fadeInUp } from '../../shared/animations';

const DEFAULT_CONTACT_URL = 'https://legalcase.fr';
const MAX_VERBATIMS = 2;
const MAX_PARTNERS = 10;

interface KpiItem {
  /** Clé de la map `includeKpis` envoyée au backend. */
  key: keyof IncludedKpis;
  /** Libellé affiché dans la checkbox + en lecture seule. */
  label: string;
  /** Valeur lue depuis F-76 (formattée en chaîne pour affichage). */
  value: string;
}

@Component({
  selector: 'app-traction-onepager',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './traction-onepager.component.html',
  styleUrl: './traction-onepager.component.scss',
  animations: [fadeInUp],
  host: { '[@fadeInUp]': '' },
})
export class TractionOnePagerComponent implements OnInit {
  readonly maxVerbatims = MAX_VERBATIMS;
  readonly maxPartners = MAX_PARTNERS;

  /** KPIs F-76 chargés (null = pas encore chargé). */
  metrics = signal<SuperAdminMetrics | null>(null);
  loadingMetrics = signal(true);
  metricsError = signal(false);

  /** En cours de génération PDF. */
  generating = signal(false);

  /** Formulaire reactive principal. */
  form: FormGroup;

  /** Items KPIs dérivés des metrics — affichés en lecture seule + checkbox. */
  kpiItems = computed<KpiItem[]>(() => {
    const m = this.metrics();
    if (!m) return [];
    return [
      { key: 'totalWorkspaces',     label: 'Workspaces total',  value: this.formatNumber(m.totalWorkspaces) },
      { key: 'trialWorkspaces',     label: 'Workspaces en essai', value: this.formatNumber(m.trialWorkspaces) },
      { key: 'paidWorkspaces',      label: 'Workspaces payants', value: this.formatNumber(m.paidWorkspaces) },
      { key: 'conversionRatePct',   label: 'Taux de conversion', value: `${this.formatNumber(m.conversionRatePct, 1)} %` },
      { key: 'activeWorkspaces30d', label: 'Workspaces actifs (30j)', value: this.formatNumber(m.activeWorkspaces30d) },
      { key: 'analysesLast7Days',   label: 'Analyses (7j)', value: this.formatNumber(m.analysesLast7Days) },
      { key: 'analysesLast30Days',  label: 'Analyses (30j)', value: this.formatNumber(m.analysesLast30Days) },
    ];
  });

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private superAdminService: SuperAdminService,
    private tractionService: TractionOnePagerService,
    private router: Router,
    private snackBar: MatSnackBar,
  ) {
    this.form = this.buildForm();
  }

  ngOnInit(): void {
    if (!this.auth.currentUser()?.isSuperAdmin) {
      this.router.navigate(['/case-files']);
      return;
    }
    this.prefillContactFromUser();
    this.loadMetrics();
  }

  // -------------------------------------------------------------------------
  // FormArray helpers
  // -------------------------------------------------------------------------

  get verbatims(): FormArray { return this.form.get('verbatims') as FormArray; }
  get partners(): FormArray  { return this.form.get('partners')  as FormArray; }
  get includeKpis(): FormGroup { return this.form.get('includeKpis') as FormGroup; }

  addVerbatim(): void {
    if (this.verbatims.length >= MAX_VERBATIMS) return;
    this.verbatims.push(this.buildVerbatimGroup());
  }

  removeVerbatim(index: number): void {
    this.verbatims.removeAt(index);
  }

  addPartner(): void {
    if (this.partners.length >= MAX_PARTNERS) return;
    this.partners.push(this.fb.control('', [Validators.required, Validators.maxLength(100)]));
  }

  removePartner(index: number): void {
    this.partners.removeAt(index);
  }

  canAddVerbatim(): boolean { return this.verbatims.length < MAX_VERBATIMS; }
  canAddPartner(): boolean  { return this.partners.length < MAX_PARTNERS; }

  // -------------------------------------------------------------------------
  // Submit
  // -------------------------------------------------------------------------

  generate(): void {
    if (this.form.invalid || this.generating()) return;
    this.generating.set(true);

    const input = this.buildInput();
    this.tractionService.generatePdf(input).subscribe({
      next: (blob: Blob) => {
        const filename = this.tractionService.buildFilename();
        this.tractionService.triggerDownload(blob, filename);
        this.generating.set(false);
        this.snackBar.open('PDF généré avec succès', 'Fermer', {
          duration: 3000, panelClass: ['snack-success'],
        });
      },
      error: (err: { status?: number; error?: { message?: string } }) => {
        this.generating.set(false);
        const message = this.errorMessage(err);
        this.snackBar.open(message, 'Fermer', {
          duration: 5000, panelClass: ['snack-error'],
        });
        if (err?.status === 403) {
          this.router.navigate(['/case-files']);
        }
      },
    });
  }

  // -------------------------------------------------------------------------
  // Helpers privés
  // -------------------------------------------------------------------------

  private buildForm(): FormGroup {
    return this.fb.group({
      includeKpis: this.fb.group({
        totalWorkspaces:     this.fb.control(true),
        trialWorkspaces:     this.fb.control(true),
        paidWorkspaces:      this.fb.control(true),
        conversionRatePct:   this.fb.control(true),
        activeWorkspaces30d: this.fb.control(true),
        analysesLast7Days:   this.fb.control(true),
        analysesLast30Days:  this.fb.control(true),
      }),
      verbatims: this.fb.array([]),
      partners:  this.fb.array([]),
      headline:  this.fb.control('', [Validators.required, Validators.maxLength(200)]),
      contact:   this.fb.group({
        name:  this.fb.control('', [Validators.required, Validators.maxLength(100)]),
        email: this.fb.control('', [Validators.required, Validators.email, Validators.maxLength(200)]),
        url:   this.fb.control(DEFAULT_CONTACT_URL, [Validators.required, Validators.maxLength(300)]),
      }),
    });
  }

  private buildVerbatimGroup(): FormGroup {
    return this.fb.group({
      quote:  this.fb.control('', [Validators.required, Validators.maxLength(500)]),
      author: this.fb.control('', [Validators.required, Validators.maxLength(200)]),
    });
  }

  private prefillContactFromUser(): void {
    const user = this.auth.currentUser();
    if (!user) return;
    const fullName = [user.firstName, user.lastName].filter(Boolean).join(' ').trim();
    this.form.get('contact.name')?.setValue(fullName || user.email);
    this.form.get('contact.email')?.setValue(user.email);
    // url reste à DEFAULT_CONTACT_URL
  }

  private loadMetrics(): void {
    this.loadingMetrics.set(true);
    this.metricsError.set(false);
    this.superAdminService.getMetrics().subscribe({
      next: (m: SuperAdminMetrics) => {
        this.metrics.set(m);
        this.loadingMetrics.set(false);
      },
      error: (err: { status?: number }) => {
        this.loadingMetrics.set(false);
        this.metricsError.set(true);
        if (err?.status === 403) {
          this.router.navigate(['/case-files']);
          return;
        }
        this.snackBar.open(
          'Impossible de charger les KPIs (les valeurs seront recalculées côté serveur).',
          'Fermer',
          { duration: 4000, panelClass: ['snack-error'] },
        );
      },
    });
  }

  private buildInput(): TractionOnePagerInput {
    const raw = this.form.getRawValue() as {
      includeKpis: IncludedKpis;
      verbatims: Verbatim[];
      partners: string[];
      headline: string;
      contact: { name: string; email: string; url: string };
    };
    return {
      includeKpis: raw.includeKpis,
      verbatims: raw.verbatims.map((v: Verbatim) => ({
        quote: (v.quote ?? '').trim(),
        author: (v.author ?? '').trim(),
      })),
      partners: raw.partners.map((p: string) => (p ?? '').trim()).filter(p => p.length > 0),
      headline: (raw.headline ?? '').trim(),
      contact: {
        name: (raw.contact.name ?? '').trim(),
        email: (raw.contact.email ?? '').trim(),
        url: (raw.contact.url ?? '').trim(),
      },
    };
  }

  private errorMessage(err: { status?: number; error?: { message?: string } } | undefined): string {
    if (!err) return 'Erreur de génération PDF, réessayez.';
    if (err.status === 400) {
      return `Données invalides : ${err.error?.message ?? 'vérifiez le formulaire'}`;
    }
    if (err.status === 403) return 'Accès refusé.';
    if (err.status === 401) return 'Session expirée, reconnectez-vous.';
    return 'Erreur de génération PDF, réessayez.';
  }

  private formatNumber(value: number, decimals = 0): string {
    if (value === null || value === undefined || Number.isNaN(value)) return '—';
    return new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: decimals,
      maximumFractionDigits: decimals,
    }).format(value);
  }
}
