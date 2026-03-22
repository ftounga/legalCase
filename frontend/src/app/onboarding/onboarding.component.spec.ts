import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { OnboardingComponent } from './onboarding.component';
import { WorkspaceService } from '../core/services/workspace.service';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule } from '@angular/router';

describe('OnboardingComponent', () => {
  let fixture: ComponentFixture<OnboardingComponent>;
  let component: OnboardingComponent;
  let workspaceService: jasmine.SpyObj<WorkspaceService>;
  let router: jasmine.SpyObj<Router>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let dialogRefSpy: jasmine.SpyObj<MatDialogRef<any>>;

  beforeEach(async () => {
    workspaceService = jasmine.createSpyObj('WorkspaceService', ['createWorkspace']);
    router = jasmine.createSpyObj('Router', ['navigate']);
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
    dialogRefSpy.afterClosed.and.returnValue(of('DROIT_DU_TRAVAIL'));
    dialog = jasmine.createSpyObj('MatDialog', ['open']);
    dialog.open.and.returnValue(dialogRefSpy);

    await TestBed.configureTestingModule({
      imports: [OnboardingComponent, NoopAnimationsModule, RouterModule.forRoot([])],
      providers: [
        { provide: WorkspaceService, useValue: workspaceService },
        { provide: Router, useValue: router },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialog, useValue: dialog }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OnboardingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // T-01 : soumission avec nom valide → dialog ouverte + createWorkspace appelé + redirect
  it('soumission avec nom valide → dialog ouverte, createWorkspace appelé avec legalDomain, redirect vers /case-files', fakeAsync(() => {
    workspaceService.createWorkspace.and.returnValue(of({} as any));

    component.form.setValue({ name: 'Cabinet Martin' });
    component.submit();
    tick();

    expect(dialog.open).toHaveBeenCalled();
    expect(workspaceService.createWorkspace).toHaveBeenCalledWith('Cabinet Martin', 'DROIT_DU_TRAVAIL');
    expect(router.navigate).toHaveBeenCalledWith(['/case-files']);
  }));

  // T-02 : soumission avec nom vide → formulaire invalide, dialog non ouverte
  it('soumission avec nom vide → formulaire invalide, pas d\'appel HTTP', () => {
    component.form.setValue({ name: '' });
    component.form.get('name')!.markAsTouched();
    component.submit();

    expect(dialog.open).not.toHaveBeenCalled();
    expect(workspaceService.createWorkspace).not.toHaveBeenCalled();
  });

  // T-03 : erreur réseau → snackbar erreur, saving = false
  it('erreur réseau → snackbar erreur affiché, bouton réactivé', fakeAsync(() => {
    workspaceService.createWorkspace.and.returnValue(throwError(() => ({ status: 500 })));

    component.form.setValue({ name: 'Mon Cabinet' });
    component.submit();
    tick();

    expect(snackBar.open).toHaveBeenCalledWith(
      jasmine.stringContaining('Erreur'), jasmine.any(String), jasmine.any(Object)
    );
    expect(component.saving).toBeFalse();
  }));
});
