import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { ProtectionTemporaireUkraineBeSectionComponent } from './protection-temporaire-ukraine-be-section.component';
import { ProtectionTemporaireUkraineBeResponse } from '../../core/models/protection-temporaire-ukraine-be.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('ProtectionTemporaireUkraineBeSectionComponent', () => {
  let component: ProtectionTemporaireUkraineBeSectionComponent;
  let fixture: ComponentFixture<ProtectionTemporaireUkraineBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/protection-temporaire-ukraine-be-analysis';

  function beResponse(
    overrides: Partial<ProtectionTemporaireUkraineBeResponse> = {},
  ): ProtectionTemporaireUkraineBeResponse {
    return {
      caseFileId: 'case-1',
      dateArrivee: '2022-03-10',
      nationaliteUkrainienne: true,
      residenceUkraineAvant24Fev2022: true,
      apatridesUkraine: false,
      membreFamilleProtege: false,
      titreSejourBE: 'ATTESTATION_IMMATRICULATION',
      eligible: true,
      dureeProtectionRestante: 200,
      droitsTravail: "Accès immédiat au marché du travail salarié et indépendant.",
      droitsAides: ['Aide sociale (CPAS)', 'Scolarisation des enfants', 'Soins médicaux'],
      prochainRenouvellement: 'Renouvellement attendu au prochain prolongement du régime.',
      cheminProcedure: [
        "Se présenter à l'administration communale",
        "Enregistrement auprès de l'Office des étrangers",
        "Délivrance de l'attestation d'immatriculation",
      ],
      baseJuridique: "Décision d'exécution (UE) 2022/382 ; directive 2001/55/CE",
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
      imports: [ProtectionTemporaireUkraineBeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(ProtectionTemporaireUkraineBeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match((r) => r.url.includes('/jurisprudence-citations')).forEach((r) => r.flush({ items: [] }));
    httpMock.verify();
  });

  it('exposes TOOL_LABEL and TOOL_ICON statics', () => {
    expect(ProtectionTemporaireUkraineBeSectionComponent.TOOL_LABEL).toContain('PROTECTION TEMPORAIRE UKRAINE');
    expect(ProtectionTemporaireUkraineBeSectionComponent.TOOL_ICON).toBe('shield');
  });

  // ---- getPrefillCount : 0 / partiel / nominal=2 ----
  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(ProtectionTemporaireUkraineBeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 1 on partial pré-fill (date only)', () => {
    expect(ProtectionTemporaireUkraineBeSectionComponent.getPrefillCount({
      aiData: { ptUkraineDateArrivee: '2022-03-10' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 2 (nominal) when both real signals present (BELGIQUE)', () => {
    expect(ProtectionTemporaireUkraineBeSectionComponent.getPrefillCount({
      aiData: { ptUkraineDateArrivee: '2022-03-10', ptUkraineNationalite: true },
      workspaceCountry: 'BELGIQUE',
    })).toBe(2);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=FRANCE', () => {
    expect(ProtectionTemporaireUkraineBeSectionComponent.getPrefillCount({
      aiData: { ptUkraineDateArrivee: '2022-03-10' },
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  // ---- HTTP lifecycle ----
  it('BELGIQUE -> GET called on ngOnInit', () => {
    expect(component.isBelgique()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'NF' }, { status: 404, statusText: 'NF' });
  });

  it('FRANCE -> no HTTP on ngOnInit', () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('loads existing analysis on GET 200', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse());
    expect(component.result()!.eligible).toBe(true);
    expect(component.showForm()).toBe(false);
    expect(component.dateArrivee()).toBe('2022-03-10');
    expect(component.titreSejourBE()).toBe('ATTESTATION_IMMATRICULATION');
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---- form validation ----
  it('formValid false until dateArrivee + titreSejourBE present', () => {
    expect(component.formValid()).toBe(false);
    component.dateArrivee.set('2022-03-10');
    expect(component.formValid()).toBe(false);
    component.titreSejourBE.set('AUCUN');
    expect(component.formValid()).toBe(true);
  });

  // ---- analyze POST ----
  it('analyze() POST nominal -> result + snack', () => {
    component.ngOnInit();
    flush404();
    component.dateArrivee.set('2022-03-10');
    component.onNationaliteChange(true);
    component.onResidenceChange(true);
    component.titreSejourBE.set('ATTESTATION_IMMATRICULATION');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateArrivee: '2022-03-10',
      nationaliteUkrainienne: true,
      residenceUkraineAvant24Fev2022: true,
      apatridesUkraine: false,
      membreFamilleProtege: false,
      titreSejourBE: 'ATTESTATION_IMMATRICULATION',
    });
    req.flush(beResponse());
    expect(component.result()!.eligible).toBe(true);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() backend 400 -> snack-error', () => {
    component.ngOnInit();
    flush404();
    component.dateArrivee.set('2022-03-10');
    component.titreSejourBE.set('AUCUN');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
  });

  // ---- pré-fill IA ----
  it('aiData with both real signals -> pre-fills + provenance IA', () => {
    component.aiData = {
      ptUkraineDateArrivee: '2022-03-10',
      ptUkraineNationalite: true,
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.dateArrivee()).toBe('2022-03-10');
    expect(component.provenanceDateArrivee()).toBe('IA');
    expect(component.nationaliteUkrainienne()).toBe(true);
    expect(component.provenanceNationalite()).toBe('IA');
  });

  it('onDateArriveeChange clears provenance', () => {
    component.aiData = { ptUkraineDateArrivee: '2022-03-10' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceDateArrivee()).toBe('IA');
    component.onDateArriveeChange('2022-04-01');
    expect(component.provenanceDateArrivee()).toBeNull();
  });

  it('ngOnChanges with new aiData in form mode -> re-prefill', () => {
    component.ngOnInit();
    flush404();
    expect(component.nationaliteUkrainienne()).toBe(false);
    component.aiData = {
      ptUkraineDateArrivee: '2022-05-01',
      ptUkraineNationalite: true,
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dateArrivee()).toBe('2022-05-01');
    expect(component.nationaliteUkrainienne()).toBe(true);
    expect(component.provenanceNationalite()).toBe('IA');
  });

  // ---- F-IA-03 VOIE (a) inline badge ----
  it('F-IA-03: inline alert when user date diverges from IA date', () => {
    component.aiData = { ptUkraineDateArrivee: '2022-03-10' } as ImmigrationExtractedData;
    component.dateArrivee.set('2022-04-01');
    component.provenanceDateArrivee.set(null);
    const alert = component.dateArriveeAlert();
    expect(alert).toBeTruthy();
    expect(alert!.label).toContain('10/03/2022');
  });

  it('F-IA-03: no inline alert when user date matches IA date', () => {
    component.aiData = { ptUkraineDateArrivee: '2022-03-10' } as ImmigrationExtractedData;
    component.dateArrivee.set('2022-03-10');
    expect(component.dateArriveeAlert()).toBeNull();
  });

  // ---- Badge éligibilité ----
  it('eligibiliteBadgeClass/Label/Icon ELIGIBLE -> vert', () => {
    expect(component.eligibiliteBadgeClass(true)).toContain('pt-elig-badge--ok');
    expect(component.eligibiliteLabel(true)).toBe('ELIGIBLE');
    expect(component.eligibiliteIcon(true)).toBe('verified_user');
  });

  it('eligibiliteBadgeClass/Label/Icon INELIGIBLE -> rouge', () => {
    expect(component.eligibiliteBadgeClass(false)).toContain('pt-elig-badge--ko');
    expect(component.eligibiliteLabel(false)).toBe('INELIGIBLE');
    expect(component.eligibiliteIcon(false)).toBe('gpp_bad');
  });

  // ---- Bandeau renouvellement imminent (< 90 j) ----
  it('renouvellementImminent true when eligible and dureeRestante <= 90', () => {
    expect(component.renouvellementImminent(beResponse({ eligible: true, dureeProtectionRestante: 45 }))).toBe(true);
    expect(component.renouvellementImminent(beResponse({ eligible: true, dureeProtectionRestante: 90 }))).toBe(true);
  });

  it('renouvellementImminent false when dureeRestante > 90 or non eligible', () => {
    expect(component.renouvellementImminent(beResponse({ eligible: true, dureeProtectionRestante: 200 }))).toBe(false);
    expect(component.renouvellementImminent(beResponse({ eligible: false, dureeProtectionRestante: 10 }))).toBe(false);
    expect(component.renouvellementImminent(null)).toBe(false);
  });

  it('renouvellementMessage handles boolean and string forms', () => {
    expect(component.renouvellementMessage(beResponse({ prochainRenouvellement: 'message custom' }))).toBe('message custom');
    expect(component.renouvellementMessage(beResponse({ prochainRenouvellement: true }))).toContain('requis');
    expect(component.renouvellementMessage(beResponse({ prochainRenouvellement: false }))).toContain('Aucun renouvellement');
  });

  // ---- dates + titre ----
  it('formatDateFr converts ISO to JJ/MM/YYYY', () => {
    expect(component.formatDateFr('2022-03-10')).toBe('10/03/2022');
    expect(component.formatDateFr(null)).toBe('—');
    expect(component.formatDateFr('not-a-date')).toBe('—');
  });

  it('titreLabel resolves codes and empty for null', () => {
    expect(component.titreLabel('ATTESTATION_IMMATRICULATION')).toContain('immatriculation');
    expect(component.titreLabel('AUCUN')).toContain('Aucun');
    expect(component.titreLabel(null)).toBe('');
  });

  // ---- rendering : anti-régression VOIE (a) — no binding errors ----
  it('renders form fields without binding errors (standaloneMode)', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    const dateInput = fixture.nativeElement.querySelector('[data-testid="date-arrivee-input"]');
    const natCheckbox = fixture.nativeElement.querySelector('[data-testid="nationalite-checkbox"]');
    const residence = fixture.nativeElement.querySelector('[data-testid="residence-checkbox"]');
    const titreSelect = fixture.nativeElement.querySelector('[data-testid="titre-sejour-select"]');
    expect(dateInput).not.toBeNull();
    expect(natCheckbox).not.toBeNull();
    expect(residence).not.toBeNull();
    expect(titreSelect).not.toBeNull();
  });

  it('FRANCE workspace shows BE-only info banner instead of form', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="fr-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('Belgique uniquement');
  });

  it('renders éligibilité badge + durée restante (JetBrains Mono) for ELIGIBLE', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({ eligible: true, dureeProtectionRestante: 200 }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('[data-testid="eligibilite-badge"]');
    const duree = fixture.nativeElement.querySelector('[data-testid="duree-restante"]');
    expect(badge.classList.contains('pt-elig-badge--ok')).toBe(true);
    expect(badge.textContent).toContain('ELIGIBLE');
    expect(duree).not.toBeNull();
    expect(duree.textContent.trim()).toBe('200 jours');
  });

  it('renders droits travail block with single permit mention', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({ eligible: true }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const travail = fixture.nativeElement.querySelector('[data-testid="droits-travail"]');
    expect(travail).not.toBeNull();
    expect(travail.textContent.toLowerCase()).toContain('single permit');
  });

  it('renders renouvellement banner only when imminent (< 90 j)', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    // > 90 j -> no banner
    component.result.set(beResponse({ eligible: true, dureeProtectionRestante: 200 }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="renouvellement-banner"]')).toBeNull();
    // <= 90 j -> banner
    component.result.set(beResponse({ eligible: true, dureeProtectionRestante: 30 }));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="renouvellement-banner"]')).not.toBeNull();
  });

  it('renders cheminProcedure numbered list', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({ eligible: true }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const proc = fixture.nativeElement.querySelector('[data-testid="chemin-procedure"]');
    expect(proc).not.toBeNull();
    expect(proc.querySelectorAll('li').length).toBe(3);
  });

  it('renders INELIGIBLE banner and hides droits when not eligible', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({ eligible: false, dureeProtectionRestante: 0 }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="ineligible-banner"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="droits-travail"]')).toBeNull();
  });

  it('standaloneMode -> no GET, form visible, simulator banner displayed', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('Mode simulateur');
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
});
