import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RuptureAmiableInfoSectionComponent } from './rupture-amiable-info-section.component';

describe('RuptureAmiableInfoSectionComponent', () => {
  let component: RuptureAmiableInfoSectionComponent;
  let fixture: ComponentFixture<RuptureAmiableInfoSectionComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RuptureAmiableInfoSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
    }).compileComponents();
    fixture = TestBed.createComponent(RuptureAmiableInfoSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it('rendu nominal : les 2 messages sont visibles', () => {
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain("Aucun barème légal ne s'impose");
    expect(html.textContent).toContain("indemnité compensatoire de préavis");
  });

  it('toggle collapsed cache le contenu', () => {
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(true);
    fixture.detectChanges();
    const body = (fixture.nativeElement as HTMLElement).querySelector('#rai-body');
    expect(body).toBeNull();
  });

  it('ne déclenche aucun appel HTTP', () => {
    // Le composant est purement informationnel — aucun service/appel attendu
    httpMock.expectNone(() => true);
  });

  // F-177 SF-177-11 : pattern B (statics + forceExpanded)
  it('SF-177-11 T-06 : expose TOOL_LABEL et TOOL_ICON statics', () => {
    expect(RuptureAmiableInfoSectionComponent.TOOL_LABEL).toBe('RUPTURE AMIABLE');
    expect(RuptureAmiableInfoSectionComponent.TOOL_ICON).toBe('handshake');
  });

  it('SF-177-11 T-04 : forceExpanded=true au mount → collapsed reste false', () => {
    const f = TestBed.createComponent(RuptureAmiableInfoSectionComponent);
    f.componentInstance.caseFileId = 'case-1';
    f.componentInstance.forceExpanded = true;
    f.componentInstance.collapsed.set(true);
    f.componentRef.setInput('forceExpanded', true);
    f.componentInstance.ngOnInit();
    expect(f.componentInstance.collapsed()).toBe(false);
  });

  it('SF-177-11 T-05 : forceExpanded passe à true en cours de vie → re-expand', () => {
    component.collapsed.set(true);
    fixture.componentRef.setInput('forceExpanded', true);
    fixture.detectChanges();
    expect(component.collapsed()).toBe(false);
  });
});
