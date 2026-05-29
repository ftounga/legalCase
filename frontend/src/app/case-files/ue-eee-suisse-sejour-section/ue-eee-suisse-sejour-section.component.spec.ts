import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { UeEeeSuisseSejourSectionComponent } from './ue-eee-suisse-sejour-section.component';
import { UeEeeSuisseSejourResponse } from '../../core/models/ue-eee-suisse-sejour.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('UeEeeSuisseSejourSectionComponent', () => {
  let component: UeEeeSuisseSejourSectionComponent;
  let fixture: ComponentFixture<UeEeeSuisseSejourSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/ue-eee-suisse-sejour-analysis';

  function frResponse(overrides: Partial<UeEeeSuisseSejourResponse> = {}): UeEeeSuisseSejourResponse {
    return {
      caseFileId: 'case-1',
      nationalite: 'Italienne',
      estCitoyenUE: true,
      membreFamilleNonUE: false,
      dureeSejourMois: 72,
      activiteProfessionnelle: 'SALARIE',
      country: 'FRANCE',
      droitSejourAutomatique3Mois: true,
      droitSejourPlus5Ans: true,
      titreObtenu: 'ATTESTATION_ENREGISTREMENT',
      conditionsRespectees: ['Séjour régulier de plus de 5 ans', 'Activité salariée'],
      situationMembreNonUE: null,
      baseJuridique: 'Directive 2004/38/CE — art. L. 233-1 et s. du CESEDA',
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function fillValidForm(): void {
    component.nationalite.set('Italienne');
    component.estCitoyenUE.set(true);
    component.membreFamilleNonUE.set(false);
    component.dureeSejourMois.set(72);
    component.activiteProfessionnelle.set('SALARIE');
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [UeEeeSuisseSejourSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UeEeeSuisseSejourSectionComponent);
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
    expect(UeEeeSuisseSejourSectionComponent.TOOL_LABEL).toContain('UE/EEE/SUISSE');
    expect(UeEeeSuisseSejourSectionComponent.TOOL_ICON).toBe('public');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(UeEeeSuisseSejourSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 3 when nationalite + citoyenUE + duree present (FRANCE)', () => {
    expect(UeEeeSuisseSejourSectionComponent.getPrefillCount({
      aiData: { nationalite: 'Italienne', nationaliteUe: true, aesDureePresenceMois: 72 },
      workspaceCountry: 'FRANCE',
    })).toBe(3);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(UeEeeSuisseSejourSectionComponent.getPrefillCount({
      aiData: { nationalite: 'Italienne', nationaliteUe: true, aesDureePresenceMois: 72 },
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
    httpMock.expectOne(BASE_URL).flush(frResponse({ droitSejourPlus5Ans: false, dureeSejourMois: 12, membreFamilleNonUE: true }));
    expect(component.result()!.droitSejourPlus5Ans).toBe(false);
    expect(component.showForm()).toBe(false);
    expect(component.nationalite()).toBe('Italienne');
    expect(component.dureeSejourMois()).toBe(12);
    expect(component.membreFamilleNonUE()).toBe(true);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid requires non-empty nationalite + non-negative duree', () => {
    component.nationalite.set(null);
    component.dureeSejourMois.set(72);
    expect(component.formValid()).toBe(false);
    component.nationalite.set('   ');
    expect(component.formValid()).toBe(false);
    component.nationalite.set('Italienne');
    component.dureeSejourMois.set(null);
    expect(component.formValid()).toBe(false);
    component.dureeSejourMois.set(-1);
    expect(component.formValid()).toBe(false);
    component.dureeSejourMois.set(0);
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST nominal -> result + snack + body shape', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      nationalite: 'Italienne',
      estCitoyenUE: true,
      membreFamilleNonUE: false,
      dureeSejourMois: 72,
      activiteProfessionnelle: 'SALARIE',
    });
    req.flush(frResponse());
    expect(component.result()!.droitSejourPlus5Ans).toBe(true);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() does nothing when form invalid', () => {
    component.ngOnInit();
    flush404();
    component.nationalite.set(null);
    component.analyze();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('analyze() backend 400 -> snack-error', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
  });

  // --- result rendering ---

  it('droitSejourPlus5Ans=true -> success chip + success badge rendered', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ droitSejourPlus5Ans: true }));
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.acc-chip--success');
    expect(chip).not.toBeNull();
    expect(chip.textContent).toContain('permanent acquis');
    const badge = fixture.nativeElement.querySelector('[data-testid="badge-plus-5-ans"]');
    expect(badge.className).toContain('acc-badge--success');
    expect(badge.textContent).toContain('Oui');
  });

  it('droitSejourPlus5Ans=false -> info chip, badge not success', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ droitSejourPlus5Ans: false }));
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.acc-chip--info');
    expect(chip).not.toBeNull();
    const badge = fixture.nativeElement.querySelector('[data-testid="badge-plus-5-ans"]');
    expect(badge.className).not.toContain('acc-badge--success');
    expect(badge.textContent).toContain('Non');
  });

  it('renders titre chip and conditions list', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse());
    fixture.detectChanges();
    const titre = fixture.nativeElement.querySelector('[data-testid="titre-obtenu"]');
    expect(titre.textContent).toContain("Attestation d'enregistrement");
    const conds = fixture.nativeElement.querySelectorAll('[data-testid="conditions-block"] li');
    expect(conds.length).toBe(2);
  });

  it('renders situationMembreNonUE box when present', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({
      membreFamilleNonUE: true,
      titreObtenu: 'CARTE_SEJOUR_MEMBRE_FAMILLE',
      situationMembreNonUE: 'Le conjoint non-UE bénéficie d\'une carte de séjour « membre de famille ».',
    }));
    fixture.detectChanges();
    const box = fixture.nativeElement.querySelector('[data-testid="membre-non-ue-block"]');
    expect(box).not.toBeNull();
    expect(box.textContent).toContain('membre de famille');
  });

  it('does NOT render situationMembreNonUE box when null', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ situationMembreNonUE: null }));
    fixture.detectChanges();
    const box = fixture.nativeElement.querySelector('[data-testid="membre-non-ue-block"]');
    expect(box).toBeNull();
  });

  // --- prefill / labels ---

  it('aiData with nationalite + nationaliteUe + duree -> pre-fills all + provenance IA', () => {
    component.aiData = {
      nationalite: 'Espagnole',
      nationaliteUe: true,
      aesDureePresenceMois: 80,
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.nationalite()).toBe('Espagnole');
    expect(component.estCitoyenUE()).toBe(true);
    expect(component.dureeSejourMois()).toBe(80);
    expect(component.provenanceNationalite()).toBe('IA');
    expect(component.provenanceCitoyenUE()).toBe('IA');
    expect(component.provenanceDuree()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (backend wins)', () => {
    component.aiData = { nationalite: 'XX', nationaliteUe: false, aesDureePresenceMois: 99 } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ nationalite: 'Italienne', dureeSejourMois: 72 }));
    expect(component.nationalite()).toBe('Italienne');
    expect(component.dureeSejourMois()).toBe(72);
    expect(component.provenanceNationalite()).toBeNull();
  });

  it('onNationaliteChange clears provenance', () => {
    component.aiData = { nationalite: 'Espagnole' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceNationalite()).toBe('IA');
    component.onNationaliteChange('Portugaise');
    expect(component.provenanceNationalite()).toBeNull();
  });

  it('titreLabel / activiteLabel cover values', () => {
    expect(component.titreLabel('ATTESTATION_ENREGISTREMENT')).toContain('Attestation');
    expect(component.titreLabel('CARTE_SEJOUR_MEMBRE_FAMILLE')).toContain('membre de famille');
    expect(component.activiteLabel('SALARIE')).toBe('Salarié');
    expect(component.activiteLabel('ETUDIANT')).toBe('Étudiant');
    expect(component.activiteLabel('SANS_ACTIVITE_RESSOURCES_SUFFISANTES')).toContain('ressources');
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
    expect(component.nationalite()).toBeNull();
    component.aiData = { nationalite: 'Belge', nationaliteUe: true, aesDureePresenceMois: 24 } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.nationalite()).toBe('Belge');
    expect(component.estCitoyenUE()).toBe(true);
    expect(component.dureeSejourMois()).toBe(24);
    expect(component.provenanceNationalite()).toBe('IA');
  });

  it('BELGIQUE workspace shows info banner instead of form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.acc-banner--info');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('français uniquement');
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
