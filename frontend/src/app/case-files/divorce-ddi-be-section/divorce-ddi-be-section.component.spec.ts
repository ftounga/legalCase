import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { DivorceDdiBeSectionComponent } from './divorce-ddi-be-section.component';

describe('DivorceDdiBeSectionComponent', () => {
  let component: DivorceDdiBeSectionComponent;
  let fixture: ComponentFixture<DivorceDdiBeSectionComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DivorceDdiBeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
    }).compileComponents();
    fixture = TestBed.createComponent(DivorceDdiBeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  it('rendu nominal : cadre juridique art. 229 et notes visibles', () => {
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('229');
    expect(html.textContent).toContain('Backend opérationnel');
  });

  it('toggle collapsed cache puis réaffiche le contenu', () => {
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    fixture.detectChanges();
    expect(component.collapsed()).toBe(true);
    expect((fixture.nativeElement as HTMLElement).querySelector('#ddibe-body')).toBeNull();
    component.toggleCollapse();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('#ddibe-body')).not.toBeNull();
  });

  it('forceExpanded=true au mount → collapsed reste false', () => {
    const f = TestBed.createComponent(DivorceDdiBeSectionComponent);
    f.componentInstance.caseFileId = 'case-1';
    f.componentInstance.collapsed.set(true);
    f.componentRef.setInput('forceExpanded', true);
    f.componentInstance.ngOnInit();
    expect(f.componentInstance.collapsed()).toBe(false);
  });

  it('expose statics TOOL_LABEL, TOOL_ICON, PREFILL_COUNT_ALWAYS_ZERO, getPrefillCount', () => {
    expect(DivorceDdiBeSectionComponent.TOOL_LABEL).toBe('DIVORCE DDI 3 VOIES (BE)');
    expect(DivorceDdiBeSectionComponent.TOOL_ICON).toBe('rule');
    expect(DivorceDdiBeSectionComponent.PREFILL_COUNT_ALWAYS_ZERO).toBe(true);
    expect(DivorceDdiBeSectionComponent.getPrefillCount()).toBe(0);
  });
});
