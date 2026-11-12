import { LanguageService } from '../../../core/i18n/language.service';
import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TrajetsApiService } from '../../../core/api/trajets-api.service';
import { VoyageurSessionService } from '../../../core/auth/voyageur-session.service';
import { TrajetSearchComponent } from '../../../shared/components/trajet-search.component';
import { TrajetCardComponent } from '../../../shared/components/trajet-card.component';
import { RechercheTrajet, TrajetResponse } from '../../../shared/models/trajet.model';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [
    RouterLink,
    MatButtonModule,
    MatProgressSpinnerModule,
    TrajetSearchComponent,
    TrajetCardComponent,
  ],
  template: `
    <section class="welcome">
      <div class="welcome-inner">
        <div class="hero-copy">
          <p class="eyebrow">{{ language.t('transport.eyebrow') }}</p>
          <h1>
            {{ language.t('transport.together') }}<br /><em>{{
              language.t('transport.simply')
            }}</em>
          </h1>
          <p class="lead">{{ language.t('transport.lead') }}</p>
          <div class="trust">
            <span>✓ {{ language.t('transport.driverKnown') }}</span
            ><span>✓ {{ language.t('transport.maximum') }}</span
            ><span>✓ {{ language.t('transport.price') }}</span>
          </div>
        </div>
        <aside class="hero-note" [attr.aria-label]="language.t('ui.illustration')">
          <img
            src="images/covoiturage-belgique.svg"
            width="480"
            height="300"
            alt=""
            fetchpriority="high"
          />
          <p>
            {{ language.t('transport.lessCars') }}<br />{{ language.t('transport.moreMeetings') }}
          </p>
          <span>{{ language.t('transport.startHere') }}</span>
        </aside>
      </div>
    </section>
    <div class="content">
      <div class="search-panel"><app-trajet-search (recherche)="rechercher($event)" /></div>
      <section id="trajets" class="available" aria-labelledby="available-title">
        <div class="section-title">
          <div>
            <p class="eyebrow">{{ language.t('transport.onRoad') }}</p>
            <h2 id="available-title">{{ language.t('transport.find') }}</h2>
          </div>
          <span>{{ language.t('transport.paymentDemo') }}</span>
        </div>
        @if (loading()) {
          <div class="loading" role="status">
            <mat-spinner
              [attr.aria-label]="language.t('transport.searching')"
              diameter="28"
            /><span>{{ language.t('transport.searching') }}</span>
          </div>
        }
        @if (erreur()) {
          <p class="status-error" role="alert">{{ erreur() }}</p>
          <button mat-stroked-button (click)="rechercher(derniereRecherche)">
            {{ language.t('transport.retry') }}
          </button>
        }
        <div class="trip-grid">
          @for (trajet of trajets(); track trajet.id) {
            <app-trajet-card [trajet]="trajet" (reservationDemandee)="reserver()" />
          }
        </div>
        @if (!loading() && !erreur() && !trajets().length) {
          <div class="empty">
            <h3>{{ language.t('transport.noRides') }}</h3>
            <p>{{ language.t('transport.tryOther') }}</p>
          </div>
        }
      </section>
      <section class="popular">
        <p class="eyebrow">{{ language.t('transport.belgium') }}</p>
        <h2>{{ language.t('transport.popular') }}</h2>
        <p>{{ language.t('transport.suggestions') }}</p>
        <div class="popular-grid">
          <button (click)="rechercher({ depart: 'MONS', arrivee: 'BRUXELLES' })">
            Mons <span>→</span> Bruxelles
          </button>
          <button (click)="rechercher({ depart: 'CHARLEROI', arrivee: 'BRUXELLES' })">
            Charleroi <span>→</span> Bruxelles
          </button>
          <button (click)="rechercher({ depart: 'NAMUR', arrivee: 'BRUXELLES' })">
            Namur <span>→</span> Bruxelles
          </button>
          <button (click)="rechercher({ depart: 'BRUXELLES', arrivee: 'LIEGE' })">
            Bruxelles <span>→</span> Liège
          </button>
          <button (click)="rechercher({ depart: 'LIEGE', arrivee: 'BRUXELLES' })">
            Liège <span>→</span> Bruxelles
          </button>
          <button (click)="rechercher({ depart: 'BRUXELLES', arrivee: 'ANVERS' })">
            Bruxelles <span>→</span> {{ language.t('ui.antwerp') }}
          </button>
        </div>
      </section>
      <section class="how">
        <p class="eyebrow">{{ language.t('transport.withYou') }}</p>
        <h2>{{ language.t('transport.how') }}</h2>
        <div class="steps">
          <article>
            <b>01</b>
            <h3>{{ language.t('transport.findStep') }}</h3>
            <p>{{ language.t('transport.findBody') }}</p>
          </article>
          <article>
            <b>02</b>
            <h3>{{ language.t('transport.payStep') }}</h3>
            <p>{{ language.t('transport.payBody') }}</p>
          </article>
          <article>
            <b>03</b>
            <h3>{{ language.t('transport.travelStep') }}</h3>
            <p>{{ language.t('transport.travelBody') }}</p>
          </article>
        </div>
      </section>
      <section class="driver-call">
        <div>
          <h2>{{ language.t('transport.shareWheel') }}</h2>
          <p>{{ language.t('transport.driverBody') }}</p>
        </div>
        <a mat-flat-button routerLink="/voyageur">{{ language.t('transport.myArea') }}</a>
      </section>
    </div>
  `,
  styles: [
    `
      :host {
        display: block;
      }
      .welcome {
        background: var(--ui-surface);
        border-bottom: 1px solid var(--ui-border);
        padding: 4.5rem 1.5rem 6rem;
      }
      .welcome-inner,
      .content {
        max-width: 1200px;
        margin-inline: auto;
      }
      .welcome-inner {
        display: grid;
        grid-template-columns: 1.2fr 1fr;
        align-items: center;
        gap: 3rem;
      }
      .eyebrow {
        font-size: 0.72rem;
        font-weight: 750;
        letter-spacing: 0.13em;
        color: var(--ui-teal-strong);
        text-transform: uppercase;
        margin: 0 0 1rem;
      }
      h1 {
        font-size: clamp(2.5rem, 4.8vw, 4.7rem);
        line-height: 1.08;
        letter-spacing: -0.045em;
        margin: 0;
        color: var(--ui-navy);
      }
      h1 em {
        font-style: normal;
        color: var(--ui-primary);
      }
      .lead {
        color: var(--ui-text-secondary);
        font-size: 1.05rem;
        line-height: 1.85;
        max-width: 34rem;
        margin: 1.5rem 0;
      }
      .trust {
        display: flex;
        flex-wrap: wrap;
        gap: 0.75rem;
        font-size: 0.77rem;
        font-weight: 600;
        color: var(--ui-teal-strong);
      }
      .trust span {
        padding: 0.4rem 0.65rem;
        border: 1px solid var(--ui-border);
        border-radius: 7px;
      }
      .hero-note {
        margin: 0;
        padding: 1rem;
        background: var(--ui-primary-soft);
        border: 1px solid var(--ui-border);
        border-radius: 28px;
      }
      .hero-note img {
        display: block;
        width: 100%;
        height: auto;
      }
      .hero-note p {
        color: var(--ui-navy);
        font-size: 1.1rem;
        font-weight: 700;
        margin: 0.5rem 1rem;
      }
      .hero-note > span {
        display: block;
        color: var(--ui-text-secondary);
        font-size: 0.8rem;
        margin: 0 1rem 1rem;
      }
      .content {
        padding: 0 1.5rem;
      }
      .search-panel {
        position: relative;
        padding: 1.5rem;
        margin-top: -2.5rem;
        border: 1px solid var(--ui-border);
        background: var(--ui-surface);
        border-radius: 18px;
        box-shadow: var(--ui-shadow-raised);
      }
      .available,
      .popular,
      .how {
        padding-top: 4rem;
      }
      .section-title {
        display: flex;
        align-items: end;
        justify-content: space-between;
        flex-wrap: wrap;
        gap: 1rem;
        margin-bottom: 1.5rem;
      }
      .section-title h2 {
        margin: 0;
      }
      .section-title > span {
        font-size: 0.8rem;
        color: var(--ui-text-secondary);
      }
      .section-title .eyebrow {
        margin-bottom: 0.5rem;
      }
      .trip-grid {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 1.25rem;
      }
      .empty {
        padding: 3rem 1.5rem;
        border: 1px dashed var(--ui-border);
        border-radius: var(--ui-radius);
        background: var(--ui-surface);
        text-align: center;
      }
      .empty p,
      .steps p,
      .popular > p {
        color: var(--ui-text-secondary);
        line-height: 1.8;
      }
      .loading {
        display: flex;
        align-items: center;
        gap: 1rem;
        padding: 2rem;
      }
      .popular-grid {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 1rem;
      }
      .popular-grid button {
        display: flex;
        justify-content: space-between;
        gap: 0.5rem;
        align-items: center;
        padding: 1.15rem;
        min-height: 64px;
        border: 1px solid var(--ui-border);
        border-radius: 12px;
        background: var(--ui-surface);
        color: var(--ui-navy);
        font-weight: 650;
        cursor: pointer;
        text-align: left;
      }
      .popular-grid button:hover {
        border-color: var(--ui-primary);
        color: var(--ui-primary-strong);
      }
      .popular-grid span {
        color: var(--ui-teal-strong);
      }
      .steps {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 1.25rem;
      }
      .steps article {
        padding: 1.75rem;
        background: var(--ui-surface);
        border: 1px solid var(--ui-border);
        border-radius: var(--ui-radius);
      }
      .steps b {
        display: grid;
        place-items: center;
        height: 40px;
        width: 40px;
        border-radius: 10px;
        color: var(--ui-primary-strong);
        background: var(--ui-primary-soft);
        font-size: 0.85rem;
      }
      .steps h3 {
        font-size: 1.1rem;
        margin: 1.5rem 0 0.75rem;
      }
      .steps p {
        font-size: 0.9rem;
        margin: 0;
      }
      .driver-call {
        display: flex;
        justify-content: space-between;
        align-items: center;
        flex-wrap: wrap;
        gap: 1.5rem;
        margin: 4rem 0 1rem;
        padding: 2.5rem;
        border-radius: 20px;
        color: var(--ui-surface);
        background: var(--ui-navy);
      }
      .driver-call h2 {
        margin-top: 0;
      }
      .driver-call p {
        color: var(--ui-surface);
        margin-bottom: 0;
      }
      @media (max-width: 1000px) {
        .trip-grid,
        .popular-grid {
          grid-template-columns: repeat(2, minmax(0, 1fr));
        }
        .welcome-inner {
          gap: 2rem;
        }
        .steps {
          gap: 1rem;
        }
        .steps article {
          padding: 1.25rem;
        }
      }
      @media (max-width: 700px) {
        .welcome {
          padding: 2.5rem 1rem 5rem;
        }
        .welcome-inner {
          grid-template-columns: 1fr;
        }
        .hero-note {
          display: none;
        }
        .content {
          padding: 0 1rem;
        }
        .search-panel {
          padding: 1rem;
        }
        .trip-grid,
        .popular-grid,
        .steps {
          grid-template-columns: 1fr;
        }
        .available,
        .popular,
        .how {
          padding-top: 2.5rem;
        }
        .driver-call {
          padding: 1.5rem;
          margin-top: 2.5rem;
        }
      }
    `,
  ],
})
export class LandingPage {
  readonly language = inject(LanguageService);
  private readonly api = inject(TrajetsApiService);
  private readonly router = inject(Router);
  private readonly session = inject(VoyageurSessionService);
  readonly trajets = signal<TrajetResponse[]>([]);
  readonly loading = signal(false);
  readonly erreur = signal('');
  derniereRecherche: RechercheTrajet = {};
  constructor() {
    this.rechercher({});
  }
  rechercher(filtres: RechercheTrajet): void {
    this.derniereRecherche = filtres;
    this.loading.set(true);
    this.erreur.set('');
    this.trajets.set([]);
    this.api.getPublic(filtres).subscribe({
      next: (trajets) => {
        this.trajets.set(trajets);
        this.loading.set(false);
      },
      error: () => {
        this.erreur.set(this.language.t('ui.loadError'));
        this.loading.set(false);
      },
    });
  }
  reserver(): void {
    this.router.navigateByUrl(this.session.isAuthenticated() ? '/voyageur/trajets' : '/voyageur');
  }
}
