import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { RegimeMayotteSectionComponent } from './regime-mayotte-section.component';
import { RegimeMayotteResponse } from '../../core/models/regime-mayotte.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('RegimeMayotteSectionComponent', () => {
  let component: RegimeMayotteSectionComponent;
  let fixture: ComponentFixture<RegimeMayotteSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/regime-mayotte-analysis';

  function frResponse(overrides: Partial<RegimeMayotteResponse> = {}): RegimeMayotteResponse {
    return {
      caseFileId: 'case-1',
      titreDelivreAMayotte: true,
      typeTitre: 'SALARIE',
      projetDeplacementMetropole: true,
      dateDelivrance: '2024-01-15',
      country: 'FRANCE',
      porteeTerritoriale: 'MAYOTTE_UNIQUEMENT',
      sousStatutDeplacement: 'BLOCAGE_DEPLACEMENT',
      obligationsSpecifiques: ['Titre territorialisé Mayotte'],
      demarchesDeplacementMetropole: ['Solliciter un visa de circulation'],
      basesJuridiques: ['Ordonnance n° 2014-464 du 7 mai 2014'],
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
      imports: [RegimeMayotteSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(RegimeMayotteSectionComponent);
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
    expect(RegimeMayotteSectionComponent.TOOL_LABEL).toContain('MAYOTTE');
    expect(RegimeMayotteSectionComponent.TOOL_ICON).toBe('public');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(RegimeMayotteSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 3 when all 3 IA signals present', () => {
    expect(RegimeMayotteSectionComponent.getPrefillCount({
      aiData: {
        mayotteTitreDelivreAMayotte: true,
        mayotteTypeTitre: 'VPF',
        mayotteProjetDeplacementMetropole: false,
      },
      workspaceCountry: 'FRANCE',
    })).toBe(3);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(RegimeMayotteSectionComponent.getPrefillCount({
      aiData: { mayotteTypeTitre: 'VPF' },
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
    expect(component.result()!.porteeTerritoriale).toBe('MAYOTTE_UNIQUEMENT');
    expect(component.showForm()).toBe(false);
    expect(component.typeTitre()).toBe('SALARIE');
    expect(component.titreDelivreAMayotte()).toBe(true);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid false until typeTitre set', () => {
    expect(component.formValid()).toBe(false);
    component.typeTitre.set('VPF');
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST nominal -> result + snack', () => {
    component.ngOnInit();
    flush404();
    component.typeTitre.set('SALARIE');
    component.titreDelivreAMayotte.set(true);
    component.projetDeplacementMetropole.set(true);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      titreDelivreAMayotte: true,
      typeTitre: 'SALARIE',
      projetDeplacementMetropole: true,
      dateDelivrance: null,
    });
    req.flush(frResponse());
    expect(component.result()!.porteeTerritoriale).toBe('MAYOTTE_UNIQUEMENT');
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
    component.typeTitre.set('VPF');
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
      mayotteTitreDelivreAMayotte: true,
      mayotteTypeTitre: 'ETUDIANT',
      mayotteProjetDeplacementMetropole: true,
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.titreDelivreAMayotte()).toBe(true);
    expect(component.provenanceTitreMayotte()).toBe('IA');
    expect(component.typeTitre()).toBe('ETUDIANT');
    expect(component.provenanceTypeTitre()).toBe('IA');
    expect(component.projetDeplacementMetropole()).toBe(true);
    expect(component.provenanceProjet()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (backend wins)', () => {
    component.aiData = {
      mayotteTypeTitre: 'AUTRE',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ typeTitre: 'SALARIE' }));
    expect(component.typeTitre()).toBe('SALARIE');
    expect(component.provenanceTypeTitre()).toBeNull();
  });

  it('onTypeTitreChange / onTitreMayotteChange / onProjetChange clear provenance', () => {
    component.aiData = {
      mayotteTitreDelivreAMayotte: true,
      mayotteTypeTitre: 'VPF',
      mayotteProjetDeplacementMetropole: true,
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceTypeTitre()).toBe('IA');
    component.onTypeTitreChange('SALARIE');
    expect(component.provenanceTypeTitre()).toBeNull();
    component.onTitreMayotteChange(false);
    expect(component.provenanceTitreMayotte()).toBeNull();
    component.onProjetChange(false);
    expect(component.provenanceProjet()).toBeNull();
  });

  it('bannerClass / bannerIcon cover the three verdict states', () => {
    expect(component.bannerClass(frResponse({ porteeTerritoriale: 'MAYOTTE_UNIQUEMENT', sousStatutDeplacement: 'BLOCAGE_DEPLACEMENT' }))).toContain('rm-banner--warning');
    expect(component.bannerClass(frResponse({ porteeTerritoriale: 'MAYOTTE_UNIQUEMENT', sousStatutDeplacement: 'DEPLACEMENT_LIBRE' }))).toContain('rm-banner--info');
    expect(component.bannerClass(frResponse({ porteeTerritoriale: 'DROIT_COMMUN', sousStatutDeplacement: 'DEPLACEMENT_LIBRE' }))).toContain('rm-banner--neutral');
    expect(component.bannerIcon(frResponse({ porteeTerritoriale: 'MAYOTTE_UNIQUEMENT', sousStatutDeplacement: 'BLOCAGE_DEPLACEMENT' }))).toBe('block');
    expect(component.bannerIcon(frResponse({ porteeTerritoriale: 'DROIT_COMMUN', sousStatutDeplacement: 'DEPLACEMENT_LIBRE' }))).toBe('menu_book');
  });

  it('porteeLabel / typeTitreLabel map codes to FR labels', () => {
    expect(component.porteeLabel('MAYOTTE_UNIQUEMENT')).toContain('Mayotte');
    expect(component.porteeLabel('DROIT_COMMUN')).toContain('Droit commun');
    expect(component.typeTitreLabel('VPF')).toContain('Vie privée');
    expect(component.typeTitreLabel('SALARIE')).toBe('Salarié');
    expect(component.typeTitreLabel('RESIDENT')).toBe('Résident');
    expect(component.typeTitreLabel(null)).toBe('');
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
    expect(component.typeTitre()).toBeNull();
    component.aiData = {
      mayotteTypeTitre: 'RESIDENT',
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.typeTitre()).toBe('RESIDENT');
    expect(component.provenanceTypeTitre()).toBe('IA');
  });

  it('ngOnChanges does NOT re-prefill when result already loaded', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ typeTitre: 'SALARIE' }));
    component.aiData = {
      mayotteTypeTitre: 'AUTRE',
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.typeTitre()).toBe('SALARIE');
    expect(component.provenanceTypeTitre()).toBeNull();
  });

  it('BELGIQUE workspace shows info banner instead of form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.rm-banner--info');
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
