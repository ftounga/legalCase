import { TestBed, ComponentFixture } from '@angular/core/testing';
import { CaseFilesListComponent } from './case-files-list.component';
import { CaseFileService } from '../../core/services/case-file.service';
import { WorkspaceService } from '../../core/services/workspace.service';
import { OnboardingWizardService } from '../../core/services/onboarding-wizard.service';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of, throwError, Subject } from 'rxjs';
import { Page } from '../../core/models/page.model';
import { CaseFile } from '../../core/models/case-file.model';
import { Workspace } from '../../core/models/workspace.model';

const mockWorkspace: Workspace = { id: 'ws-1', name: 'Test', slug: 'test', planCode: 'SOLO', status: 'ACTIVE' };

const mockPage: Page<CaseFile> = {
  content: [{ id: 'cf1', title: 'Dossier A', legalDomain: 'DROIT_DU_TRAVAIL', description: null, status: 'OPEN', createdAt: '2026-03-17T10:00:00Z', lastDocumentDeletedAt: null }],
  totalElements: 1, totalPages: 1, size: 20, number: 0
};

const emptyPage: Page<CaseFile> = { content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 };

describe('CaseFilesListComponent', () => {
  let fixture: ComponentFixture<CaseFilesListComponent>;
  let component: CaseFilesListComponent;
  let caseFileServiceSpy: jasmine.SpyObj<CaseFileService>;
  let workspaceServiceSpy: jasmine.SpyObj<WorkspaceService>;
  let wizardServiceSpy: jasmine.SpyObj<OnboardingWizardService>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    caseFileServiceSpy = jasmine.createSpyObj('CaseFileService', ['list']);
    workspaceServiceSpy = jasmine.createSpyObj('WorkspaceService', ['getCurrentWorkspace'], {
      workspaceSwitched$: new Subject<void>().asObservable()
    });
    wizardServiceSpy = jasmine.createSpyObj('OnboardingWizardService', ['shouldShow', 'markDone']);
    dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    caseFileServiceSpy.list.and.returnValue(of(emptyPage));
    workspaceServiceSpy.getCurrentWorkspace.and.returnValue(of(mockWorkspace));
    wizardServiceSpy.shouldShow.and.returnValue(false);

    await TestBed.configureTestingModule({
      imports: [CaseFilesListComponent],
      providers: [
        { provide: CaseFileService, useValue: caseFileServiceSpy },
        { provide: WorkspaceService, useValue: workspaceServiceSpy },
        { provide: OnboardingWizardService, useValue: wizardServiceSpy },
        { provide: MatDialog, useValue: dialogSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        provideRouter([]),
        provideAnimationsAsync(),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(CaseFilesListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('ngOnInit → charge la liste des dossiers', () => {
    expect(caseFileServiceSpy.list).toHaveBeenCalledWith(0, 20);
  });

  it('liste vide → dataSource vide', () => {
    expect(component.dataSource.length).toBe(0);
    expect(component.totalElements).toBe(0);
  });

  it('liste avec items → dataSource peuplé', () => {
    caseFileServiceSpy.list.and.returnValue(of(mockPage));
    component.loadCaseFiles();
    expect(component.dataSource.length).toBe(1);
    expect(component.totalElements).toBe(1);
  });

  it('erreur API → affiche snackbar', () => {
    caseFileServiceSpy.list.and.returnValue(throwError(() => new Error('500')));
    component.loadCaseFiles();
    expect(snackBarSpy.open).toHaveBeenCalled();
  });

  it('statusLabel OPEN → "Ouvert"', () => {
    expect(component.statusLabel('OPEN')).toBe('Ouvert');
  });

  it('statusClass OPEN → badge--success', () => {
    expect(component.statusClass('OPEN')).toBe('badge--success');
  });

  it('wizard — shouldShow false → MatDialog.open non appelé', () => {
    wizardServiceSpy.shouldShow.and.returnValue(false);
    expect(dialogSpy.open).not.toHaveBeenCalled();
  });

});

describe('CaseFilesListComponent — wizard shouldShow true', () => {
  it('shouldShow true → MatDialog.open appelé', async () => {
    const caseFileServiceSpy = jasmine.createSpyObj('CaseFileService', ['list']);
    const workspaceServiceSpy = jasmine.createSpyObj('WorkspaceService', ['getCurrentWorkspace'], {
      workspaceSwitched$: new Subject<void>().asObservable()
    });
    const wizardServiceSpy = jasmine.createSpyObj('OnboardingWizardService', ['shouldShow', 'markDone']);
    const dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
    const snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    caseFileServiceSpy.list.and.returnValue(of(emptyPage));
    workspaceServiceSpy.getCurrentWorkspace.and.returnValue(of(mockWorkspace));
    wizardServiceSpy.shouldShow.and.returnValue(true);
    const dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['afterClosed']);
    dialogRefSpy.afterClosed.and.returnValue(of(undefined));
    dialogSpy.open.and.returnValue(dialogRefSpy);

    await TestBed.configureTestingModule({
      imports: [CaseFilesListComponent],
      providers: [
        { provide: CaseFileService, useValue: caseFileServiceSpy },
        { provide: WorkspaceService, useValue: workspaceServiceSpy },
        { provide: OnboardingWizardService, useValue: wizardServiceSpy },
        { provide: MatDialog, useValue: dialogSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        provideRouter([]),
        provideAnimationsAsync(),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    const f = TestBed.createComponent(CaseFilesListComponent);
    f.detectChanges();

    expect(dialogSpy.open).toHaveBeenCalled();
  });
});
