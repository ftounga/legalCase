import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { signal } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

import { TractionOnePagerComponent } from './traction-onepager.component';
import { TractionOnePagerService } from './traction-onepager.service';
import { SuperAdminService } from '../../core/services/super-admin.service';
import { AuthService } from '../../core/services/auth.service';
import { SuperAdminMetrics } from '../../core/models/super-admin.model';
import { User } from '../../core/models/user.model';

const mockMetrics: SuperAdminMetrics = {
  totalWorkspaces: 12,
  activeWorkspaces30d: 9,
  inactiveWorkspaces30d: 3,
  trialWorkspaces: 5,
  paidWorkspaces: 7,
  conversionRatePct: 58.3,
  analysesLast7Days: 42,
  analysesLast30Days: 156,
  newWorkspacesLast30Days: 4,
};

const mockSuperAdminUser: User = {
  id: 'u-1',
  email: 'admin@test.com',
  firstName: 'Franck',
  lastName: 'Tounga',
  provider: 'GOOGLE',
  isSuperAdmin: true,
};

describe('TractionOnePagerComponent', () => {
  let component: TractionOnePagerComponent;
  let fixture: ComponentFixture<TractionOnePagerComponent>;
  let superAdminService: jest.Mocked<SuperAdminService>;
  let tractionService: jest.Mocked<TractionOnePagerService>;
  let snackBar: jest.Mocked<MatSnackBar>;
  let router: Router;

  function setup(opts: {
    metricsReturn?: any;
    pdfReturn?: any;
    user?: User | null;
  } = {}) {
    superAdminService = jasmine.createSpyObj('SuperAdminService', [
      'listWorkspaces', 'getUsage', 'listUsers', 'deleteWorkspace', 'deleteUser',
      'getMetrics', 'getPipelineHealth',
    ]);
    tractionService = jasmine.createSpyObj('TractionOnePagerService', [
      'generatePdf', 'triggerDownload', 'buildFilename',
    ]);
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);

    superAdminService.getMetrics.mockReturnValue(opts.metricsReturn ?? of(mockMetrics));
    if (opts.pdfReturn) {
      tractionService.generatePdf.mockReturnValue(opts.pdfReturn);
    }
    tractionService.buildFilename.mockReturnValue('legalcase-traction-2026-05-04.pdf');

    const userSignal = signal<User | null>(opts.user === undefined ? mockSuperAdminUser : opts.user);
    const authStub: Partial<AuthService> = { currentUser: userSignal };

    TestBed.configureTestingModule({
      imports: [TractionOnePagerComponent, NoopAnimationsModule],
      providers: [
        { provide: SuperAdminService, useValue: superAdminService },
        { provide: TractionOnePagerService, useValue: tractionService },
        { provide: AuthService, useValue: authStub },
        { provide: MatSnackBar, useValue: snackBar },
        provideRouter([]),
      ],
    });

    fixture = TestBed.createComponent(TractionOnePagerComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    // Empêche les navigations de tenter de résoudre une URL réelle (router stub vide)
    jest.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
  }

  // T-01 — KPIs affichés au chargement (mock SuperAdminMetricsService)
  it('T-01: affiche les KPIs après chargement F-76 et pré-remplit le contact', fakeAsync(() => {
    setup();

    expect(superAdminService.getMetrics).toHaveBeenCalled();
    expect(component.metrics()).not.toBeNull();
    expect(component.kpiItems().length).toBe(7);

    // Contact pré-rempli depuis le user connecté
    expect(component.form.get('contact.name')?.value).toBe('Franck Tounga');
    expect(component.form.get('contact.email')?.value).toBe('admin@test.com');
    expect(component.form.get('contact.url')?.value).toBe('https://legalcase.fr');
  }));

  // T-02 — Formulaire invalide si headline vide → bouton désactivé
  it('T-02: form.invalid si headline vide → bouton générer désactivé', fakeAsync(() => {
    setup();
    component.form.get('headline')?.setValue('');
    expect(component.form.invalid).toBe(true);

    const btn: HTMLButtonElement = fixture.nativeElement.querySelector('.generate-btn');
    expect(btn.disabled).toBe(true);
  }));

  // T-03 — Décocher un KPI → champ correspondant false dans le body envoyé
  it('T-03: décocher un KPI met le champ à false dans le body envoyé', fakeAsync(() => {
    const fakeBlob = new Blob(['%PDF'], { type: 'application/pdf' });
    setup({ pdfReturn: of(fakeBlob) });

    component.form.get('headline')?.setValue('Headline test');
    component.includeKpis.get('paidWorkspaces')?.setValue(false);
    component.generate();
    tick();

    expect(tractionService.generatePdf).toHaveBeenCalledTimes(1);
    const sent = tractionService.generatePdf.mock.calls[0][0];
    expect(sent.includeKpis.paidWorkspaces).toBe(false);
    expect(sent.includeKpis.totalWorkspaces).toBe(true);
    expect(sent.headline).toBe('Headline test');
  }));

  // T-04 — Ajouter 2 verbatims → bouton "Ajouter" masqué
  it('T-04: après 2 verbatims, canAddVerbatim() est false', fakeAsync(() => {
    setup();
    expect(component.canAddVerbatim()).toBe(true);

    component.addVerbatim();
    component.addVerbatim();
    expect(component.verbatims.length).toBe(2);
    expect(component.canAddVerbatim()).toBe(false);

    // Tentative d'ajout au-delà : ignorée
    component.addVerbatim();
    expect(component.verbatims.length).toBe(2);
  }));

  // T-05 — Soumission valide → appel TractionOnePagerService.generatePdf + triggerDownload
  it('T-05: soumission valide appelle generatePdf et triggerDownload', fakeAsync(() => {
    const fakeBlob = new Blob(['%PDF'], { type: 'application/pdf' });
    setup({ pdfReturn: of(fakeBlob) });

    component.form.get('headline')?.setValue('Mon accroche');
    component.generate();
    tick();

    expect(tractionService.generatePdf).toHaveBeenCalledTimes(1);
    expect(tractionService.triggerDownload).toHaveBeenCalledWith(
      fakeBlob,
      'legalcase-traction-2026-05-04.pdf',
    );
    expect(component.generating()).toBe(false);
  }));

  // T-06 — Erreur 400 → MatSnackBar avec message
  it('T-06: erreur 400 affiche un snackbar avec le message d\'erreur backend', fakeAsync(() => {
    setup({ pdfReturn: throwError(() => ({ status: 400, error: { message: 'headline manquant' } })) });

    component.form.get('headline')?.setValue('valid');
    component.generate();
    tick();

    expect(snackBar.open).toHaveBeenCalled();
    const args = snackBar.open.mock.calls[snackBar.open.mock.calls.length - 1];
    expect(args[0]).toContain('Données invalides');
    expect(args[0]).toContain('headline manquant');
    expect(component.generating()).toBe(false);
  }));

  // Bonus — utilisateur non super-admin redirigé
  it('redirige vers /case-files si l\'utilisateur n\'est pas super-admin', fakeAsync(() => {
    setup({ user: { ...mockSuperAdminUser, isSuperAdmin: false } });
    // ngOnInit court-circuite → loadMetrics jamais appelé
    expect(superAdminService.getMetrics).not.toHaveBeenCalled();
  }));

  // Bonus — KPI failure → snackbar mais le formulaire reste utilisable
  it('si getMetrics échoue (500), affiche un message et garde le formulaire utilisable', fakeAsync(() => {
    setup({ metricsReturn: throwError(() => ({ status: 500 })) });

    expect(component.metricsError()).toBe(true);
    expect(component.metrics()).toBeNull();
    expect(snackBar.open).toHaveBeenCalled();

    // Le formulaire reste utilisable
    component.form.get('headline')?.setValue('headline');
    expect(component.form.get('headline')?.valid).toBe(true);
  }));
});
