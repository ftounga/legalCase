import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { HttpParams } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AuthService } from '../../core/services/auth.service';
import { fadeInUp } from '../../shared/animations';

import { ProspectBootstrapService } from './prospect-bootstrap.service';
import {
  ProspectBootstrapErrorBody,
  ProspectBootstrapRequest,
  ProspectBootstrapResponse,
  ProspectCountry,
  ProspectLegalDomain,
} from './prospect-bootstrap.model';

/**
 * F-251 SF-251-04 — Écran super-admin « Bootstrap prospect ».
 *
 * <p>Formulaire 7 champs qui consomme {@code POST /api/v1/super-admin/prospect-bootstrap}
 * (SF-251-03) pour provisionner en un clic un compte prospect (user + workspace
 * + période d'évaluation). Particulièrement utile en plein milieu d'une démo
 * où chaque seconde compte — remplace un curl manuel auparavant exécuté en
 * console.</p>
 *
 * <p>Décisions produit notables :</p>
 * <ul>
 *   <li>Le champ {@code password} est en {@code type=text} visible — l'opérateur
 *       doit pouvoir le dicter au téléphone au prospect. Masquer ajoute de la
 *       friction sans valeur (l'opérateur doit déjà partager le mot de passe).</li>
 *   <li>Aucune redirection automatique post-succès — l'opérateur reste sur la
 *       page pour copier les infos dans son mail de bienvenue.</li>
 *   <li>Sur 409 conflict (compte déjà actif), un lien direct vers
 *       {@code /super-admin/workspaces?email=...} est proposé pour vérifier
 *       le workspace existant.</li>
 * </ul>
 */
@Component({
  selector: 'app-prospect-bootstrap-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './prospect-bootstrap-page.component.html',
  styleUrl: './prospect-bootstrap-page.component.scss',
  animations: [fadeInUp],
  host: { '[@fadeInUp]': '' },
})
export class ProspectBootstrapPageComponent implements OnInit {
  // Dépendances — pattern inject() Angular 19
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly bootstrapService = inject(ProspectBootstrapService);
  private readonly cdr = inject(ChangeDetectorRef);

  // Signal-based state
  readonly submitting = signal(false);
  /** Résultat du dernier bootstrap réussi — utilisé par le panneau résumé. */
  readonly lastSuccess = signal<ProspectBootstrapResponse | null>(null);
  /** Email du dernier bootstrap réussi — utilisé pour pré-filtrer le lien vers /super-admin/workspaces. */
  readonly lastSuccessEmail = signal<string | null>(null);

  readonly countries: ReadonlyArray<{ value: ProspectCountry; label: string }> = [
    { value: 'FRANCE', label: 'France' },
    { value: 'BELGIQUE', label: 'Belgique' },
  ];

  readonly legalDomains: ReadonlyArray<{ value: ProspectLegalDomain; label: string }> = [
    { value: 'DROIT_DU_TRAVAIL', label: 'Droit du travail' },
    { value: 'DROIT_IMMOBILIER', label: 'Droit immobilier' },
    { value: 'DROIT_DE_LA_FAMILLE', label: 'Droit de la famille' },
  ];

