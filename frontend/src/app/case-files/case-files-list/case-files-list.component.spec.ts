import { TestBed, ComponentFixture } from '@angular/core/testing';
import { CaseFilesListComponent } from './case-files-list.component';
import { CaseFileService } from '../../core/services/case-file.service';
import { WorkspaceService } from '../../core/services/workspace.service';
import { TourService } from '../../core/services/tour.service';
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

const makeCaseFile = (overrides: Partial<CaseFile> = {}): CaseFile => ({
  id: 'cf1', title: 'Dossier Alpha', legalDomain: 'DROIT_DU_TRAVAIL', description: null,
  status: 'OPEN', createdAt: '2026-03-17T10:00:00Z', lastDocumentDeletedAt: null,
  riskLevel: null, riskScore: null, ...overrides
});

const mockPage: Page<CaseFile> = {
  content: [
    makeCaseFile({ id: 'cf1', title: 'Dossier Alpha', status: 'OPEN' }),
    makeCaseFile({ id: 'cf2', title: 'Cabinet Beta', legalDomain: 'DROIT_IMMIGRATION', status: 'CLOSED', createdAt: '2026-03-18T10:00:00Z' })
  ],
  totalElements: 2, totalPages: 1, size: 20, number: 0
};

const emptyPage: Page<CaseFile> = { content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 };

