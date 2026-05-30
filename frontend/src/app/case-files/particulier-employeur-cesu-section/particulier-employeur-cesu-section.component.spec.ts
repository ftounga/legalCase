import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { ParticulierEmployeurCesuSectionComponent } from './particulier-employeur-cesu-section.component';
import { ParticulierEmployeurCesuResponse } from '../../core/models/particulier-employeur-cesu.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('ParticulierEmployeurCesuSectionComponent', () => {
  let component: ParticulierEmployeurCesuSectionComponent;
  let fixture: ComponentFixture<ParticulierEmployeurCesuSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/particulier-employeur-cesu-analysis';

  function reguliereResponse(overrides: Partial<ParticulierEmployeurCesuResponse> = {}): ParticulierEmployeurCesuResponse {
    return {
      caseFileId: 'case-1',
      dateEntree: '2020-01-01',
      dateRupture: '2023-06-15',
      categorieEmploye: 'SALARIE_PARTICULIER_EMPLOYEUR',
      causeRupture: 'LICENCIEMENT_MOTIF_PERSONNEL',
      salaireMensuelMoyen: 1200,
      dureePreavisJours: 60,
      dureePreavisLibelle: '2 mois',
      eligibiliteIndemnite: 'DUE',
      motifNonDue: null,
      indemniteLicenciement: 900,
      methodeCalcul: 'CONVENTIONNEL_PE',
      verdictGlobal: 'RUPTURE_REGULIERE',
      country: 'FRANCE',
      baseJuridique: 'CCN salariés du particulier employeur (IDCC 3239, 2021) (à vérifier par avocat)',
      ...overrides,
    };
  }

  function assmatResponse(): ParticulierEmployeurCesuResponse {
    return reguliereResponse({
      categorieEmploye: 'ASSISTANT_MATERNEL',
      causeRupture: 'RETRAIT_ENFANT',
      methodeCalcul: 'INDEMNITE_RUPTURE_ASSMAT',
      indemniteLicenciement: 1450,
    });
  }

  function nonDueResponse(): ParticulierEmployeurCesuResponse {
    return reguliereResponse({
      causeRupture: 'FAUTE_GRAVE',
      eligibiliteIndemnite: 'NON_DUE',
      motifNonDue: 'Faute grave — indemnité non due',
      indemniteLicenciement: 0,
      verdictGlobal: 'INDEMNITE_NON_DUE',
    });
  }

  function flush404(): void {
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function fillValidForm(): void {
    component.dateEntree.set('2020-01-01');
    component.dateRupture.set('2023-06-15');
    component.categorieEmploye.set('SALARIE_PARTICULIER_EMPLOYEUR');
    component.causeRupture.set('LICENCIEMENT_MOTIF_PERSONNEL');
    component.salaireMensuelMoyen.set(1200);
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [ParticulierEmployeurCesuSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ParticulierEmployeurCesuSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  // --- statics / contract ---

  it('exposes TOOL_LABEL and TOOL_ICON statics', () => {
    expect(ParticulierEmployeurCesuSectionComponent.TOOL_LABEL).toContain('PARTICULIER EMPLOYEUR');
    expect(ParticulierEmployeurCesuSectionComponent.TOOL_ICON).toBe('home');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(ParticulierEmployeurCesuSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 3 (nominal) with the three fields', () => {
    expect(ParticulierEmployeurCesuSectionComponent.getPrefillCount({
      aiData: { dateEntree: '2020-01-01', dateLicenciement: '2023-06-15', cesuCategorieEmploye: 'ASSISTANT_MATERNEL' },
      workspaceCountry: 'FRANCE',
    })).toBe(3);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(ParticulierEmployeurCesuSectionComponent.getPrefillCount({
      aiData: { dateEntree: '2020-01-01', dateLicenciement: '2023-06-15', cesuCategorieEmploye: 'ASSISTANT_MATERNEL' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  // --- gate pays / lifecycle ---

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

  it('BELGIQUE workspace shows FR gate banner instead of form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="country-gate-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('français uniquement');
  });

  it('loads existing analysis on GET 200', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(reguliereResponse());
    expect(component.result()!.verdictGlobal).toBe('RUPTURE_REGULIERE');
    expect(component.showForm()).toBe(false);
    expect(component.dateEntree()).toBe('2020-01-01');
    expect(component.categorieEmploye()).toBe('SALARIE_PARTICULIER_EMPLOYEUR');
    expect(component.salaireMensuelMoyen()).toBe(1200);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // --- form validation ---

  it('formValid requires both dates + positive salaire', () => {
    component.dateEntree.set(null);
    component.dateRupture.set('2023-06-15');
    component.salaireMensuelMoyen.set(1200);
    expect(component.formValid()).toBe(false);
    component.dateEntree.set('2020-01-01');
    expect(component.formValid()).toBe(true);
    component.salaireMensuelMoyen.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('formValid rejects dateRupture before dateEntree', () => {
    component.dateEntree.set('2023-06-15');
    component.dateRupture.set('2020-01-01');
    component.salaireMensuelMoyen.set(1200);
    expect(component.formValid()).toBe(false);
    expect(component.coherenceAlerts().length).toBeGreaterThan(0);
  });

  it('formValid rejects RETRAIT_ENFANT with SALARIE_PARTICULIER_EMPLOYEUR', () => {
    fillValidForm();
    component.categorieEmploye.set('ASSISTANT_MATERNEL');
    component.causeRupture.set('RETRAIT_ENFANT');
    expect(component.formValid()).toBe(true);
    // now force the incoherent combo directly
    component.categorieEmploye.set('SALARIE_PARTICULIER_EMPLOYEUR');
    // onCategorieChange would reset, but a direct set leaves it incoherent
    expect(component.formValid()).toBe(false);
  });

  // --- RETRAIT_ENFANT conditioning ---

  it('retraitEnfantDisabled true unless categorie = ASSISTANT_MATERNEL', () => {
    component.categorieEmploye.set('SALARIE_PARTICULIER_EMPLOYEUR');
    expect(component.retraitEnfantDisabled()).toBe(true);
    component.categorieEmploye.set('ASSISTANT_MATERNEL');
    expect(component.retraitEnfantDisabled()).toBe(false);
  });

  it('onCategorieChange resets RETRAIT_ENFANT cause when leaving ASSISTANT_MATERNEL', () => {
    component.categorieEmploye.set('ASSISTANT_MATERNEL');
    component.causeRupture.set('RETRAIT_ENFANT');
    component.onCategorieChange('SALARIE_PARTICULIER_EMPLOYEUR');
    expect(component.causeRupture()).toBe('LICENCIEMENT_MOTIF_PERSONNEL');
  });

  // --- analyze ---

  it('analyze() POST nominal -> result + snack + refresh + exact body', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateEntree: '2020-01-01',
      dateRupture: '2023-06-15',
      categorieEmploye: 'SALARIE_PARTICULIER_EMPLOYEUR',
      causeRupture: 'LICENCIEMENT_MOTIF_PERSONNEL',
      salaireMensuelMoyen: 1200,
    });
    req.flush(reguliereResponse());
    expect(component.result()!.verdictGlobal).toBe('RUPTURE_REGULIERE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('analyze() error -> snack error, stays in form', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    req.flush({ message: 'Boom' }, { status: 400, statusText: 'Bad Request' });
    expect(component.analyzing()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  // --- result rendering : montants + régime + verdict ---

  it('renders préavis, indemnité montant and méthode (CONVENTIONNEL_PE)', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(reguliereResponse());
    component.collapsed.set(false);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="preavis-libelle"]')!.textContent).toContain('2 mois');
    expect(el.querySelector('[data-testid="indemnite-licenciement"]')!.textContent).toContain('900');
    expect(el.querySelector('[data-testid="methode-calcul"]')!.textContent).toContain('CCN particulier employeur');
    expect(el.querySelector('[data-testid="eligibilite-chip"]')!.textContent).toContain('due');
    expect(el.querySelector('[data-testid="verdict-chip"]')!.textContent).toContain('régulière');
    expect(el.querySelector('[data-testid="bareme-note"]')!.textContent).toContain('actualiser annuellement');
  });

  it('renders assmat régime (INDEMNITE_RUPTURE_ASSMAT 1/80)', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(assmatResponse());
    component.collapsed.set(false);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="methode-calcul"]')!.textContent).toContain('1/80');
    expect(el.querySelector('[data-testid="indemnite-licenciement"]')!.textContent).toContain('1450');
  });

  it('renders NON_DUE chip (danger) with motif when faute grave', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(nonDueResponse());
    component.collapsed.set(false);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="eligibilite-chip"]')!;
    expect(chip.textContent).toContain('non due');
    expect(chip.className).toContain('pec-chip--danger');
    expect(el.querySelector('[data-testid="motif-non-due"]')!.textContent).toContain('Faute grave');
  });

  it('eligibiliteChipClass maps DUE->success, NON_DUE->danger', () => {
    expect(component.eligibiliteChipClass('DUE')).toContain('success');
    expect(component.eligibiliteChipClass('NON_DUE')).toContain('danger');
  });

  it('chipClass maps verdict global states', () => {
    expect(component.chipClass('RUPTURE_REGULIERE')).toContain('navy');
    expect(component.chipClass('RUPTURE_A_SECURISER')).toContain('warning');
    expect(component.chipClass('INDEMNITE_NON_DUE')).toContain('danger');
  });

  // --- pré-fill IA ---

  it('pré-fills dateEntree, dateRupture and categorie from aiData (with provenance)', () => {
    const aiData: TravailExtractedData = {
      dateEntree: '2019-03-01',
      dateLicenciement: '2024-02-10',
      cesuCategorieEmploye: 'ASSISTANT_MATERNEL',
    };
    component.aiData = aiData;
    component.ngOnInit();
    flush404();
    expect(component.dateEntree()).toBe('2019-03-01');
    expect(component.dateRupture()).toBe('2024-02-10');
    expect(component.categorieEmploye()).toBe('ASSISTANT_MATERNEL');
    expect(component.provenanceDateEntree()).toBe('IA');
    expect(component.provenanceDateRupture()).toBe('IA');
    expect(component.provenanceCategorie()).toBe('IA');
  });

  it('ngOnChanges aiData triggers pré-fill while in form mode', () => {
    const aiData: TravailExtractedData = { dateEntree: '2021-05-05' };
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, true) });
    expect(component.dateEntree()).toBe('2021-05-05');
    expect(component.provenanceDateEntree()).toBe('IA');
  });

  it('onDateEntreeChange clears provenance', () => {
    component.provenanceDateEntree.set('IA');
    component.onDateEntreeChange('2022-01-01');
    expect(component.dateEntree()).toBe('2022-01-01');
    expect(component.provenanceDateEntree()).toBeNull();
  });
});
