import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  LicenciementBeStatutUniquePreavisSectionComponent,
} from './licenciement-be-statut-unique-preavis-section.component';
import {
  LicenciementBeStatutUniquePreavisResponse,
} from '../../core/models/licenciement-be-statut-unique-preavis.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('LicenciementBeStatutUniquePreavisSectionComponent', () => {
  let component: LicenciementBeStatutUniquePreavisSectionComponent;
  let fixture: ComponentFixture<LicenciementBeStatutUniquePreavisSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/licenciement-be-statut-unique-preavis';

  function response(
    overrides: Partial<LicenciementBeStatutUniquePreavisResponse> = {},
  ): LicenciementBeStatutUniquePreavisResponse {
    return {
      caseFileId: 'case-1',
      ancienneteAnnees: 5,
      ancienneteMoisSupplementaires: 6,
      salaireHebdomadaireBrut: 800,
      dateNotificationLicenciement: '2026-03-15',
      partieStatutUniqueSeulement: true,
      dureePreavisEnSemaines: 18,
      dateFinPreavis: '2026-07-19',
      indemniteCompensatoire: 14400,
      formuleCalcul: 'Statut unique 5a 6m → 18 sem (art. 37/2 §1er)',
      baseJuridique: 'Loi 03/07/1978 art. 37/2 §1er ; Loi 26/12/2013',
      avertissement: null,
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        LicenciementBeStatutUniquePreavisSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(LicenciementBeStatutUniquePreavisSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Flush silencieusement les requêtes jurisprudence-citations émises par
    // le composant injecté `<app-tool-jurisprudence-citations>` (F-JU-03).
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  // ============================================================
  // Gate pays + init
  // ============================================================

  it('BELGIQUE → isAvailable() true, GET au ngOnInit', () => {
    expect(component.isAvailable()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  });

  it('FRANCE → isAvailable() false, aucun GET au ngOnInit', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.isAvailable()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone((r) => r.url === BASE_URL);
  });

  it('FRANCE → bannière « réservé droit BE » + masquage form', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.empty-result');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('belge');
  });

  it('GET 200 → hydrate result + mode résultat', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    expect(component.result()!.dureePreavisEnSemaines).toBe(18);
    expect(component.showForm()).toBe(false);
    expect(component.provenanceAnciennete()).toBeNull();
  });

  it('GET 404 → reste en mode formulaire', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ============================================================
  // Pré-fill IA
  // ============================================================

  it('pré-fill depuis aiData BE : ancienneté + salaire hebdo + dateNotification + partieStatutUnique=true', () => {
    component.aiData = {
      dateEntree: '2018-06-01',
      dateLicenciement: '2026-03-15',
      salaireBrutAnnuel: 52000,
    } as TravailExtractedData;
    component.ngOnInit();

    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });

    expect(component.ancienneteAnnees()).toBeGreaterThanOrEqual(7); // ~7 ans 9 mois
    expect(component.ancienneteMoisSupplementaires()).toBeGreaterThanOrEqual(0);
    expect(component.salaireHebdomadaireBrut()).toBe(1000); // 52000 / 52
    expect(component.dateNotificationLicenciement()).toBe('2026-03-15');
    expect(component.partieStatutUniqueSeulement()).toBe(true);
    expect(component.provenanceAnciennete()).toBe('IA');
    expect(component.provenanceSalaireHebdo()).toBe('IA');
    expect(component.provenanceDateNotification()).toBe('IA');
    expect(component.provenancePartieStatutUnique()).toBe('IA');
  });

  it('contrat pré-2014 → partieStatutUniqueSeulement=false dérivé', () => {
    component.aiData = {
      dateEntree: '2010-06-01',
      dateLicenciement: '2026-03-15',
      salaireBrutAnnuel: 52000,
    } as TravailExtractedData;
    component.ngOnInit();

    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });

    expect(component.partieStatutUniqueSeulement()).toBe(false);
    expect(component.provenancePartieStatutUnique()).toBe('IA');
  });

  it('FRANCE → aucun pré-fill', () => {
    component.workspaceCountry = 'FRANCE';
    component.aiData = {
      dateEntree: '2018-06-01',
      salaireBrutAnnuel: 52000,
    } as TravailExtractedData;
    component.ngOnInit();
    // Pas de GET attendu sur ce dossier FR.
    httpMock.expectNone((r) => r.url === BASE_URL);
    expect(component.ancienneteAnnees()).toBeNull();
    expect(component.salaireHebdomadaireBrut()).toBeNull();
  });

  // ============================================================
  // formValid()
  // ============================================================

  it('formValid false si ancienneté absente ou hors bornes', () => {
    component.salaireHebdomadaireBrut.set(800);
    component.dateNotificationLicenciement.set('2026-03-15');

    component.ancienneteAnnees.set(null);
    expect(component.formValid()).toBe(false);

    component.ancienneteAnnees.set(-1);
    expect(component.formValid()).toBe(false);

    component.ancienneteAnnees.set(81);
    expect(component.formValid()).toBe(false);

    component.ancienneteAnnees.set(5);
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si mois hors bornes', () => {
    component.ancienneteAnnees.set(5);
    component.salaireHebdomadaireBrut.set(800);
    component.dateNotificationLicenciement.set('2026-03-15');

    component.ancienneteMoisSupplementaires.set(12);
    expect(component.formValid()).toBe(false);

    component.ancienneteMoisSupplementaires.set(-1);
    expect(component.formValid()).toBe(false);

    component.ancienneteMoisSupplementaires.set(11);
    expect(component.formValid()).toBe(true);

    component.ancienneteMoisSupplementaires.set(null);
    expect(component.formValid()).toBe(true);
  });

  it('formValid false si salaire ≤ 0 ou date manquante', () => {
    component.ancienneteAnnees.set(5);
    component.salaireHebdomadaireBrut.set(0);
    component.dateNotificationLicenciement.set('2026-03-15');
    expect(component.formValid()).toBe(false);

    component.salaireHebdomadaireBrut.set(800);
    component.dateNotificationLicenciement.set(null);
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // calculate()
  // ============================================================

  it('calculate() POST avec partieStatutUniqueSeulement=true par défaut', () => {
    component.ancienneteAnnees.set(5);
    component.ancienneteMoisSupplementaires.set(6);
    component.salaireHebdomadaireBrut.set(800);
    component.dateNotificationLicenciement.set('2026-03-15');
    // partieStatutUniqueSeulement défaut true.

    component.calculate();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      ancienneteAnnees: 5,
      ancienneteMoisSupplementaires: 6,
      salaireHebdomadaireBrut: 800,
      dateNotificationLicenciement: '2026-03-15',
      partieStatutUniqueSeulement: true,
    });
    req.flush(response());

    expect(component.result()!.dureePreavisEnSemaines).toBe(18);
    expect(component.showForm()).toBe(false);
  });

  it('calculate() FRANCE → snackbar erreur + pas de requête', () => {
    component.workspaceCountry = 'FRANCE';
    component.ancienneteAnnees.set(5);
    component.salaireHebdomadaireBrut.set(800);
    component.dateNotificationLicenciement.set('2026-03-15');
    component.calculate();
    expect(snackSpy.open).toHaveBeenCalled();
    httpMock.expectNone((r) => r.url === BASE_URL);
  });

  it('calculate() 400 → snackbar avec message backend', () => {
    component.ancienneteAnnees.set(5);
    component.salaireHebdomadaireBrut.set(800);
    component.dateNotificationLicenciement.set('2026-03-15');
    component.calculate();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Données invalides' }, { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      expect.stringContaining('Données invalides'),
      'Fermer',
      expect.objectContaining({ panelClass: 'snack-error' }),
    );
  });

  // ============================================================
  // Affichage résultat + badge + avertissement
  // ============================================================

  it('badge STATUT_UNIQUE_PUR (vert) si partieStatutUniqueSeulement=true', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    const r = response({ partieStatutUniqueSeulement: true });
    req.flush(r);
    const badge = component.badgeFromResponse(component.result()!);
    expect(badge).toBe('STATUT_UNIQUE_PUR');
    expect(component.badgeClass(badge)).toBe('verdict-ok');
    expect(component.humanizeStatutUniqueBadge(badge)).toContain('Statut unique');
  });

  it('badge MIXTE_CLAEYS (ambre) si partieStatutUniqueSeulement=false', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      partieStatutUniqueSeulement: false,
      avertissement: 'Contrat mixte : la fraction Claeys (pré-2014) doit être calculée séparément.',
    }));
    const badge = component.badgeFromResponse(component.result()!);
    expect(badge).toBe('MIXTE_CLAEYS_PLUS_STATUT_UNIQUE');
    expect(component.badgeClass(badge)).toBe('verdict-warn');
    expect(component.humanizeStatutUniqueBadge(badge)).toContain('mixte');
  });

  it('avertissement Claeys présent dans la réponse → exposé via result()', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      partieStatutUniqueSeulement: false,
      avertissement: 'Contrat mixte : la fraction Claeys (pré-2014) doit être calculée séparément.',
    }));
    expect(component.result()!.avertissement).toContain('Claeys');
  });

  it('pas d\'avertissement (statut unique pur) → result().avertissement null', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({ avertissement: null }));
    expect(component.result()!.avertissement).toBeNull();
  });

  // ============================================================
  // Statics F-177 / F-236
  // ============================================================

  it('static TOOL_LABEL et TOOL_ICON présents', () => {
    expect(LicenciementBeStatutUniquePreavisSectionComponent.TOOL_LABEL)
      .toBe('PRÉAVIS STATUT UNIQUE (BE)');
    expect(LicenciementBeStatutUniquePreavisSectionComponent.TOOL_ICON)
      .toBe('event_busy');
  });

  it('static getPrefillCount délègue au helper (parité)', () => {
    expect(LicenciementBeStatutUniquePreavisSectionComponent.getPrefillCount({
      aiData: {} as TravailExtractedData,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);

    expect(LicenciementBeStatutUniquePreavisSectionComponent.getPrefillCount({
      aiData: {
        dateEntree: '2018-06-01',
        dateLicenciement: '2026-03-15',
        salaireBrutAnnuel: 52000,
      } as TravailExtractedData,
      workspaceCountry: 'BELGIQUE',
    })).toBe(3);
  });

  it('static getPrefillCount FRANCE → 0', () => {
    expect(LicenciementBeStatutUniquePreavisSectionComponent.getPrefillCount({
      aiData: {
        dateEntree: '2018-06-01',
        salaireBrutAnnuel: 52000,
      } as TravailExtractedData,
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });
});