describe('CaseFilesListComponent', () => {
  let fixture: ComponentFixture<CaseFilesListComponent>;
  let component: CaseFilesListComponent;
  let caseFileServiceSpy: jasmine.SpyObj<CaseFileService>;
  let workspaceServiceSpy: jasmine.SpyObj<WorkspaceService>;
  let tourServiceSpy: jasmine.SpyObj<TourService>;
  let dialogSpy: jasmine.SpyObj<MatDialog>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    caseFileServiceSpy = jasmine.createSpyObj('CaseFileService', ['list']);
    workspaceServiceSpy = jasmine.createSpyObj('WorkspaceService', ['getCurrentWorkspace'], {
      workspaceSwitched$: new Subject<void>().asObservable()
    });
    tourServiceSpy = jasmine.createSpyObj('TourService', ['start', 'shouldShow']);
    dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);
    snackBarSpy = jasmine.createSpyObj('MatSnackBar', ['open']);

    caseFileServiceSpy.list.and.returnValue(of(emptyPage));
    workspaceServiceSpy.getCurrentWorkspace.and.returnValue(of(mockWorkspace));

    await TestBed.configureTestingModule({
      imports: [CaseFilesListComponent],
      providers: [
        { provide: CaseFileService, useValue: caseFileServiceSpy },
        { provide: WorkspaceService, useValue: workspaceServiceSpy },
        { provide: TourService, useValue: tourServiceSpy },
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
    expect(component.dataSource.length).toBe(2);
    expect(component.totalElements).toBe(2);
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

  it('ngOnInit → tourService.start() appelé avec le workspaceId', () => {
    expect(tourServiceSpy.start).toHaveBeenCalledWith('ws-1');
  });

  // C-01 — contrat data-tour-target : le bouton "Nouveau dossier" a l'attribut requis
  it('C-01: bouton "Nouveau dossier" a data-tour-target="new-dossier-btn"', () => {
    const el: HTMLElement = fixture.nativeElement;
    const btn = el.querySelector('[data-tour-target="new-dossier-btn"]');
    expect(btn).withContext('data-tour-target="new-dossier-btn" manquant dans le template').not.toBeNull();
  });

  // T-01 : searchTerm vide → filteredDataSource = dataSource complet
  it('T-01: searchTerm vide → filteredDataSource retourne tous les dossiers', () => {
    caseFileServiceSpy.list.and.returnValue(of(mockPage));
    component.loadCaseFiles();
    component.searchTerm = '';
    expect(component.filteredDataSource.length).toBe(2);
  });

  // T-02 : saisie partielle → filtre appliqué sur le nom
  it('T-02: saisie "alpha" → seul "Dossier Alpha" retourné', () => {
    caseFileServiceSpy.list.and.returnValue(of(mockPage));
    component.loadCaseFiles();
    component.onSearch('alpha');
    expect(component.filteredDataSource.length).toBe(1);
    expect(component.filteredDataSource[0].title).toBe('Dossier Alpha');
  });

  // T-03 : filtre insensible à la casse
  it('T-03: filtre insensible à la casse — "ALPHA" correspond à "Dossier Alpha"', () => {
    caseFileServiceSpy.list.and.returnValue(of(mockPage));
    component.loadCaseFiles();
    component.onSearch('ALPHA');
    expect(component.filteredDataSource.length).toBe(1);
    expect(component.filteredDataSource[0].id).toBe('cf1');
  });

  // T-04 : effacement du champ → liste complète restaurée
  it('T-04: effacement du champ → filteredDataSource restauré complet', () => {
    caseFileServiceSpy.list.and.returnValue(of(mockPage));
    component.loadCaseFiles();
    component.onSearch('alpha');
    expect(component.filteredDataSource.length).toBe(1);
    component.onSearch('');
    expect(component.filteredDataSource.length).toBe(2);
  });

  // T-05 : aucune correspondance → filteredDataSource vide
  it('T-05: aucune correspondance → filteredDataSource vide', () => {
    caseFileServiceSpy.list.and.returnValue(of(mockPage));
    component.loadCaseFiles();
    component.onSearch('zzz_inexistant');
    expect(component.filteredDataSource.length).toBe(0);
  });

  // T-06 : tri ASC par titre → ordre alphabétique
  it('T-06: tri ASC par titre → premier élément "Cabinet Beta"', () => {
    caseFileServiceSpy.list.and.returnValue(of(mockPage));
    component.loadCaseFiles();
    component.onSort({ active: 'title', direction: 'asc' });
    expect(component.filteredDataSource[0].title).toBe('Cabinet Beta');
    expect(component.filteredDataSource[1].title).toBe('Dossier Alpha');
  });

  // T-07 : tri DESC par titre → ordre inversé
  it('T-07: tri DESC par titre → premier élément "Dossier Alpha"', () => {
    caseFileServiceSpy.list.and.returnValue(of(mockPage));
    component.loadCaseFiles();
    component.onSort({ active: 'title', direction: 'desc' });
    expect(component.filteredDataSource[0].title).toBe('Dossier Alpha');
  });

  // T-08 : tri par date ASC → ordre chronologique
  it('T-08: tri ASC par date → cf1 avant cf2', () => {
    caseFileServiceSpy.list.and.returnValue(of(mockPage));
    component.loadCaseFiles();
    component.onSort({ active: 'createdAt', direction: 'asc' });
    expect(component.filteredDataSource[0].id).toBe('cf1');
    expect(component.filteredDataSource[1].id).toBe('cf2');
  });

  // T-09 : tri + filtre combinés → résultat cohérent
  it('T-09: tri + filtre combinés → seul résultat filtré, trié', () => {
    caseFileServiceSpy.list.and.returnValue(of(mockPage));
    component.loadCaseFiles();
    component.onSearch('alpha');
    component.onSort({ active: 'title', direction: 'asc' });
    expect(component.filteredDataSource.length).toBe(1);
    expect(component.filteredDataSource[0].title).toBe('Dossier Alpha');
  });

  // F-01 : filtre statut OPEN → seul cf1 retourné
  it('F-01: filterStatus OPEN → seul dossier OPEN retourné', () => {
    caseFileServiceSpy.list.and.returnValue(of(mockPage));
    component.loadCaseFiles();
    component.onFilterStatus('OPEN');
    expect(component.filteredDataSource.length).toBe(1);
    expect(component.filteredDataSource[0].id).toBe('cf1');
  });

  // F-02 : filtre statut CLOSED → seul cf2 retourné
  it('F-02: filterStatus CLOSED → seul dossier CLOSED retourné', () => {
    caseFileServiceSpy.list.and.returnValue(of(mockPage));
    component.loadCaseFiles();
    component.onFilterStatus('CLOSED');
    expect(component.filteredDataSource.length).toBe(1);
    expect(component.filteredDataSource[0].id).toBe('cf2');
  });

  // F-03 : filtre domaine DROIT_IMMIGRATION → seul cf2 retourné
  it('F-03: filterDomain DROIT_IMMIGRATION → seul dossier correspondant retourné', () => {
    caseFileServiceSpy.list.and.returnValue(of(mockPage));
    component.loadCaseFiles();
    component.onFilterDomain('DROIT_IMMIGRATION');
    expect(component.filteredDataSource.length).toBe(1);
    expect(component.filteredDataSource[0].id).toBe('cf2');
  });

  // F-04 : reset filtre statut → liste complète restaurée
  it('F-04: reset filterStatus → filteredDataSource complet', () => {
    caseFileServiceSpy.list.and.returnValue(of(mockPage));
    component.loadCaseFiles();
    component.onFilterStatus('OPEN');
    expect(component.filteredDataSource.length).toBe(1);
    component.onFilterStatus('');
    expect(component.filteredDataSource.length).toBe(2);
  });

  // F-05 : filtre statut + recherche combinés
  it('F-05: filterStatus + searchTerm combinés → résultat cumulé', () => {
    caseFileServiceSpy.list.and.returnValue(of(mockPage));
    component.loadCaseFiles();
    component.onFilterStatus('OPEN');
    component.onSearch('alpha');
    expect(component.filteredDataSource.length).toBe(1);
    expect(component.filteredDataSource[0].id).toBe('cf1');
  });

  // R-01 : badge de risque présent si riskLevel non null
  it('R-01: riskLevel non null → badge .risk-badge présent dans le DOM', () => {
    const page: Page<CaseFile> = {
      content: [makeCaseFile({ riskLevel: 'MOYEN', riskScore: 65 })],
      totalElements: 1, totalPages: 1, size: 20, number: 0
    };
    caseFileServiceSpy.list.and.returnValue(of(page));
    component.loadCaseFiles();
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.risk-badge');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('Moyen');
    expect(badge.textContent).toContain('65');
  });

  // R-02 : badge absent si riskLevel null
  it('R-02: riskLevel null → pas de badge .risk-badge', () => {
    const page: Page<CaseFile> = {
      content: [makeCaseFile({ riskLevel: null, riskScore: null })],
      totalElements: 1, totalPages: 1, size: 20, number: 0
    };
    caseFileServiceSpy.list.and.returnValue(of(page));
    component.loadCaseFiles();
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.risk-badge');
    expect(badge).toBeNull();
  });
});
