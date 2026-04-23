import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { WorkspaceCreateDialogComponent } from './workspace-create-dialog.component';
import { WorkspaceService } from '../../core/services/workspace.service';
import { Workspace } from '../../core/models/workspace.model';

describe('WorkspaceCreateDialogComponent', () => {
  let fixture: ComponentFixture<WorkspaceCreateDialogComponent>;
  let component: WorkspaceCreateDialogComponent;
  let workspaceServiceSpy: jest.Mocked<WorkspaceService>;
  let dialogRefSpy: jest.Mocked<MatDialogRef<WorkspaceCreateDialogComponent, Workspace | null>>;
  let snackBarSpy: jest.Mocked<MatSnackBar>;

  const createdWorkspace: Workspace = {
    id: 'ws-new',
    name: 'Cabinet Immigration FR',
    legalDomain: 'DROIT_IMMIGRATION',
    country: 'FRANCE',
    planCode: 'FREE',
    status: 'ACTIVE',
    primary: true,
  } as Workspace;

  async function setup() {
    workspaceServiceSpy = {
      createWorkspace: jest.fn(),
    } as any;
    dialogRefSpy = { close: jest.fn() } as any;
    snackBarSpy = { open: jest.fn() } as any;

    await TestBed.configureTestingModule({
      imports: [WorkspaceCreateDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: WorkspaceService, useValue: workspaceServiceSpy },
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WorkspaceCreateDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('U-01 — formulaire vide → bouton Créer désactivé', async () => {
    await setup();
    expect(component.canSubmit).toBe(false);
  });

  it('U-02 — formulaire complet → bouton Créer activé', async () => {
    await setup();
    component.name = 'Cabinet Test';
    component.selectedDomain = 'DROIT_IMMIGRATION';
    component.selectedCountry = 'FRANCE';
    expect(component.canSubmit).toBe(true);
  });

  it('U-02b — nom avec seulement des espaces → bouton désactivé', async () => {
    await setup();
    component.name = '   ';
    expect(component.canSubmit).toBe(false);
  });

  it('U-03 — submit succès → createWorkspace appelé + dialog fermé avec le workspace', async () => {
    await setup();
    workspaceServiceSpy.createWorkspace.mockReturnValue(of(createdWorkspace));
    component.name = '  Cabinet Test  ';
    component.selectedDomain = 'DROIT_IMMIGRATION';
    component.selectedCountry = 'FRANCE';

    component.submit();

    expect(workspaceServiceSpy.createWorkspace).toHaveBeenCalledWith(
      'Cabinet Test', 'DROIT_IMMIGRATION', 'FRANCE'
    );
    expect(dialogRefSpy.close).toHaveBeenCalledWith(createdWorkspace);
    expect(component.submitting()).toBe(false);
  });

  it('U-04 — submit erreur → snackbar affiché, dialog pas fermé, submitting revient à false', async () => {
    await setup();
    workspaceServiceSpy.createWorkspace.mockReturnValue(throwError(() => ({ status: 500 })));
    component.name = 'Cabinet Test';

    component.submit();

    expect(snackBarSpy.open).toHaveBeenCalled();
    expect(dialogRefSpy.close).not.toHaveBeenCalled();
    expect(component.submitting()).toBe(false);
  });

  it('U-05 — cancel → dialog fermé avec null', async () => {
    await setup();
    component.cancel();
    expect(dialogRefSpy.close).toHaveBeenCalledWith(null);
  });

  it('U-06 — selectDomain change selectedDomain', async () => {
    await setup();
    component.selectDomain('DROIT_FAMILLE');
    expect(component.selectedDomain).toBe('DROIT_FAMILLE');
  });

  it('U-07 — 3 tiles domaines rendues + 2 chips pays', async () => {
    await setup();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('.wcd-domain-tile').length).toBe(3);
    expect(el.querySelectorAll('.wcd-country-chip').length).toBe(2);
  });

  it('U-08 — submit bloque les clics pendant la requête', async () => {
    await setup();
    workspaceServiceSpy.createWorkspace.mockReturnValue(of(createdWorkspace));
    component.name = 'Cabinet Test';
    component.submitting.set(true);
    component.submit();
    expect(workspaceServiceSpy.createWorkspace).not.toHaveBeenCalled();
  });
});
