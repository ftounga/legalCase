import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { RegimeTunisienSectionComponent } from './regime-tunisien-section.component';
import { RegimeTunisienResponse } from '../../core/models/regime-tunisien.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('RegimeTunisienSectionComponent', () => {
  let component: RegimeTunisienSectionComponent;
  let fixture: ComponentFixture<RegimeTunisienSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/regime-tunisien-analysis';

  function frResponse(overrides: Partial<RegimeTunisienResponse> = {}): RegimeTunisienResponse {
    return {
      caseFileId: 'case-1',
      categorie: 'ETUDIANT',
      dureeSejourEnvisageeMois: 12,
      titreEnCours: false,
      dejaResident: false,
      country: 'FRANCE',
      regime: 'ACCORD_1988_DEROGATOIRE',
      particularitesApplicables: ['Carte étudiant accord 1988'],
      basesJuridiques: ['Accord franco-tunisien du 17/03/1988'],
      renvoiDroitCommun: false,
      messages: [],
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [RegimeTunisienSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(RegimeTunisienSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  it('exposes TOOL_LABEL and TOOL_ICON statics', () => {
    expect(RegimeTunisienSectionComponent.TOOL_LABEL).toContain('1988');
    expect(RegimeTunisienSectionComponent.TOOL_ICON).toBe('public');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(RegimeTunisienSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 3 when all 3 IA signals present', () => {
    expect(RegimeTunisienSectionComponent.getPrefillCount({
      aiData: {
        regimeTunisienCategorie: 'ETUDIANT',
        regimeTunisienDureeSejour: 12,
        regimeTunisienTitreEnCours: true,
      },
      workspaceCountry: 'FRANCE',
    })).toBe(3);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(RegimeTunisienSectionComponent.getPrefillCount({
      aiData: { regimeTunisienCategorie: 'ETUDIANT' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('FRANCE -> GET called on ngOnInit', () => {
    expect(component.isFrance()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'NF' }, { status: 404, statusText: 'NF' });
  });

  it('BELGIQUE -> no HTTP on ngOnInit', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('loads existing analysis on GET 200', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse());
    expect(component.result()!.regime).toBe('ACCORD_1988_DEROGATOIRE');
    expect(component.showForm()).toBe(false);
    expect(component.categorie()).toBe('ETUDIANT');
    expect(component.dureeSejourEnvisageeMois()).toBe(12);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid false until categorie set; duree negative invalid', () => {
    expect(component.formValid()).toBe(false);
    component.categorie.set('ETUDIANT');
    expect(component.formValid()).toBe(true);
    component.dureeSejourEnvisageeMois.set(-2);
    expect(component.formValid()).toBe(false);
    component.dureeSejourEnvisageeMois.set(12);
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST nominal -> result + snack', () => {
    component.ngOnInit();
    flush404();
    component.categorie.set('ETUDIANT');
    component.dureeSejourEnvisageeMois.set(12);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      categorie: 'ETUDIANT',
      dureeSejourEnvisageeMois: 12,
      titreEnCours: false,
      dejaResident: false,
    });
    req.flush(frResponse());
    expect(component.result()!.regime).toBe('ACCORD_1988_DEROGATOIRE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() does nothing when form invalid', () => {
    component.ngOnInit();
    flush404();
    component.analyze();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('analyze() backend 400 -> snack-error', () => {
    component.ngOnInit();
    flush404();
    component.categorie.set('ETUDIANT');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
  });

  it('aiData with all 3 IA signals -> pre-fills + provenance IA', () => {
    component.aiData = {
      regimeTunisienCategorie: 'SALARIE',
      regimeTunisienDureeSejour: 24,
      regimeTunisienTitreEnCours: true,
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.categorie()).toBe('SALARIE');
    expect(component.provenanceCategorie()).toBe('IA');
    expect(component.dureeSejourEnvisageeMois()).toBe(24);
    expect(component.provenanceDuree()).toBe('IA');
    expect(component.titreEnCours()).toBe(true);
    expect(component.provenanceTitre()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (backend wins)', () => {
    component.aiData = {
      regimeTunisienCategorie: 'AUTRE',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ categorie: 'ETUDIANT' }));
    expect(component.categorie()).toBe('ETUDIANT');
    expect(component.provenanceCategorie()).toBeNull();
  });

  it('onCategorieChange / onDureeChange / onTitreChange clear provenance', () => {
    component.aiData = {
      regimeTunisienCategorie: 'ETUDIANT',
      regimeTunisienDureeSejour: 12,
      regimeTunisienTitreEnCours: true,
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceCategorie()).toBe('IA');
    component.onCategorieChange('SALARIE');
    expect(component.provenanceCategorie()).toBeNull();
    component.onDureeChange(36);
    expect(component.provenanceDuree()).toBeNull();
    component.onTitreChange(false);
    expect(component.provenanceTitre()).toBeNull();
  });

  it('bannerClass / bannerIcon / regimeLabel cover all regimes', () => {
    expect(component.bannerClass('ACCORD_1988_DEROGATOIRE')).toContain('rt-banner--info');
    expect(component.bannerClass('MIXTE')).toContain('rt-banner--warning');
    expect(component.bannerClass('DROIT_COMMUN_CESEDA')).toContain('rt-banner--neutral');
    expect(component.bannerIcon('ACCORD_1988_DEROGATOIRE')).toBe('gavel');
    expect(component.bannerIcon('MIXTE')).toBe('call_split');
    expect(component.bannerIcon('DROIT_COMMUN_CESEDA')).toBe('menu_book');
    expect(component.regimeLabel('DROIT_COMMUN_CESEDA')).toContain('CESEDA');
    expect(component.regimeLabel('MIXTE')).toContain('mixte');
  });

  it('categorieLabel maps each code to FR label', () => {
    expect(component.categorieLabel('ETUDIANT')).toBe('Étudiant');
    expect(component.categorieLabel('COMMERCANT')).toBe('Commerçant');
    expect(component.categorieLabel('SALARIE')).toBe('Salarié');
    expect(component.categorieLabel('FAMILIAL')).toBe('Familial');
    expect(component.categorieLabel('AUTRE')).toBe('Autre');
    expect(component.categorieLabel(null)).toBe('');
  });

  it('toggleCollapse inverts collapsed state', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
  });

  it('editMode resets showForm to true', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  it('ngOnChanges with new aiData in form mode -> re-prefill', () => {
    component.ngOnInit();
    flush404();
    expect(component.categorie()).toBeNull();
    component.aiData = {
      regimeTunisienCategorie: 'COMMERCANT',
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.categorie()).toBe('COMMERCANT');
    expect(component.provenanceCategorie()).toBe('IA');
  });

  it('ngOnChanges does NOT re-prefill when result already loaded', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ categorie: 'ETUDIANT' }));
    component.aiData = {
      regimeTunisienCategorie: 'AUTRE',
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.categorie()).toBe('ETUDIANT');
    expect(component.provenanceCategorie()).toBeNull();
  });

  it('BELGIQUE workspace shows info banner instead of form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.rt-banner--info');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('française uniquement');
  });

  it('standaloneMode -> no GET, form visible, banner displayed', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('Mode simulateur');
  });
});
