import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { OfpraIntroductionSectionComponent } from './ofpra-introduction-section.component';
import { OfpraIntroductionResponse } from '../../core/models/ofpra-introduction.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('OfpraIntroductionSectionComponent', () => {
  let component: OfpraIntroductionSectionComponent;
  let fixture: ComponentFixture<OfpraIntroductionSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/ofpra-introduction-analysis';

  function frResponse(overrides: Partial<OfpraIntroductionResponse> = {}): OfpraIntroductionResponse {
    return {
      caseFileId: 'case-1',
      country: 'FRANCE',
      dateArriveeEnFrance: '2026-01-10',
      passageGudaEffectue: true,
      datePassageGuda: '2026-01-20',
      adaRequise: true,
      paysOrigine: null,
      dateEcheanceIntroduction: '2026-02-10',
      statutDelai: 'A_DEPOSER',
      etapesAPrendre: [
        'Enregistrement en préfecture (GUDA)',
        'Remise de l\'attestation de demande d\'asile',
        'Retrait du formulaire OFPRA',
        'Introduction de la demande auprès de l\'OFPRA',
        'Convocation à l\'entretien OFPRA',
      ],
      piecesRequises: ['Attestation de demande d\'asile', 'Formulaire OFPRA complété', 'Récit de vie'],
      procedureAccelereeRisque: false,
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function fillValidForm(): void {
    component.dateArriveeEnFrance.set('2026-01-10');
    component.passageGudaEffectue.set(true);
    component.datePassageGuda.set('2026-01-20');
    component.adaRequise.set(true);
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [
        OfpraIntroductionSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
        RouterTestingModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(OfpraIntroductionSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match((r) => r.url.includes('/jurisprudence-citations')).forEach((r) => r.flush({ items: [] }));
    httpMock.verify();
  });

  it('creates', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('FRANCE : charge l\'analyse existante au ngOnInit', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush(frResponse());
    expect(component.result()?.statutDelai).toBe('A_DEPOSER');
    expect(component.showForm()).toBe(false);
  });

  it('404 : bascule sur le formulaire', () => {
    fixture.detectChanges();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('gate FRANCE : aucun appel HTTP en Belgique + bannière BE', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    expect(component.isFrance()).toBe(false);
    const banner = fixture.nativeElement.querySelector('[data-testid="be-banner"]');
    expect(banner).toBeTruthy();
  });

  it('formValid : exige une date d\'arrivée ISO + GUDA cohérent', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    expect(component.formValid()).toBe(false);
    component.dateArriveeEnFrance.set('2026-01-10');
    expect(component.formValid()).toBe(true);
    component.datePassageGuda.set('2026-01-05'); // antérieure à l'arrivée → invalide
    expect(component.formValid()).toBe(false);
    component.datePassageGuda.set('2026-01-20');
    expect(component.formValid()).toBe(true);
  });

  it('analyze : POST + bascule sur le résultat + snackbar', () => {
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.dateArriveeEnFrance).toBe('2026-01-10');
    expect(req.request.body.passageGudaEffectue).toBe(true);
    expect(req.request.body.datePassageGuda).toBe('2026-01-20');
    expect(req.request.body.adaRequise).toBe(true);
    req.flush(frResponse());
    expect(component.showForm()).toBe(false);
    expect(component.result()?.statutDelai).toBe('A_DEPOSER');
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze : GUDA non effectué → datePassageGuda null dans le payload', () => {
    fixture.detectChanges();
    flush404();
    component.dateArriveeEnFrance.set('2026-01-10');
    component.passageGudaEffectue.set(false);
    component.datePassageGuda.set('2026-01-20'); // ignorée car passage non effectué
    component.adaRequise.set(false);
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.body.datePassageGuda).toBeNull();
    req.flush(frResponse());
    expect(component.result()).toBeTruthy();
  });

  it('analyze : erreur HTTP → snackbar erreur, reste sur le formulaire', () => {
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Boom' }, { status: 500, statusText: 'Server Error' });
    expect(component.analyzing()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('résultat : stepper rend les 5 étapes ordonnées', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(frResponse());
    component.collapsed.set(false);
    fixture.detectChanges();
    const stepper = fixture.nativeElement.querySelector('[data-testid="stepper"]');
    expect(stepper).toBeTruthy();
    const steps = fixture.nativeElement.querySelectorAll('[data-testid="stepper-step"]');
    expect(steps.length).toBe(5);
    expect(steps[0].textContent).toContain('Enregistrement en préfecture');
  });

  it('procédure accélérée : bannière orange rendue si risque', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(frResponse({ procedureAccelereeRisque: true }));
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="procedure-acceleree"]');
    expect(banner).toBeTruthy();
    expect(banner.textContent).toContain('procédure accélérée');
  });

  it('sans risque : pas de bannière procédure accélérée', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(frResponse({ procedureAccelereeRisque: false }));
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="procedure-acceleree"]');
    expect(banner).toBeNull();
  });

  it('statut URGENT : chip rendu avec le bon libellé', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(frResponse({ statutDelai: 'URGENT' }));
    component.collapsed.set(false);
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('[data-testid="statut-chip"]');
    expect(chip.textContent).toContain('Urgent');
  });

  it('prefillFromAi : pré-remplit dateArriveeEnFrance depuis aesDateEntreeFrance', () => {
    const aiData = { aesDateEntreeFrance: '2026-01-10' } as ImmigrationExtractedData;
    fixture.detectChanges();
    flush404();
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, false) });
    expect(component.dateArriveeEnFrance()).toBe('2026-01-10');
    expect(component.provenanceArrivee()).toBe('IA');
  });

  it('getPrefillCount static reflète le runtime (1 champ)', () => {
    const count = OfpraIntroductionSectionComponent.getPrefillCount({
      aiData: { aesDateEntreeFrance: '2026-01-10' },
      workspaceCountry: 'FRANCE',
    });
    expect(count).toBe(1);
  });

  it('toggleCollapse inverse l\'état replié', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    const before = component.collapsed();
    component.toggleCollapse();
    expect(component.collapsed()).toBe(!before);
  });

  it('standaloneMode : pas d\'appel HTTP, formulaire visible', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    expect(component.showForm()).toBe(true);
    expect(component.collapsed()).toBe(false);
  });
});
