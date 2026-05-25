import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';

import { ProspectBootstrapPageComponent } from './prospect-bootstrap-page.component';
import { ProspectBootstrapService } from './prospect-bootstrap.service';
import { ProspectBootstrapResponse } from './prospect-bootstrap.model';
import { AuthService } from '../../core/services/auth.service';
import { User } from '../../core/models/user.model';

const mockSuperAdmin: User = {
  id: 'u-super',
  email: 'admin@test.com',
  firstName: 'Franck',
  lastName: 'Tounga',
  provider: 'GOOGLE',
  isSuperAdmin: true,
};

const mockSuccess: ProspectBootstrapResponse = {
  userId: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
  workspaceId: 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
  workspaceName: 'RENVERSEZ-MARJOLAINE',
  expiresAt: '2026-06-08T23:59:59Z',
};

/** Valeurs nominales valides — utilisées par les tests qui doivent passer la validation. */
const VALID_FORM = {
  firstName: 'Marjolaine',
  lastName: 'RENVERSEZ',
  email: 'avocat@renversez.com',
  password: 'printemps2026',
  country: 'FRANCE',
  legalDomain: 'DROIT_DU_TRAVAIL',
  workspaceName: 'RENVERSEZ-MARJOLAINE',
};

describe('ProspectBootstrapPageComponent', () => {
  let component: ProspectBootstrapPageComponent;
  let fixture: ComponentFixture<ProspectBootstrapPageComponent>;
  let bootstrapService: jest.Mocked<ProspectBootstrapService>;
  let snackBar: jest.Mocked<MatSnackBar>;
  let router: Router;

  function setup(opts: {
    user?: User | null;
    bootstrapReturn?: any;
  } = {}) {
    bootstrapService = jasmine.createSpyObj('ProspectBootstrapService', ['bootstrap']);
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);

    if (opts.bootstrapReturn) {
      bootstrapService.bootstrap.mockReturnValue(opts.bootstrapReturn);
    }

    const userSignal = signal<User | null>(
      opts.user === undefined ? mockSuperAdmin : opts.user,
    );
    const authStub: Partial<AuthService> = { currentUser: userSignal };

    TestBed.configureTestingModule({
      imports: [ProspectBootstrapPageComponent, NoopAnimationsModule],
      providers: [
        { provide: ProspectBootstrapService, useValue: bootstrapService },
        { provide: AuthService, useValue: authStub },
        { provide: MatSnackBar, useValue: snackBar },
        provideRouter([]),
      ],
    });

    fixture = TestBed.createComponent(ProspectBootstrapPageComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    jest.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
  }

  // ---------------------------------------------------------------------------
  // T-01 : rendu initial — 7 champs visibles, valeurs par défaut FR + DT
  // ---------------------------------------------------------------------------
  it('shouldRenderForm — affiche les 7 champs du formulaire avec valeurs initiales', fakeAsync(() => {
    setup();

    const root: HTMLElement = fixture.nativeElement;
    expect(root.querySelector('[data-testid="first-name-input"]')).not.toBeNull();
    expect(root.querySelector('[data-testid="last-name-input"]')).not.toBeNull();
    expect(root.querySelector('[data-testid="email-input"]')).not.toBeNull();
    expect(root.querySelector('[data-testid="password-input"]')).not.toBeNull();
    expect(root.querySelector('[data-testid="country-select"]')).not.toBeNull();
    expect(root.querySelector('[data-testid="legal-domain-select"]')).not.toBeNull();
    expect(root.querySelector('[data-testid="workspace-name-input"]')).not.toBeNull();

    expect(component.form.get('country')?.value).toBe('FRANCE');
    expect(component.form.get('legalDomain')?.value).toBe('DROIT_DU_TRAVAIL');
    expect(component.form.get('firstName')?.value).toBe('');

    // Le password est en input type=text (visible, pas masqué)
    const passwordInput = root.querySelector('[data-testid="password-input"]') as HTMLInputElement;
    expect(passwordInput.type).toBe('text');
  }));

  // ---------------------------------------------------------------------------
  // T-02 : bouton submit désactivé tant que le formulaire est invalide
  // ---------------------------------------------------------------------------
  it('shouldDisableSubmitWhenFormInvalid — le bouton est disabled si la validation échoue', fakeAsync(() => {
    setup();

    const submitBtn = fixture.nativeElement.querySelector('[data-testid="submit-button"]') as HTMLButtonElement;
    expect(submitBtn.disabled).toBe(true);
    expect(component.form.invalid).toBe(true);

    // On remplit avec des valeurs valides — le bouton devient cliquable
    component.form.patchValue(VALID_FORM);
    fixture.detectChanges();

    expect(component.form.valid).toBe(true);
    expect(submitBtn.disabled).toBe(false);

    // Email invalide → re-disabled
    component.form.patchValue({ email: 'pas-un-email' });
    fixture.detectChanges();
    expect(submitBtn.disabled).toBe(true);

    // Password trop court → re-disabled
    component.form.patchValue({ email: 'ok@ok.com', password: 'short' });
    fixture.detectChanges();
    expect(submitBtn.disabled).toBe(true);
  }));

  // ---------------------------------------------------------------------------
  // T-03 : succès 201 → snackbar succès + panneau résumé affiché
  // ---------------------------------------------------------------------------
  it('shouldSubmitValidFormAndDisplaySuccess — POST 201, snackbar succès + panneau résumé', fakeAsync(() => {
    setup({ bootstrapReturn: of(mockSuccess) });
    component.form.patchValue(VALID_FORM);

    component.submit();
    tick();
    fixture.detectChanges();

    expect(bootstrapService.bootstrap).toHaveBeenCalledTimes(1);
    const sent = bootstrapService.bootstrap.mock.calls[0][0];
    expect(sent.firstName).toBe('Marjolaine');
    expect(sent.email).toBe('avocat@renversez.com');
    expect(sent.country).toBe('FRANCE');
    expect(sent.legalDomain).toBe('DROIT_DU_TRAVAIL');

    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('RENVERSEZ-MARJOLAINE'),
      'Fermer',
      expect.objectContaining({ panelClass: ['snack-success'] }),
    );
    // Le message doit aussi contenir la date formatée en local time (jj/mm/aaaa).
    // On calcule dynamiquement la date attendue pour rester indépendant du fuseau
    // horaire de la machine de CI (UTC vs CEST décalent la date du 08 au 09 juin).
    const expectedDate = new Date(mockSuccess.expiresAt);
    const expectedDateStr = `${String(expectedDate.getDate()).padStart(2, '0')}/${String(expectedDate.getMonth() + 1).padStart(2, '0')}/${expectedDate.getFullYear()}`;
    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining(expectedDateStr),
      'Fermer',
      expect.anything(),
    );

    expect(component.lastSuccess()).toEqual(mockSuccess);
    expect(component.lastSuccessEmail()).toBe('avocat@renversez.com');

    const panel = fixture.nativeElement.querySelector('[data-testid="success-panel"]');
    expect(panel).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="success-user-id"]')?.textContent)
      .toContain(mockSuccess.userId);
    expect(fixture.nativeElement.querySelector('[data-testid="success-workspace-id"]')?.textContent)
      .toContain(mockSuccess.workspaceId);
    expect(fixture.nativeElement.querySelector('[data-testid="success-expires-at"]')?.textContent)
      .toContain(expectedDateStr);

    // Le lien « Voir le workspace » est pré-filtré sur l'email
    const link = fixture.nativeElement.querySelector('[data-testid="view-workspace-link"]') as HTMLAnchorElement;
    expect(link).not.toBeNull();
    // HttpParams n'encode pas @ (RFC 3986 sub-delim), on accepte la forme brute.
    expect(link.getAttribute('href')).toContain('email=avocat@renversez.com');
  }));

  // ---------------------------------------------------------------------------
  // T-04 : erreur 409 → snackbar warning avec lien vers /super-admin/workspaces
  // ---------------------------------------------------------------------------
  it('shouldDisplay409ConflictWarning — snackbar warning + URL workspaces pré-filtrée', fakeAsync(() => {
    setup({
      bootstrapReturn: throwError(() => ({
        status: 409,
        error: {
          message: 'Ce compte est déjà actif (3 dossiers).',
          userId: 'existing-user-uuid',
        },
      })),
    });
    component.form.patchValue(VALID_FORM);

    component.submit();
    tick();
    fixture.detectChanges();

    expect(snackBar.open).toHaveBeenCalledTimes(1);
    const callArgs = snackBar.open.mock.calls[0];
    expect(callArgs[0]).toContain('Ce compte est déjà actif');
    expect(callArgs[0]).toContain('/super-admin/workspaces');
    expect(callArgs[0]).toContain('email=avocat');
    expect(callArgs[2]).toEqual(expect.objectContaining({ panelClass: ['snack-warning'] }));

    // Pas de panneau succès affiché
    expect(component.lastSuccess()).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="success-panel"]')).toBeNull();
    expect(component.submitting()).toBe(false);
  }));

  // ---------------------------------------------------------------------------
  // T-05 : erreur 400 → snackbar erreur + mat-error inline sur le champ pointé
  // ---------------------------------------------------------------------------
  it('shouldDisplay400ValidationError — snackbar erreur + mat-error sur le champ identifié', fakeAsync(() => {
    setup({
      bootstrapReturn: throwError(() => ({
        status: 400,
        error: {
          message: "Email déjà utilisé pour un compte différent.",
          field: 'email',
        },
      })),
    });
    component.form.patchValue(VALID_FORM);

    component.submit();
    tick();
    fixture.detectChanges();

    expect(snackBar.open).toHaveBeenCalledWith(
      'Email déjà utilisé pour un compte différent.',
      'Fermer',
      expect.objectContaining({ panelClass: ['snack-error'] }),
    );

    const emailControl = component.form.get('email');
    expect(emailControl?.errors?.['backend']).toBe('Email déjà utilisé pour un compte différent.');
    expect(emailControl?.touched).toBe(true);
    expect(component.submitting()).toBe(false);
  }));

  // ---------------------------------------------------------------------------
  // T-06 : bouton « Bootstrap un autre » → reset complet du formulaire
  // ---------------------------------------------------------------------------
  it('shouldResetFormAfterSuccess — le bouton reset remet le formulaire et le panneau à l\'état initial', fakeAsync(() => {
    setup({ bootstrapReturn: of(mockSuccess) });
    component.form.patchValue(VALID_FORM);

    component.submit();
    tick();
    fixture.detectChanges();

    expect(component.lastSuccess()).not.toBeNull();
    expect(component.form.get('firstName')?.value).toBe('Marjolaine');

    component.resetForm();
    fixture.detectChanges();

    expect(component.lastSuccess()).toBeNull();
    expect(component.lastSuccessEmail()).toBeNull();
    expect(component.form.get('firstName')?.value).toBe('');
    expect(component.form.get('email')?.value).toBe('');
    expect(component.form.get('password')?.value).toBe('');
    // Les valeurs par défaut FRANCE + DROIT_DU_TRAVAIL sont restaurées
    expect(component.form.get('country')?.value).toBe('FRANCE');
    expect(component.form.get('legalDomain')?.value).toBe('DROIT_DU_TRAVAIL');
    // Le panneau résumé n'est plus dans le DOM
    expect(fixture.nativeElement.querySelector('[data-testid="success-panel"]')).toBeNull();
  }));

  // ---------------------------------------------------------------------------
  // T-07 (sécurité) : non super-admin → redirection /case-files
  // ---------------------------------------------------------------------------
  it('redirige vers /case-files si l\'utilisateur n\'est pas super-admin', fakeAsync(() => {
    setup({ user: { ...mockSuperAdmin, isSuperAdmin: false } });
    expect(router.navigate).toHaveBeenCalledWith(['/case-files']);
  }));
});
