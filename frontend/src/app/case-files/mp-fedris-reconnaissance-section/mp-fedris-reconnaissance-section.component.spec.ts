import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  MpFedrisReconnaissanceSectionComponent,
} from './mp-fedris-reconnaissance-section.component';
import {
  MpFedrisReconnaissanceResponse,
} from '../../core/models/mp-fedris-reconnaissance.model';

describe('MpFedrisReconnaissanceSectionComponent', () => {
  let component: MpFedrisReconnaissanceSectionComponent;
  let fixture: ComponentFixture<MpFedrisReconnaissanceSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: { open: jest.Mock };

  const BASE_URL =
    '/api/v1/case-files/case-1/decision-tools/mp-fedris-reconnaissance';

  /**
   * Reponse backend par defaut — verdict MALADIE_LISTE_FERMEE_PRESOMPTION
   * (cas favorable typique : maladie liste fermee + exposition averee,
   * presomption art. 32 lois coordonnees 03/06/1970 joue).
   */
  function response(
    overrides: Partial<MpFedrisReconnaissanceResponse> = {},
  ): MpFedrisReconnaissanceResponse {
    return {
      caseFileId: 'case-1',
      typeMaladie: 'LISTE_FERMEE',
      codeMaladieListe: '1.404.01 silicose',
      libelleMaladie: 'Silicose pulmonaire',
      dateConstatationMedicale: '2024-03-15',
      dateConnaissanceProfessionnel: '2024-06-10',
      dateDeclarationEnvisagee: '2026-06-01',
      exposition: 'AVEREE',
      causalite: 'NON_APPLICABLE',
      verdict: 'MALADIE_LISTE_FERMEE_PRESOMPTION',
      presomptionApplicable: true,
      causaliteRequise: false,
      datePrescription: '2027-06-10',
      prescriteAuJour: false,
      joursAvantPrescription: 374,
      raison: 'LISTE_FERMEE_EXPOSITION_AVEREE_PRESOMPTION_ART_32',
      synthese: 'Silicose section 1.404.01 — presomption art. 32 lois coordonnees.',
      baseJuridique: 'Lois coordonnees du 03/06/1970 + AR du 28/03/1969 + AR du 16/12/1985.',
      avertissement: 'Verifier la duree minimale d\'exposition pour la pathologie.',
      ...overrides,
    };
  }

  beforeEach(async () => {
    snackSpy = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [
        MpFedrisReconnaissanceSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(MpFedrisReconnaissanceSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  // ============================================================
  // Gate pays + init
  // ============================================================

  it('BELGIQUE => isAvailable() true, GET au ngOnInit', () => {
    expect(component.isAvailable()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  });

  it('FRANCE => isAvailable() false, aucun GET au ngOnInit', () => {
    component.workspaceCountry = 'FRANCE';
    expect(component.isAvailable()).toBe(false);
    component.ngOnInit();
    httpMock.expectNone((r) => r.url === BASE_URL);
  });

  it('FRANCE => banniere "reserve droit BE" + masquage form', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.empty-result');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('belge');
    expect(banner.textContent).toContain('CPAM');
  });

  // ============================================================
  // GET initial
  // ============================================================

  it('GET 200 => hydrate result + mode resultat + rehydrate form', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    expect(component.result()!.verdict).toBe('MALADIE_LISTE_FERMEE_PRESOMPTION');
    expect(component.showForm()).toBe(false);
    expect(component.typeMaladie()).toBe('LISTE_FERMEE');
    expect(component.codeMaladieListe()).toBe('1.404.01 silicose');
    expect(component.libelleMaladie()).toBe('Silicose pulmonaire');
    expect(component.dateConstatationMedicale()).toBe('2024-03-15');
    expect(component.dateConnaissanceProfessionnel()).toBe('2024-06-10');
    expect(component.dateDeclarationEnvisagee()).toBe('2026-06-01');
    expect(component.exposition()).toBe('AVEREE');
    expect(component.causalite()).toBe('NON_APPLICABLE');
  });

  it('GET 404 => reste en mode formulaire vierge', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.result()).toBeNull();
    expect(component.showForm()).toBe(true);
  });

  // ============================================================
  // Form valid
  // ============================================================

  it('formValid() : false sans champs', () => {
    expect(component.formValid()).toBe(false);
  });

  function fillForm() {
    component.typeMaladie.set('LISTE_FERMEE');
    component.codeMaladieListe.set('1.404.01');
    component.libelleMaladie.set('Silicose pulmonaire');
    component.dateConstatationMedicale.set('2024-03-15');
    component.dateConnaissanceProfessionnel.set('2024-06-10');
    component.dateDeclarationEnvisagee.set('2026-06-01');
    component.exposition.set('AVEREE');
    component.causalite.set('NON_APPLICABLE');
  }

  it('formValid() : true avec les champs requis remplis (LISTE_FERMEE)', () => {
    fillForm();
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false sans typeMaladie', () => {
    fillForm();
    component.typeMaladie.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans libelleMaladie', () => {
    fillForm();
    component.libelleMaladie.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false avec libelleMaladie vide', () => {
    fillForm();
    component.libelleMaladie.set('   ');
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans dateConstatationMedicale', () => {
    fillForm();
    component.dateConstatationMedicale.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans exposition', () => {
    fillForm();
    component.exposition.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false sans causalite', () => {
    fillForm();
    component.causalite.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : false en LISTE_FERMEE sans codeMaladieListe', () => {
    fillForm();
    component.codeMaladieListe.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() : true en HORS_LISTE sans codeMaladieListe', () => {
    fillForm();
    component.typeMaladie.set('HORS_LISTE');
    component.codeMaladieListe.set(null);
    component.causalite.set('DIRECTE_DETERMINANTE');
    expect(component.formValid()).toBe(true);
  });

  it('formValid() : false si connaissance professionnel < constatation medicale', () => {
    fillForm();
    component.dateConnaissanceProfessionnel.set('2024-01-01');
    expect(component.formValid()).toBe(false);
  });

  // ============================================================
  // calculate() — POST + states
  // ============================================================

  it('calculate() => POST + snack succes', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.typeMaladie).toBe('LISTE_FERMEE');
    expect(post.request.body.codeMaladieListe).toBe('1.404.01');
    expect(post.request.body.libelleMaladie).toBe('Silicose pulmonaire');
    expect(post.request.body.exposition).toBe('AVEREE');
    expect(post.request.body.causalite).toBe('NON_APPLICABLE');
    post.flush(response());
    expect(component.result()).not.toBeNull();
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Analyse MP Fedris calculee', 'OK', expect.anything(),
    );
  });

  it('calculate() : erreur 400 => snack avec message backend', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({ message: 'libelleMaladie est requis' },
      { status: 400, statusText: 'Bad Request' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'libelleMaladie est requis', 'Fermer', expect.anything(),
    );
  });

  it('calculate() : erreur 500 => snack generique', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({}, { status: 500, statusText: 'Server Error' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Une erreur est survenue. Veuillez reessayer.', 'Fermer', expect.anything(),
    );
  });

  it('calculate() : erreur 404 => snack "Dossier introuvable"', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    fillForm();
    component.calculate();
    const post = httpMock.expectOne(BASE_URL);
    post.flush({}, { status: 404, statusText: 'Not Found' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Dossier introuvable', 'Fermer', expect.anything(),
    );
  });

  it('calculate() : FRANCE => snack indispo + pas de POST', () => {
    component.workspaceCountry = 'FRANCE';
    fillForm();
    component.calculate();
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Outil indisponible pour ce workspace', 'Fermer', expect.anything(),
    );
    httpMock.expectNone((r) => r.url === BASE_URL && r.method === 'POST');
  });

  // ============================================================
  // Verdict — label / classe / icone (6 cas)
  // ============================================================

  it('verdict MALADIE_LISTE_FERMEE_PRESOMPTION => info, check_circle', () => {
    component.result.set(response());
    expect(component.verdictLabel()).toContain('presomption');
    expect(component.verdictClass()).toBe('verdict-info');
    expect(component.verdictIcon()).toBe('check_circle');
  });

  it('verdict MALADIE_LISTE_FERMEE_EXPOSITION_INSUFFISANTE => warn, description', () => {
    component.result.set(response({
      verdict: 'MALADIE_LISTE_FERMEE_EXPOSITION_INSUFFISANTE',
      exposition: 'INSUFFISANTE',
      presomptionApplicable: false,
    }));
    expect(component.verdictLabel()).toContain('exposition');
    expect(component.verdictClass()).toBe('verdict-warn');
    expect(component.verdictIcon()).toBe('description');
  });

  it('verdict SYSTEME_OUVERT_CAUSALITE_DIRECTE_DETERMINANTE => info, verified', () => {
    component.result.set(response({
      verdict: 'SYSTEME_OUVERT_CAUSALITE_DIRECTE_DETERMINANTE',
      typeMaladie: 'HORS_LISTE',
      codeMaladieListe: null,
      exposition: 'AVEREE',
      causalite: 'DIRECTE_DETERMINANTE',
      presomptionApplicable: false,
      causaliteRequise: true,
    }));
    expect(component.verdictLabel()).toContain('causalite directe et determinante');
    expect(component.verdictClass()).toBe('verdict-info');
    expect(component.verdictIcon()).toBe('verified');
  });

  it('verdict SYSTEME_OUVERT_CAUSALITE_INSUFFISANTE => critical, report', () => {
    component.result.set(response({
      verdict: 'SYSTEME_OUVERT_CAUSALITE_INSUFFISANTE',
      typeMaladie: 'HORS_LISTE',
      codeMaladieListe: null,
      causalite: 'INSUFFISANTE',
      presomptionApplicable: false,
      causaliteRequise: true,
    }));
    expect(component.verdictLabel()).toContain('causal direct');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('report');
  });

  it('verdict DECLARATION_PRESCRITE => critical, event_busy', () => {
    component.result.set(response({
      verdict: 'DECLARATION_PRESCRITE',
      datePrescription: '2024-01-15',
      prescriteAuJour: true,
      joursAvantPrescription: -500,
    }));
    expect(component.verdictLabel()).toContain('prescription');
    expect(component.verdictClass()).toBe('verdict-critical');
    expect(component.verdictIcon()).toBe('event_busy');
  });

  it('verdict A_QUALIFIER => neutral, help_outline', () => {
    component.result.set(response({
      verdict: 'A_QUALIFIER',
      exposition: 'INDETERMINEE',
      presomptionApplicable: false,
    }));
    expect(component.verdictLabel()).toContain('Qualification ouverte');
    expect(component.verdictClass()).toBe('verdict-neutral');
    expect(component.verdictIcon()).toBe('help_outline');
  });

  it('result null => labels neutres', () => {
    expect(component.verdictLabel()).toBe('—');
    expect(component.verdictClass()).toBe('');
    expect(component.verdictIcon()).toBe('medical_services');
  });

  // ============================================================
  // Badge dans header (6 cas)
  // ============================================================

  it('badge MALADIE_LISTE_FERMEE_PRESOMPTION = info', () => {
    expect(component.verdictBadge('MALADIE_LISTE_FERMEE_PRESOMPTION')).toContain('PRESOMPTION');
    expect(component.verdictBadgeClass('MALADIE_LISTE_FERMEE_PRESOMPTION')).toBe('badge-info');
  });

  it('badge MALADIE_LISTE_FERMEE_EXPOSITION_INSUFFISANTE = warn', () => {
    expect(component.verdictBadge('MALADIE_LISTE_FERMEE_EXPOSITION_INSUFFISANTE')).toContain('EXPOSITION');
    expect(component.verdictBadgeClass('MALADIE_LISTE_FERMEE_EXPOSITION_INSUFFISANTE')).toBe('badge-warn');
  });

  it('badge SYSTEME_OUVERT_CAUSALITE_DIRECTE_DETERMINANTE = info', () => {
    expect(component.verdictBadge('SYSTEME_OUVERT_CAUSALITE_DIRECTE_DETERMINANTE')).toContain('ETABLIE');
    expect(component.verdictBadgeClass('SYSTEME_OUVERT_CAUSALITE_DIRECTE_DETERMINANTE')).toBe('badge-info');
  });

  it('badge SYSTEME_OUVERT_CAUSALITE_INSUFFISANTE = critical', () => {
    expect(component.verdictBadge('SYSTEME_OUVERT_CAUSALITE_INSUFFISANTE')).toContain('INSUFFISANTE');
    expect(component.verdictBadgeClass('SYSTEME_OUVERT_CAUSALITE_INSUFFISANTE')).toBe('badge-critical');
  });

  it('badge DECLARATION_PRESCRITE = critical', () => {
    expect(component.verdictBadge('DECLARATION_PRESCRITE')).toBe('PRESCRITE');
    expect(component.verdictBadgeClass('DECLARATION_PRESCRITE')).toBe('badge-critical');
  });

  it('badge A_QUALIFIER = neutre', () => {
    expect(component.verdictBadge('A_QUALIFIER')).toBe('A QUALIFIER');
    expect(component.verdictBadgeClass('A_QUALIFIER')).toBe('badge-neutral');
  });

  // ============================================================
  // DOM — rendu verdict
  // ============================================================

  it('MALADIE_LISTE_FERMEE_PRESOMPTION => badge info + 9 ventilations (avec date connaissance)', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response());
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('.section-badge.badge-info');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toContain('PRESOMPTION');
    const ventilations = fixture.nativeElement.querySelectorAll('.ventilation-row');
    // 6 fixes + date connaissance (1) + 2 prescription = 9.
    expect(ventilations.length).toBe(9);
  });

  it('Reponse sans dateConnaissanceProfessionnel => 8 ventilations', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({ dateConnaissanceProfessionnel: null }));
    fixture.detectChanges();
    const ventilations = fixture.nativeElement.querySelectorAll('.ventilation-row');
    expect(ventilations.length).toBe(8);
  });

  // ============================================================
  // Libelles
  // ============================================================

  it('libelle typeMaladie : 2 cas + null', () => {
    expect(component.typeMaladieLabel('LISTE_FERMEE')).toContain('Liste fermee');
    expect(component.typeMaladieLabel('HORS_LISTE')).toContain('Hors liste');
    expect(component.typeMaladieLabel(null)).toBe('—');
  });

  it('libelle exposition : 3 cas + null', () => {
    expect(component.expositionLabel('AVEREE')).toContain('Averee');
    expect(component.expositionLabel('INSUFFISANTE')).toContain('Insuffisante');
    expect(component.expositionLabel('INDETERMINEE')).toContain('Indeterminee');
    expect(component.expositionLabel(null)).toBe('—');
  });

  it('libelle causalite : 4 cas + null', () => {
    expect(component.causaliteLabel('DIRECTE_DETERMINANTE')).toContain('Directe');
    expect(component.causaliteLabel('INSUFFISANTE')).toContain('Insuffisante');
    expect(component.causaliteLabel('INDETERMINEE')).toContain('Indeterminee');
    expect(component.causaliteLabel('NON_APPLICABLE')).toContain('Non applicable');
    expect(component.causaliteLabel(null)).toBe('—');
  });

  // ============================================================
  // Formats
  // ============================================================

  it('formatDate : ISO -> jj/mm/aaaa', () => {
    expect(component.formatDate('2024-03-15')).toBe('15/03/2024');
    expect(component.formatDate(null)).toBe('—');
    expect(component.formatDate(undefined)).toBe('—');
    expect(component.formatDate('')).toBe('—');
  });

  it('formatJoursPrescription : positif / negatif / 0', () => {
    expect(component.formatJoursPrescription(0)).toContain('echeance');
    expect(component.formatJoursPrescription(1)).toContain('jour restant');
    expect(component.formatJoursPrescription(30)).toContain('jours restant');
    expect(component.formatJoursPrescription(-1)).toContain('jour ecoule');
    expect(component.formatJoursPrescription(-30)).toContain('jours ecoule');
    expect(component.formatJoursPrescription(null)).toBe('—');
    expect(component.formatJoursPrescription(undefined)).toBe('—');
  });

  it('formatBoolean : Oui / Non / —', () => {
    expect(component.formatBoolean(true)).toBe('Oui');
    expect(component.formatBoolean(false)).toBe('Non');
    expect(component.formatBoolean(null)).toBe('—');
    expect(component.formatBoolean(undefined)).toBe('—');
  });

  // ============================================================
  // Avertissement
  // ============================================================

  it('avertissement non vide => banniere ambre rendue', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      avertissement: 'Verifier indexation des decimes additionnels.',
    }));
    fixture.detectChanges();
    const alerts = fixture.nativeElement.querySelectorAll('.result-alert');
    const text = Array.from(alerts).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('indexation');
  });

  // ============================================================
  // Synthese
  // ============================================================

  it('synthese affichee si presente', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    const req = httpMock.expectOne(BASE_URL);
    req.flush(response({
      synthese: 'Silicose section 1.404 — presomption art. 32.',
    }));
    fixture.detectChanges();
    const founds = fixture.nativeElement.querySelectorAll('.result-fondement');
    const text = Array.from(founds).map((a: unknown) => (a as HTMLElement).textContent).join(' ');
    expect(text).toContain('Silicose');
  });

  // ============================================================
  // F-177 SF-177-12 — parite getPrefillCount static
  // ============================================================

  it('static getPrefillCount => 0 (V1)', () => {
    expect(MpFedrisReconnaissanceSectionComponent.getPrefillCount({})).toBe(0);
    expect(MpFedrisReconnaissanceSectionComponent.getPrefillCount({
      workspaceCountry: 'BELGIQUE',
      aiData: {
        dateNaissanceSalarie: '1965-04-12',
        dateRuptureContrat: '2026-09-30',
        motifRupture: 'autre',
        anneesCarriereSalarie: 30,
        salaireBrutMensuel: 2900,
        salaireBrutAnnuel: 34800,
      },
    })).toBe(0);
  });

  // ============================================================
  // editMode => re-affiche le form
  // ============================================================

  it('editMode() => showForm true', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  // ============================================================
  // F-JU-03 — citation jurisprudentielle bien cablee
  // ============================================================

  it('toolId pour jurisprudence = mp-fedris-reconnaissance', () => {
    expect((component as unknown as { toolIdForJurisprudence: string }).toolIdForJurisprudence)
      .toBe('mp-fedris-reconnaissance');
  });

  // ============================================================
  // Toggle collapse + forceExpanded
  // ============================================================

  it('toggleCollapse() flips collapsed signal', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
  });

  it('forceExpanded => collapsed=false au ngOnInit', () => {
    component.forceExpanded = true;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'nope' }, { status: 404, statusText: 'NF' });
    expect(component.collapsed()).toBe(false);
  });

  // ============================================================
  // Handlers signals
  // ============================================================

  it('handlers mettent a jour les signals correctement', () => {
    component.onTypeMaladieChange('LISTE_FERMEE');
    expect(component.typeMaladie()).toBe('LISTE_FERMEE');

    component.onCodeMaladieListeChange('2.3.1');
    expect(component.codeMaladieListe()).toBe('2.3.1');

    component.onLibelleMaladieChange('Mesotheliome');
    expect(component.libelleMaladie()).toBe('Mesotheliome');

    component.onDateConstatationMedicaleChange('2024-03-15');
    expect(component.dateConstatationMedicale()).toBe('2024-03-15');

    component.onDateConnaissanceProfessionnelChange('2024-06-10');
    expect(component.dateConnaissanceProfessionnel()).toBe('2024-06-10');

    component.onDateDeclarationEnvisageeChange('2026-06-01');
    expect(component.dateDeclarationEnvisagee()).toBe('2026-06-01');

    component.onExpositionChange('AVEREE');
    expect(component.exposition()).toBe('AVEREE');

    component.onCausaliteChange('DIRECTE_DETERMINANTE');
    expect(component.causalite()).toBe('DIRECTE_DETERMINANTE');

    component.onCodeMaladieListeChange('');
    expect(component.codeMaladieListe()).toBeNull();

    component.onLibelleMaladieChange('');
    expect(component.libelleMaladie()).toBeNull();
  });
});
