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
  selector: 'app-landing-page', standalone: true,
  imports: [RouterLink, MatButtonModule, MatProgressSpinnerModule, TrajetSearchComponent, TrajetCardComponent],
  template: `
    <section class="welcome">
      <div class="welcome-inner">
        <div class="hero-copy"><p class="eyebrow">LE COVOITURAGE, À LA BELGE</p>
          <h1>Voyagez ensemble,<br/><em>simplement.</em></h1>
          <p class="lead">De Mons à Bruxelles, de Namur à Liège : partagez votre trajet et prenez la route en toute confiance.</p>
          <div class="trust"><span>✓ Un chauffeur identifié</span><span>✓ 4 clients maximum</span><span>✓ 25 € par personne</span></div>
        </div>
        <aside class="hero-note" aria-label="Notre engagement"><span class="route-icon" aria-hidden="true">↗</span><p>Moins de voitures.<br/>Plus de rencontres.</p><span>Votre prochain départ commence ici.</span></aside>
      </div>
    </section>
    <div class="content">
      <div class="search-panel"><app-trajet-search (recherche)="rechercher($event)" /></div>
      <section class="available" aria-labelledby="available-title">
        <div class="section-title"><div><p class="eyebrow">PRENEZ LA ROUTE</p><h2 id="available-title">Trouvez votre prochain trajet</h2></div><span>25 € / personne · paiement simulé</span></div>
        @if (loading()) { <div class="loading" role="status"><mat-spinner diameter="28" /><span>Recherche des trajets…</span></div> }
        @if (erreur()) { <p class="status-error" role="alert">{{ erreur() }}</p><button mat-stroked-button (click)="rechercher(derniereRecherche)">Réessayer</button> }
        <div class="trip-grid">
          @for (trajet of trajets(); track trajet.id) { <app-trajet-card [trajet]="trajet" (reservationDemandee)="reserver()" /> }
        </div>
        @if (!loading() && !erreur() && !trajets().length) { <div class="empty"><h3>Aucun départ pour le moment</h3><p>Essayez une autre ville ou une autre date. Les trajets publiés apparaîtront ici.</p></div> }
      </section>
      <section class="popular"><p class="eyebrow">UNE BELGIQUE PLUS PROCHE</p><h2>Trajets populaires</h2><p>Des idées de recherche, selon les départs réellement publiés.</p>
        <div class="popular-grid">
          <button (click)="rechercher({depart:'MONS',arrivee:'BRUXELLES'})">Mons <span>→</span> Bruxelles</button>
          <button (click)="rechercher({depart:'CHARLEROI',arrivee:'BRUXELLES'})">Charleroi <span>→</span> Bruxelles</button>
          <button (click)="rechercher({depart:'NAMUR',arrivee:'BRUXELLES'})">Namur <span>→</span> Bruxelles</button>
          <button (click)="rechercher({depart:'BRUXELLES',arrivee:'LIEGE'})">Bruxelles <span>→</span> Liège</button>
        </div>
      </section>
      <section class="how"><p class="eyebrow">ON VOUS ACCOMPAGNE</p><h2>Comment ça marche ?</h2>
        <div class="steps"><article><b>01</b><h3>Trouvez votre trajet</h3><p>Choisissez vos villes et votre date, puis découvrez le chauffeur et les places disponibles.</p></article><article><b>02</b><h3>Réservez et payez</h3><p>Votre paiement simulé confirme une place disponible. Une demande impayée ne garantit pas la place.</p></article><article><b>03</b><h3>Voyagez l'esprit libre</h3><p>Retrouvez votre confirmation, vos prochains voyages et votre historique dans votre espace.</p></article></div>
      </section>
      <section class="driver-call"><div><h2>Envie de partager le volant ?</h2><p>Les chauffeurs autorisés proposent leurs trajets, publics ou sur invitation.</p></div><a mat-flat-button routerLink="/voyageur">Mon espace voyageur</a></section>
    </div>
    <footer><strong>GestionTransport-Multiple</strong><span>Le covoiturage entre villes belges.</span><a routerLink="/admin/login">Administration</a></footer>
  `,
  styles: [`
    :host{display:block;color:#173f36}.welcome{background:#e7f1eb;padding:4rem 1.5rem 6rem}.welcome-inner,.content,footer{max-width:1180px;margin:auto}.welcome-inner{display:grid;grid-template-columns:1.5fr 1fr;align-items:center;gap:3rem}.eyebrow{font-size:.72rem;font-weight:800;letter-spacing:.15em;color:#33765d;margin:0 0 1rem}h1{font-size:clamp(2.8rem,5vw,5rem);line-height:1.08;letter-spacing:-.04em;margin:0}h1 em{color:#327658;font-style:normal}.lead{max-width:590px;font-size:1.08rem;line-height:1.8;color:#47665b;margin:1.5rem 0}.trust{display:flex;flex-wrap:wrap;gap:1rem;font-size:.82rem;font-weight:600}.hero-note{background:#173f36;border-radius:42% 42% 20px 20px;min-height:330px;padding:3rem;text-align:center;color:#f4f4df}.hero-note p{font-size:1.8rem;line-height:1.4;font-weight:700}.hero-note>span:last-child{font-size:.8rem;color:#d7e4d9}.route-icon{font-size:4rem;color:#bfd894}.content{padding:0 1.5rem}.search-panel{position:relative;margin-top:-2.5rem;padding:1.5rem;background:white;border:1px solid #dce7e0;border-radius:20px;box-shadow:0 15px 50px #173f3610}.available,.popular,.how{padding-top:4rem}h2{font-size:clamp(1.6rem,3vw,2.1rem);margin:.5rem 0 1rem;letter-spacing:-.025em}.section-title{display:flex;justify-content:space-between;align-items:end;gap:1rem;flex-wrap:wrap}.section-title>span{font-size:.8rem;color:#526662;margin-bottom:1rem}.trip-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:1.2rem}.empty{padding:2.5rem;border:1px dashed #c8d8cf;border-radius:20px;text-align:center;background:#fafcf9}.loading{display:flex;align-items:center;gap:1rem;padding:2rem}.popular>p,.steps p{color:#526662;line-height:1.7}.popular-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:1rem}.popular-grid button{background:#f0f5ed;border:1px solid #dce7d8;border-radius:14px;padding:1.3rem 1rem;color:#173f36;font-weight:700;cursor:pointer;min-height:60px}.popular-grid span{padding:0 .3rem}.steps{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:2rem;padding-top:1rem}.steps b{display:grid;place-items:center;border-radius:50%;width:44px;height:44px;background:#e7f1eb;color:#327658}.steps h3{font-size:1.15rem;margin:1.2rem 0 .4rem}.driver-call{background:#173f36;color:white;border-radius:20px;margin:4rem 0;padding:2.3rem;display:flex;align-items:center;justify-content:space-between;gap:1rem;flex-wrap:wrap}.driver-call p{color:#cddcd4}footer{padding:1.5rem;display:flex;gap:1rem;flex-wrap:wrap;justify-content:space-between;font-size:.85rem;border-top:1px solid #dce7e0}footer a{color:#327658}@media(max-width:1000px){.trip-grid,.popular-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.welcome-inner{gap:1.5rem}.hero-note{padding:2rem;min-height:280px}}@media(max-width:650px){.welcome{padding:2.5rem 1.25rem 5rem}.welcome-inner{grid-template-columns:1fr}.hero-note{display:none}.content{padding:0 1rem}.search-panel{padding:1rem}.trip-grid,.popular-grid,.steps{grid-template-columns:1fr}.available,.popular,.how{padding-top:2.5rem}.driver-call{padding:1.5rem}.trust{font-size:.75rem;gap:.5rem}footer{flex-direction:column}}
  `]
})
export class LandingPage {
  private readonly api = inject(TrajetsApiService);
  private readonly router = inject(Router);
  private readonly session = inject(VoyageurSessionService);
  readonly trajets = signal<TrajetResponse[]>([]);
  readonly loading = signal(false);
  readonly erreur = signal('');
  derniereRecherche: RechercheTrajet = {};
  constructor() { this.rechercher({}); }
  rechercher(filtres: RechercheTrajet): void {
    this.derniereRecherche = filtres;
    this.loading.set(true); this.erreur.set(''); this.trajets.set([]);
    this.api.getPublic(filtres).subscribe({
      next: trajets => { this.trajets.set(trajets); this.loading.set(false); },
      error: () => { this.erreur.set('Les trajets ne sont pas disponibles pour le moment.'); this.loading.set(false); }
    });
  }
  reserver(): void { this.router.navigateByUrl(this.session.isAuthenticated() ? '/voyageur/trajets' : '/voyageur'); }
}
