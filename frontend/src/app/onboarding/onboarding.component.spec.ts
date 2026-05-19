import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { OnboardingComponent } from './onboarding.component';
import { WorkspaceService } from '../core/services/workspace.service';
import { LegalConsentService } from '../core/services/legal-consent.service';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule } from '@angular/router';
import { AnalyticsService } from '../core/services/analytics.service';

describe('OnboardingComponent', () => {
  let fixture: ComponentFixture<OnboardingComponent>;
  let component: OnboardingComponent;
  let workspaceService: jest.Mocked<WorkspaceService>;
  let legalConsentService: jest.Mocked<LegalConsentService>;
  let router: jest.Mocked<Router>;
  let snackBar: jest.Mocked<MatSnackBar>;
  let dialog: jest.Mocked<MatDialog>;
  let dialogRefSpy: jest.Mocked<MatDialogRef<any>>;
  let analyticsSpy: jest.Mocked<AnalyticsService>;

  beforeEach(async () => {
    workspaceService = jasmine.createSpyObj('WorkspaceService', ['createWorkspace']);
    legalConsentService = jasmine.createSpyObj('LegalConsentService', ['acceptConsent']);
    router = jasmine.createSpyObj('Router', ['navigate']);
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
    dialogRefSpy.afterClosed.mockReturnValue(of({ legalDomain: 'DROIT_DU_TRAVAIL', country: 'FRANCE' }));
    dialog = jasmine.createSpyObj('MatDialog', ['open']);
    dialog.open.mockReturnValue(dialogRefSpy);
    analyticsSpy = jasmine.createSpyObj('AnalyticsService', ['trackConversion', 'trackEvent', 'captureUtmParams']);

    await TestBed.configureTestingModule({
      imports: [OnboardingComponent, NoopAnimationsModule, RouterModule.forRoot([])],
      providers: [
        { provide: WorkspaceService, useValue: workspaceService },
        { provide: LegalConsentService, useValue: legalConsentService },
        { provide: Router, useValue: router },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialog, useValue: dialog },
        { provide: AnalyticsService, useValue: analyticsSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OnboardingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // OB-01 : checkbox initialement non cochée → bouton désactivé (même si name valide)
  it('OB-01 : checkbox initialement non cochée → formulaire invalide, bouton désactivé', () => {
    component.form.get('name')!.setValue('Cabinet Martin');
    component.form.get('acceptedTerms')!.setValue(false);
    fixture.detectChanges();

    expect(component.form.invalid).toBe(true);
    expect(component.form.get('acceptedTerms')!.valid).toBe(false);
  });

  // OB-02 : name valide + checkbox cochée → formulaire valide, bouton actif
  it('OB-02 : name valide + checkbox cochée → formulaire valide', () => {
    component.form.get('name')!.setValue('Cabinet Martin');
    component.form.get('acceptedTerms')!.setValue(true);
    fixture.detectChanges();

    expect(component.form.valid).toBe(true);
  });

  // OB-03 : submit() appelle consentService AVANT workspaceService
  it('OB-03 : submit() appelle acceptConsent AVANT createWorkspace', fakeAsync(() => {
    const callOrder: string[] = [];
    legalConsentService.acceptConsent.mockImplementation(() => {
      callOrder.push('consent');
      return of({ acceptances: [] });
    });
    workspaceService.createWorkspace.mockImplementation(() => {
      callOrder.push('workspace');
      return of({} as any);
    });

    component.form.setValue({ name: 'Cabinet Martin', acceptedTerms: true });
    component.submit();
    tick();

    expect(callOrder[0]).toBe('consent');
    expect(callOrder[1]).toBe('workspace');
  }));

  // OB-04 : consentService échoue → workspaceService non appelé + snackBar d'erreur
  it('OB-04 : acceptConsent échoue → createWorkspace non appelé + snackBar erreur', fakeAsync(() => {
    legalConsentService.acceptConsent.mockReturnValue(throwError(() => ({ status: 500 })));

    component.form.setValue({ name: 'Cabinet Martin', acceptedTerms: true });
    component.submit();
    tick();

    expect(workspaceService.createWorkspace).not.toHaveBeenCalled();
    expect(dialog.open).not.toHaveBeenCalled();
    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('Impossible d\'enregistrer votre acceptation'),
      expect.any(String),
      expect.any(Object)
    );
    expect(component.saving).toBe(false);
  }));

  // OB-04b : consentService 400 → message générique snackBar
  it('OB-04b : acceptConsent 400 → snackBar message générique', fakeAsync(() => {
    legalConsentService.acceptConsent.mockReturnValue(throwError(() => ({ status: 400 })));

    component.form.setValue({ name: 'Cabinet Martin', acceptedTerms: true });
    component.submit();
    tick();

    expect(workspaceService.createWorkspace).not.toHaveBeenCalled();
    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('Une erreur est survenue'),
      expect.any(String),
      expect.any(Object)
    );
    expect(component.saving).toBe(false);
  }));

  // OB-05 : acceptConsent OK mais createWorkspace échoue → consent enregistré (pas de rollback)
  it('OB-05 : consent OK mais createWorkspace échoue → snackBar erreur workspace, consent non rollbacké', fakeAsync(() => {
    legalConsentService.acceptConsent.mockReturnValue(of({ acceptances: [] }));
    workspaceService.createWorkspace.mockReturnValue(throwError(() => ({ status: 500 })));

    component.form.setValue({ name: 'Mon Cabinet', acceptedTerms: true });
    component.submit();
    tick();

    expect(legalConsentService.acceptConsent).toHaveBeenCalledTimes(1);
    expect(workspaceService.createWorkspace).toHaveBeenCalledTimes(1);
    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('Erreur'), expect.any(String), expect.any(Object)
    );
    expect(component.saving).toBe(false);
  }));

  // OB-06 : non-régression — soumission réussie complète (consent + workspace + redirect)
  it('OB-06 : soumission complète consent OK + workspace OK → redirect /case-files', fakeAsync(() => {
    legalConsentService.acceptConsent.mockReturnValue(of({ acceptances: [] }));
    workspaceService.createWorkspace.mockReturnValue(of({} as any));

    component.form.setValue({ name: 'Cabinet Martin', acceptedTerms: true });
    component.submit();
    tick();

    expect(dialog.open).toHaveBeenCalled();
    expect(workspaceService.createWorkspace).toHaveBeenCalledWith('CABINET MARTIN', 'DROIT_DU_TRAVAIL', 'FRANCE');
    expect(router.navigate).toHaveBeenCalledWith(['/case-files']);
  }));

  // Non-régression T-SF119 : onboarding réussi → trackConversion appelé
  it('onboarding réussi → appelle trackConversion()', fakeAsync(() => {
    legalConsentService.acceptConsent.mockReturnValue(of({ acceptances: [] }));
    workspaceService.createWorkspace.mockReturnValue(of({} as any));

    component.form.setValue({ name: 'Cabinet Martin', acceptedTerms: true });
    component.submit();
    tick();

    expect(analyticsSpy.trackConversion).toHaveBeenCalledTimes(1);
  }));

  // Non-régression T-02 : soumission avec nom vide → invalide, pas d'appel
  it('soumission avec nom vide → formulaire invalide, pas d\'appel HTTP', () => {
    component.form.setValue({ name: '', acceptedTerms: true });
    component.form.get('name')!.markAsTouched();
    component.submit();

    expect(legalConsentService.acceptConsent).not.toHaveBeenCalled();
    expect(dialog.open).not.toHaveBeenCalled();
    expect(workspaceService.createWorkspace).not.toHaveBeenCalled();
  });

  // Non-régression T-03 : erreur réseau workspace → snackbar erreur, saving = false
  it('erreur réseau workspace → snackbar erreur affiché, bouton réactivé', fakeAsync(() => {
    legalConsentService.acceptConsent.mockReturnValue(of({ acceptances: [] }));
    workspaceService.createWorkspace.mockReturnValue(throwError(() => ({ status: 500 })));

    component.form.setValue({ name: 'Mon Cabinet', acceptedTerms: true });
    component.submit();
    tick();

    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('Erreur'), expect.any(String), expect.any(Object)
    );
    expect(component.saving).toBe(false);
  }));
});
