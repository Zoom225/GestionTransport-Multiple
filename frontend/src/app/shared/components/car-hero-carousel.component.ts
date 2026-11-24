import { DOCUMENT } from '@angular/common';
import { AfterViewInit, ChangeDetectionStrategy, Component, DestroyRef, ElementRef, computed, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LanguageService } from '../../core/i18n/language.service';

@Component({
  selector: 'app-car-hero-carousel',
  standalone: true,
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
:host { display: block; }
.car-hero {
  position: relative;
  display: grid;
  grid-template-rows: 1fr auto;
  isolation: isolate;
  background: var(--color-primary);
  color: var(--color-surface);
  min-height: clamp(39rem, 75svh, 49rem);
  overflow: clip;
  touch-action: pan-y pinch-zoom;
}
.carousel-photo, .hero-shade, .carousel-photos { position: absolute; inset: 0; pointer-events: none; }
.carousel-photo {
  opacity: 0;
  transition: opacity 650ms cubic-bezier(.22, .61, .36, 1);
}
.carousel-photo.active { opacity: 1; }
.carousel-photo picture, .carousel-photo img { display: block; width: 100%; height: 100%; }
.carousel-photo img { object-fit: cover; object-position: 64% 55%; }
.hero-shade {
  background:
    linear-gradient(90deg, var(--color-primary), rgb(16 36 58 / 96%) 20%, rgb(16 36 58 / 71%) 43%, rgb(16 36 58 / 7%) 74%),
    linear-gradient(0deg, rgb(16 36 58 / 96%), transparent 38%);
}
.hero-progress-track {
  position: absolute;
  inset: 0 0 auto;
  height: 2px;
  z-index: 1;
  background: rgb(245 247 250 / 14%);
  overflow: hidden;
}
.hero-progress-bar {
  height: 100%;
  background: var(--color-accent);
  transform-origin: left;
  animation: progressTick var(--carousel-period) linear infinite;
}
.hero-progress-track.paused .hero-progress-bar { animation: none; transform: scaleX(0); }
@keyframes progressTick { from { transform: scaleX(0); } to { transform: scaleX(1); } }
.hero-inner, .hero-footer {
  position: relative;
  width: min(1312px, calc(100% - var(--space-gutter) * 2));
  margin-inline: auto;
}
.hero-inner {
  display: flex;
  align-items: center;
  padding-block: clamp(3rem, 9vw, 7.5rem) 2.5rem;
}
.hero-copy { max-width: 805px; }
.hero-eyebrow {
  display: flex;
  align-items: center;
  gap: .7rem;
  color: var(--ui-hero-accent);
  text-transform: uppercase;
  letter-spacing: .17em;
  font-size: .74rem;
  font-weight: 750;
  margin: 0 0 1.35rem;
}
.hero-eyebrow::before { content: ''; width: 1.7rem; height: 2px; background: var(--color-accent); }
h1 {
  font-size: clamp(3rem, 5.55vw, 6.15rem);
  font-weight: 770;
  line-height: .99;
  letter-spacing: -.07em;
  margin: 0;
  text-wrap: initial;
}
h1 em { display: block; font-style: normal; color: var(--ui-hero-accent); }
.hero-lead {
  font-size: clamp(1rem, 1.45vw, 1.19rem);
  max-width: 40ch;
  line-height: 1.6;
  color: var(--color-surface);
  margin: 1.45rem 0 1.85rem;
}
.hero-actions, .hero-trust { display: flex; flex-wrap: wrap; gap: .7rem; }
.hero-actions a {
  display: inline-flex;
  min-height: 52px;
  align-items: center;
  justify-content: center;
  gap: .75rem;
  padding: .7rem 1.2rem;
  border: 1px solid transparent;
  border-radius: var(--ui-radius-button);
  font-weight: 750;
  text-decoration: none;
  transition: background var(--ui-motion), box-shadow var(--ui-motion), transform var(--ui-motion);
}
.hero-primary { color: var(--color-primary); background: var(--color-accent); box-shadow: var(--ui-shadow-action); }
.hero-actions .hero-secondary { color: var(--color-surface); border-color: rgb(245 247 250 / 58%); background: rgb(16 36 58 / 36%); }
.hero-trust {
  margin: 1.55rem 0 0;
  padding: 0;
  gap: .5rem 1.1rem;
  font-size: .8rem;
  color: var(--color-surface);
}
.hero-trust span { display: inline-flex; gap: .35rem; align-items: center; }
.hero-trust b { color: var(--ui-hero-accent); }
.hero-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: .7rem 1rem;
  flex-wrap: wrap;
  padding-bottom: 4.35rem;
}
.photo-credit { display: grid; gap: .12rem; font-size: .72rem; margin: 0; color: var(--color-surface); }
.photo-credit a { color: inherit; text-decoration-color: rgb(245 247 250 / 55%); }
.carousel-controls, .carousel-arrows, .carousel-dots { display: flex; align-items: center; }
.carousel-controls { flex-wrap: wrap; gap: .3rem .75rem; }
.carousel-arrows { gap: .35rem; }
.carousel-controls button {
  display: grid;
  place-items: center;
  width: 44px;
  min-width: 44px;
  height: 44px;
  padding: 0;
  border-radius: 50%;
  color: var(--color-surface);
  border: 1px solid rgb(245 247 250 / 54%);
  background: rgb(16 36 58 / 78%);
  cursor: pointer;
  transition: background var(--ui-motion), border-color var(--ui-motion), transform var(--ui-motion);
}
.carousel-controls button:disabled { cursor: not-allowed; opacity: .65; }
.carousel-dots button { background: transparent; border-color: transparent; border-radius: 10px; }
.carousel-dots span { width: 8px; height: 8px; border-radius: 9px; background: var(--color-surface); transition: width var(--ui-motion), background var(--ui-motion); }
.carousel-dots [aria-current=true] span { width: 24px; background: var(--color-accent); }
.car-hero :focus-visible { outline-color: var(--ui-hero-accent); outline-offset: 3px; }
.visually-hidden { position: absolute; width: 1px; height: 1px; margin: -1px; overflow: hidden; clip-path: inset(50%); white-space: nowrap; }
@media (hover: hover) {
  .hero-primary:hover { background: var(--color-accent-hover); }
  .hero-secondary:hover, .carousel-controls button:hover:not(:disabled) { background: var(--ui-primary-strong); border-color: var(--color-surface); }
  .hero-actions a:hover { transform: translateY(-2px); }
}
.hero-actions a:active { transform: translateY(0); }
@media (prefers-reduced-motion: no-preference) {
  .hero-copy { animation: hero-enter 480ms var(--ease-ui) both; }
}
@keyframes hero-enter { from { opacity: .84; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
@media (max-width: 720px) {
  .car-hero { min-height: auto; }
  .hero-inner { padding-block: 2.9rem clamp(11rem, 41vw, 17rem); align-items: start; }
  .carousel-photos { inset: auto 0 5.8rem; height: min(64%, 27rem); }
  .carousel-photo img { object-position: center 57%; }
  .hero-shade { background: linear-gradient(180deg, var(--color-primary) 31%, rgb(16 36 58 / 84%) 47%, transparent 68%, var(--color-primary) 91%); }
  h1 { font-size: clamp(2.63rem, 10vw, 4.3rem); }
  .hero-eyebrow { margin-bottom: .95rem; font-size: .68rem; letter-spacing: .12em; }
  .hero-lead { margin: 1rem 0 1.2rem; max-width: 36ch; }
  .hero-actions { max-width: 29rem; }
  .hero-actions a { flex: 1 0 min(100%, 14rem); }
  .hero-trust { margin-top: 1rem; text-shadow: 0 1px 4px var(--color-primary); }
  .hero-footer { padding-bottom: 2.65rem; }
}
@media (max-width: 365px) {
  .carousel-controls { justify-content: space-between; width: 100%; gap: .15rem; }
  .carousel-arrows { gap: .15rem; }
  .carousel-dots button { width: 36px; min-width: 36px; }
}
@media (prefers-reduced-motion: reduce) {
  .hero-actions a:hover { transform: none; }
  .hero-progress-bar { animation: none; transform: scaleX(0); }
}
  `],
  template: `
    <section class="car-hero" role="region" [attr.aria-label]="language.t('carousel.label')"
      [attr.aria-roledescription]="language.t('carousel.description')"
      [style.--carousel-period]="rotationMs + 'ms'"
      (pointerenter)="onHover($event)" (pointerleave)="hovered.set(false)"
      (pointerdown)="beginSwipe($event)" (pointerup)="endSwipe($event)" (pointercancel)="cancelSwipe()"
      (focusin)="onFocus($event)" (keydown)="onKey($event)">
      <div class="hero-progress-track" [class.paused]="!canRotate()" aria-hidden="true">
        <div class="hero-progress-bar"></div>
      </div>
      <div class="carousel-photos" aria-live="off">
        @for (slide of slides; track slide.name; let i = $index) {
          @if (prepared().has(i)) {
            <div class="carousel-photo" [class.active]="current() === i" [attr.aria-hidden]="current() !== i"
              role="group" [attr.aria-roledescription]="language.t('carousel.slide')"
              [attr.aria-label]="language.t('carousel.position', {current: i + 1, total: slides.length})">
              <picture>
                @if (!failed().has(i)) {
                  <source media="(max-width: 640px)" [srcset]="'images/car-' + slide.name + '-mobile.webp'" />
                }
                <img [src]="failed().has(i) ? 'images/mobilite-transport.svg' : 'images/car-' + slide.name + '-1024.webp'"
                  [attr.srcset]="failed().has(i) ? null : imageSet(slide.name)" sizes="100vw"
                  width="1920" height="1080" [alt]="language.t(failed().has(i) ? 'design.carAlt' : slide.alt)"
                  [attr.loading]="i === 0 ? 'eager' : 'lazy'" [attr.fetchpriority]="i === 0 ? 'high' : 'low'"
                  decoding="async" (load)="onImageLoad(i)" (error)="onImageError(i)" />
              </picture>
            </div>
          }
        }
      </div>
      <div class="hero-shade" aria-hidden="true"></div>
      <div class="hero-inner">
        <div class="hero-copy">
          <p class="hero-eyebrow">{{ language.t('transport.eyebrow') }}</p>
          <h1>{{ language.t('transport.together') }} <em>{{ language.t('transport.simply') }}</em></h1>
          <p class="hero-lead">{{ language.t('carousel.subtitle') }}</p>
          <div class="hero-actions">
            <a href="#recherche" class="hero-primary">{{ language.t('transport.search') }} <span aria-hidden="true">↓</span></a>
            <a routerLink="/voyageur/trajets/new" class="hero-secondary">{{ language.t('carousel.propose') }} <span aria-hidden="true">↗</span></a>
          </div>
          <div class="hero-trust">
            <span><b aria-hidden="true">✓</b> {{ language.t('transport.driverKnown') }}</span>
            <span><b aria-hidden="true">✓</b> {{ language.t('transport.maximum') }}</span>
            <span><b aria-hidden="true">✓</b> {{ language.t('transport.price') }}</span>
          </div>
        </div>
      </div>
      <div class="hero-footer">
        <p class="photo-credit">
          <span>{{ language.t('carousel.illustration') }}</span>
          @if (slides[current()].url; as url) {
            <a [href]="url" target="_blank" rel="noopener noreferrer">{{ slides[current()].author }} · Pexels <span aria-hidden="true">↗</span></a>
          } @else {
            <span>{{ language.t('carousel.generated') }}</span>
          }
        </p>
        <div class="carousel-controls" role="group" [attr.aria-label]="language.t('carousel.controls')">
          <div class="carousel-arrows">
            <button type="button" class="carousel-previous" (click)="select(current() - 1)" [attr.aria-label]="language.t('carousel.previous')"><span aria-hidden="true">←</span></button>
            <button type="button" class="carousel-pause" (click)="toggleRotation()" [disabled]="reducedMotion()"
              [attr.aria-label]="language.t(reducedMotion() ? 'carousel.motionOff' : paused() ? 'carousel.play' : 'carousel.pause')">
              <span aria-hidden="true">{{ paused() || reducedMotion() ? '▶' : 'Ⅱ' }}</span>
            </button>
            <button type="button" class="carousel-next" (click)="select(current() + 1)" [attr.aria-label]="language.t('carousel.next')"><span aria-hidden="true">→</span></button>
          </div>
          <div class="carousel-dots" role="group" [attr.aria-label]="language.t('carousel.pagination')">
            @for (slide of slides; track slide.name; let i = $index) {
              <button type="button" (click)="select(i)" [attr.aria-current]="current() === i ? 'true' : null"
                [attr.aria-label]="language.t('carousel.goTo', {current: i + 1, total: slides.length})"><span aria-hidden="true"></span></button>
            }
          </div>
        </div>
      </div>
      <p class="visually-hidden" role="status" aria-live="polite" aria-atomic="true">{{ announcement() }}</p>
    </section>
  `,
})
export class CarHeroCarouselComponent implements AfterViewInit {
  readonly language = inject(LanguageService);
  private readonly document = inject(DOCUMENT);
  private readonly destroyRef = inject(DestroyRef);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private swipeStart?: { pointerId: number; x: number; y: number };
  readonly rotationMs = 3500;
  readonly slides = [
    {name:'suv', alt:'carousel.suvAlt', author:'', url:null},
    {name:'electrique', alt:'carousel.electricAlt', author:'', url:null},
    {name:'route', alt:'carousel.roadAlt', author:'Kelvin Linhares', url:'https://www.pexels.com/photo/sleek-silver-car-on-scenic-country-road-28502253/'},
    {name:'famille', alt:'carousel.familyAlt', author:'Mikhail Nilov', url:'https://www.pexels.com/photo/a-family-standing-near-parked-car-while-looking-at-the-camera-8323226/'},
  ] as const;
  readonly current = signal(0);
  readonly paused = signal(false);
  readonly hovered = signal(false);
  readonly inViewport = signal(true);
  readonly hidden = signal(this.document.hidden);
  readonly reducedMotion = signal(false);
  readonly prepared = signal<ReadonlySet<number>>(new Set([0]));
  readonly failed = signal<ReadonlySet<number>>(new Set());
  readonly announcement = signal('');
  readonly canRotate = computed(() =>
    !this.paused() && !this.hovered() && this.inViewport() && !this.hidden() && !this.reducedMotion(),
  );

  constructor() {
    const motion = typeof window.matchMedia === 'function' ? window.matchMedia('(prefers-reduced-motion: reduce)') : null;
    this.reducedMotion.set(motion?.matches ?? false);
    const onMotion = (event: MediaQueryListEvent) => this.reducedMotion.set(event.matches);
    const onVisibility = () => this.hidden.set(this.document.hidden);
    motion?.addEventListener('change', onMotion);
    this.document.addEventListener('visibilitychange', onVisibility);
    this.destroyRef.onDestroy(() => {
      motion?.removeEventListener('change', onMotion);
      this.document.removeEventListener('visibilitychange', onVisibility);
    });
    effect((onCleanup) => {
      if (!this.canRotate()) return;
      const timer = window.setInterval(() => this.advance(this.current() + 1), this.rotationMs);
      onCleanup(() => window.clearInterval(timer));
    });
  }

  ngAfterViewInit(): void {
    if (typeof IntersectionObserver === 'undefined') return;
    const observer = new IntersectionObserver(([entry]) => {
      this.inViewport.set(entry.isIntersecting && entry.intersectionRatio >= .25);
    }, { threshold: .25 });
    observer.observe(this.host.nativeElement);
    this.destroyRef.onDestroy(() => observer.disconnect());
  }

  imageSet(name: string): string {
    const widths = name === 'suv' || name === 'electrique' ? [640, 1024, 1536] : [640, 1024, 1920];
    return widths.map(width => `images/car-${name}-${width}.webp ${width}w`).join(', ');
  }

  pause(): void { this.paused.set(true); }

  onHover(event: PointerEvent): void {
    if (event.pointerType !== 'touch') this.hovered.set(true);
  }

  onFocus(event: FocusEvent): void {
    if (!(event.relatedTarget instanceof Node) || !this.host.nativeElement.contains(event.relatedTarget)) {
      this.pause();
    }
  }

  beginSwipe(event: PointerEvent): void {
    if (event.pointerType !== 'touch' || (event.target instanceof Element && event.target.closest('button, a'))) return;
    this.swipeStart = { pointerId: event.pointerId, x: event.clientX, y: event.clientY };
  }

  cancelSwipe(): void { this.swipeStart = undefined; }

  endSwipe(event: PointerEvent): void {
    const start = this.swipeStart;
    this.cancelSwipe();
    if (!start || start.pointerId !== event.pointerId) return;
    const horizontal = event.clientX - start.x;
    const vertical = event.clientY - start.y;
    if (Math.abs(horizontal) >= 50 && Math.abs(horizontal) > Math.abs(vertical) * 1.35) {
      this.select(this.current() + (horizontal < 0 ? 1 : -1));
    }
  }

  toggleRotation(): void {
    if (!this.reducedMotion()) this.paused.update(paused => !paused);
  }

  select(index: number): void {
    this.pause();
    this.advance(index);
    this.announcement.set(this.language.t('carousel.position', {current:this.current()+1, total:this.slides.length}));
  }

  onKey(event: KeyboardEvent): void {
    const index = event.key === 'ArrowLeft' ? this.current()-1 : event.key === 'ArrowRight' ? this.current()+1
      : event.key === 'Home' ? 0 : event.key === 'End' ? this.slides.length-1 : null;
    if (index === null) return;
    event.preventDefault(); this.select(index);
  }

  onImageLoad(index: number): void {
    if (index === this.current()) this.prepare((index + 1) % this.slides.length);
  }

  onImageError(index: number): void {
    this.failed.update(values => new Set([...values,index]));
  }

  private advance(index: number): void {
    const next = (index + this.slides.length) % this.slides.length;
    this.prepare(next); this.current.set(next);
  }

  private prepare(index: number): void {
    if (!this.prepared().has(index)) this.prepared.update(values => new Set([...values,index]));
  }
}
