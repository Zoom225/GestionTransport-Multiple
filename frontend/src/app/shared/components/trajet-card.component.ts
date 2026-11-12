import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { TrajetResponse } from '../models/trajet.model';

@Component({
  selector: 'app-trajet-card', standalone: true, imports: [CommonModule, MatButtonModule],
  template: `
    <article class="trip-card">
      <div class="trip-top"><span class="type">{{ trajet.typeTrajet }}</span><span>{{ trajet.date | date:'dd/MM/yyyy' }} · {{ trajet.heureDebut.slice(0,5) }}</span></div>
      <h3>{{ nomVille(trajet.villeDepart) }} <span aria-label="vers">→</span> {{ nomVille(trajet.villeArrivee) }}</h3>
      <p class="driver">Chauffeur · <strong>{{ trajet.chauffeurNom || trajet.organisateurNom }}</strong></p>
      <p>{{ trajet.vehiculeNom }} · {{ trajet.agenceNom }}</p>
      <div class="trip-bottom"><span>{{ restantes() }} place(s) restante(s) sur {{ trajet.placesMaximum ?? 4 }}</span><strong>{{ trajet.prixParPassager | currency:'EUR':'symbol':'1.0-2' }} <small>/ personne</small></strong></div>
      <div class="actions"><button mat-flat-button type="button" (click)="reservationDemandee.emit(trajet)" [disabled]="disabled || restantes() === 0 || trajet.statut !== 'PLANIFIE'">{{ restantes() === 0 ? 'Complet' : labelAction }}</button><ng-content /></div>
    </article>
  `,
  styles: [`:host{display:block;min-width:0}.trip-card{height:100%;padding:1.5rem;background:white;border:1px solid #dce7e6;border-radius:20px;box-shadow:0 8px 28px #0f4e3e08;transition:transform .2s}.trip-card:hover{transform:translateY(-3px)}.trip-top,.trip-bottom,.actions{display:flex;align-items:center;justify-content:space-between;gap:.75rem;flex-wrap:wrap}.trip-top{font-size:.8rem;color:#526662}.type{background:#e0f4eb;color:#145a42;padding:.25rem .6rem;border-radius:30px;font-weight:700}h3{font-size:1.45rem;color:#173f36;line-height:1.4;margin:1.3rem 0}p{font-size:.9rem;color:#526662;margin:.5rem 0}.trip-bottom{border-top:1px solid #e6eeeb;margin-top:1.25rem;padding-top:1rem;font-size:.85rem}.trip-bottom strong{font-size:1.5rem;color:#173f36}.trip-bottom small{font-weight:400;font-size:.75rem}.actions{justify-content:flex-start;margin-top:1.2rem}button{min-height:44px}@media(prefers-reduced-motion:reduce){.trip-card{transition:none;transform:none}}`]
})
export class TrajetCardComponent {
  @Input({required:true}) trajet!: TrajetResponse;
  @Input() disabled = false;
  @Input() labelAction = 'Réserver';
  @Output() readonly reservationDemandee = new EventEmitter<TrajetResponse>();
  restantes(): number { return this.trajet.placesRestantes ?? Math.max(0, 4 - this.trajet.nbPassagersActuels); }
  nomVille(code?: string | null): string { return code ? (code.charAt(0) + code.slice(1).toLowerCase()).replace('Liege', 'Liège') : 'Ville à préciser'; }
}