  /**
   * Formulaire de bootstrap — 7 champs, valeurs initiales selon mini-spec
   * (FRANCE + DROIT_DU_TRAVAIL pré-sélectionnés, autres champs vides).
   */
  readonly form: FormGroup = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(320)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
    country: ['FRANCE' as ProspectCountry, [Validators.required]],
    legalDomain: ['DROIT_DU_TRAVAIL' as ProspectLegalDomain, [Validators.required]],
    workspaceName: ['', [Validators.required, Validators.maxLength(100)]],
  });

  ngOnInit(): void {
    if (!this.auth.currentUser()?.isSuperAdmin) {
      this.router.navigate(['/case-files']);
    }
  }

  // ---------------------------------------------------------------------------
  // Public actions
  // ---------------------------------------------------------------------------

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    this.submitting.set(true);

    const req = this.buildRequest();
    this.bootstrapService.bootstrap(req).subscribe({
      next: (response: ProspectBootstrapResponse) => {
        this.submitting.set(false);
        this.lastSuccess.set(response);
        this.lastSuccessEmail.set(req.email);
        const formattedDate = this.formatDate(response.expiresAt);
        this.snackBar.open(
          `Compte bootstrappé — workspace ${response.workspaceName} (expire le ${formattedDate})`,
          'Fermer',
          { duration: 6000, panelClass: ['snack-success'] },
        );
        this.cdr.markForCheck();
      },
      error: (err: HttpErrorLike) => {
        this.submitting.set(false);
        this.handleError(err, req);
        this.cdr.markForCheck();
      },
    });
  }

  /** Bouton « Bootstrap un autre prospect » — reset complet du formulaire et du panneau résumé. */
  resetForm(): void {
    this.form.reset({
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      country: 'FRANCE' as ProspectCountry,
      legalDomain: 'DROIT_DU_TRAVAIL' as ProspectLegalDomain,
      workspaceName: '',
    });
    this.lastSuccess.set(null);
    this.lastSuccessEmail.set(null);
    this.cdr.markForCheck();
  }

  /** URL pré-filtrée vers la liste workspaces super-admin (filtre email). */
  workspacesUrlForEmail(email: string | null): string {
    if (!email) return '/super-admin/workspaces';
    const params = new HttpParams().set('email', email);
    return `/super-admin/workspaces?${params.toString()}`;
  }

  // ---------------------------------------------------------------------------
  // Private helpers
  // ---------------------------------------------------------------------------

  private buildRequest(): ProspectBootstrapRequest {
    const raw = this.form.getRawValue() as ProspectBootstrapRequest;
    return {
      firstName: (raw.firstName ?? '').trim(),
      lastName: (raw.lastName ?? '').trim(),
      email: (raw.email ?? '').trim(),
      password: raw.password ?? '',
      country: raw.country,
      legalDomain: raw.legalDomain,
      workspaceName: (raw.workspaceName ?? '').trim(),
    };
  }

  private handleError(err: HttpErrorLike, req: ProspectBootstrapRequest): void {
    const status = err?.status;
    const body: ProspectBootstrapErrorBody = err?.error ?? {};

    if (status === 401) {
      // L'intercepteur global redirige vers /login — pas d'action UI ici.
      return;
    }
    if (status === 403) {
      this.snackBar.open(
        'Accès refusé — super-admin requis.',
        'Fermer',
        { duration: 5000, panelClass: ['snack-error'] },
      );
      return;
    }
    if (status === 409) {
      // Compte déjà actif — message + lien vers /super-admin/workspaces?email=...
      const url = this.workspacesUrlForEmail(req.email);
      const baseMessage = body.message ?? 'Ce compte est déjà actif — bootstrap refusé.';
      this.snackBar.open(
        `${baseMessage} Voir : ${url}`,
        'Fermer',
        { duration: 8000, panelClass: ['snack-warning'] },
      );
      return;
    }
    if (status === 400) {
      const message = body.message ?? 'Données invalides — vérifier les champs.';
      this.snackBar.open(message, 'Fermer', {
        duration: 6000,
        panelClass: ['snack-error'],
      });
      // mat-error inline sur le champ identifié par `field` quand présent.
      if (body.field) {
        const control: AbstractControl | null = this.form.get(body.field);
        if (control) {
          control.setErrors({ backend: message });
          control.markAsTouched();
        }
      }
      return;
    }
    // Erreur réseau ou statut inattendu
    this.snackBar.open(
      'Erreur réseau — réessayer.',
      'Fermer',
      { duration: 5000, panelClass: ['snack-error'] },
    );
  }

  private formatDate(iso: string): string {
    if (!iso) return '';
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return iso;
    const dd = String(d.getDate()).padStart(2, '0');
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const yyyy = d.getFullYear();
    return `${dd}/${mm}/${yyyy}`;
  }
}

/**
 * Forme minimaliste d'une erreur HttpClient sérialisée par le backend
 * ({@code error: { message, field?, userId? }}). Aligné sur la convention
 * SF-251-03.
 */
interface HttpErrorLike {
  status?: number;
  error?: ProspectBootstrapErrorBody;
}
