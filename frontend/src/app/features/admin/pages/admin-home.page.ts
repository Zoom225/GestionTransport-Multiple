import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { TrajetsApiService } from '../../../core/api/trajets-api.service';
import { VoyageursApiService } from '../../../core/api/voyageurs-api.service';
import { ReservationsApiService } from '../../../core/api/reservations-api.service';
import { AgencesApiService } from '../../../core/api/agences-api.service';
import { VehiculesApiService } from '../../../core/api/vehicules-api.service';
import { AdminSessionService } from '../../../core/auth/admin-session.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { TrajetResponse } from '../../../shared/models/trajet.model';
import { VoyageurResponse } from '../../../shared/models/voyageur.model';
import { ReservationResponse } from '../../../shared/models/reservation.model';
import { AgenceResponse, VehiculeResponse } from '../../../shared/models/agence-vehicule.model';
import { extractApiErrorMessage } from '../../../shared/utils/api-error.util';

@Component({
  selector: 'app-admin-home-page',
  standalone: true,
  imports: [CommonModule, RouterLink, MatCardModule, MatButtonModule, MatProgressSpinnerModule],
  template: `
    <section class="admin-shell">
      <div class="admin-hero">
        <div>
          <p class="eyebrow">{{ language.t('admin.home.eyebrow') }}</p>
          <h1>{{ language.t('admin.home.title') }}</h1>
          <p class="hero-subtitle">
            {{
              language.t('admin.home.subtitle', {
                scope: adminSession.isGlobalAdmin() ? 'global' : 'agence',
              })
            }}
          </p>
        </div>

        <div class="admin-actions">
          <a routerLink="/admin/voyageurs">{{ language.t('nav.voyageurs') }}</a>
          <a routerLink="/admin/trajets">{{ language.t('nav.trajets') }}</a>
          <a routerLink="/admin/agences">{{ language.t('nav.agences') }}</a>
          <a routerLink="/admin/vehicules">{{ language.t('nav.vehicules') }}</a>
          <a routerLink="/admin/indisponibilites">{{ language.t('nav.indisponibilites') }}</a>
        </div>
      </div>

      @if (loading()) {
        <div class="loading-panel">
          <mat-spinner
            [attr.aria-label]="language.t('transport.searching')"
            diameter="32"
          ></mat-spinner>
          <span>{{ language.t('admin.home.loading') }}</span>
        </div>
      }
      @if (errorMessage()) {
        <p role="alert" class="status-error">{{ errorMessage() }}</p>
      }

      <div class="kpi-grid">
        <div class="admin-kpi green">
          <p>{{ language.t('admin.home.trajets') }}</p>
          <strong>{{ trajets().length }}</strong>
          <span>{{ language.t('admin.home.allStatuses') }}</span>
        </div>
        <div class="admin-kpi blue">
          <p>{{ language.t('admin.home.reservations') }}</p>
          <strong>{{ reservations().length }}</strong>
          <span>{{ pendingReservationsCount() }} {{ language.t('admin.home.waiting') }}</span>
        </div>
        <div class="admin-kpi slate">
          <p>{{ language.t('admin.home.voyageurs') }}</p>
          <strong>{{ voyageurs().length }}</strong>
          <span>{{ language.t('admin.home.visible') }}</span>
        </div>
        <div class="admin-kpi amber">
          <p>{{ language.t('admin.home.revenue') }}</p>
          <strong>{{ revenue() }} EUR</strong>
          <span>{{ language.t('admin.home.validPayments') }}</span>
        </div>
      </div>

      <div class="dashboard-grid">
        <mat-card class="card-soft admin-panel">
          <mat-card-header>
            <mat-card-title>{{ language.t('admin.home.occupancy') }}</mat-card-title>
            <mat-card-subtitle>{{ language.t('admin.home.occupancySub') }}</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <div class="data-list">
              @for (item of occupancyByAgence(); track item.agence) {
                <div class="data-row">
                  <span>{{ item.agence }}</span>
                  <strong>{{ item.count }} {{ language.t('admin.home.trajetCount') }}</strong>
                </div>
              } @empty {
                <p class="empty-state">{{ language.t('admin.home.empty') }}</p>
              }
            </div>
          </mat-card-content>
        </mat-card>

        <mat-card class="card-soft admin-panel">
          <mat-card-header>
            <mat-card-title>{{ language.t('admin.home.resources') }}</mat-card-title>
            <mat-card-subtitle>{{ language.t('admin.home.resourcesSub') }}</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <div class="resource-grid">
              <div>
                <span>{{ language.t('admin.home.agencesVisible') }}</span>
                <strong>{{ agences().length }}</strong>
              </div>
              <div>
                <span>{{ language.t('admin.home.vehiculesVisible') }}</span>
                <strong>{{ vehicules().length }}</strong>
              </div>
              <div>
                <span>{{ language.t('admin.home.completeTrajets') }}</span>
                <strong>{{ completeTrajetsCount() }}</strong>
              </div>
              <div>
                <span>{{ language.t('admin.home.pendingReservations') }}</span>
                <strong>{{ pendingReservationsCount() }}</strong>
              </div>
            </div>
          </mat-card-content>
        </mat-card>
      </div>
    </section>
  `,
  styles: [
    `
      .admin-shell {
        width: min(1220px, calc(100% - 2rem));
        margin: 0 auto;
        padding: 2rem 0 4rem;
      }

      .admin-hero {
        display: flex;
        align-items: flex-end;
        justify-content: space-between;
        gap: 1.5rem;
        padding: clamp(1.5rem, 3vw, 2.5rem);
        border-radius: 8px;
        background: var(--ui-navy);
        background-size:
          auto,
          44px 44px;
        color: var(--ui-surface);
        box-shadow: var(--ui-shadow);
      }

      .eyebrow {
        margin: 0 0 0.75rem;
        color: var(--ui-surface);
        font-size: 0.78rem;
        font-weight: 900;
        letter-spacing: 0.12em;
        text-transform: uppercase;
      }

      h1 {
        margin: 0;
        font-size: clamp(1.8rem, 3vw, 2.5rem);
        font-weight: 900;
        letter-spacing: 0;
        line-height: 1;
      }

      .hero-subtitle {
        max-width: 620px;
        margin: 1rem 0 0;
        color: color-mix(in srgb, var(--ui-surface) 78%, transparent);
        line-height: 1.65;
      }

      .admin-actions {
        display: flex;
        flex-wrap: wrap;
        justify-content: flex-end;
        gap: 0.65rem;
      }

      .admin-actions a {
        padding: 0.55rem 0.85rem;
        border: 1px solid color-mix(in srgb, var(--ui-surface) 18%, transparent);
        border-radius: 8px;
        background: color-mix(in srgb, var(--ui-surface) 12%, transparent);
        color: var(--ui-surface);
        font-size: 0.86rem;
        font-weight: 800;
        text-decoration: none;
        backdrop-filter: blur(12px);
        transition:
          background 0.15s,
          transform 0.15s;
      }

      .admin-actions a:hover {
        background: color-mix(in srgb, var(--ui-surface) 20%, transparent);
        transform: translateY(-1px);
      }

      .loading-panel {
        display: flex;
        align-items: center;
        gap: 0.8rem;
        margin-top: 1.25rem;
        color: var(--ui-text-secondary);
        font-weight: 800;
      }

      .kpi-grid {
        display: grid;
        grid-template-columns: repeat(4, minmax(0, 1fr));
        gap: 1rem;
        margin-top: 1.25rem;
      }

      .admin-kpi {
        min-height: 148px;
        padding: 1.2rem;
        border: 1px solid color-mix(in srgb, var(--ui-navy) 8%, transparent);
        border-radius: 8px;
        background: var(--ui-surface);
        box-shadow: var(--ui-shadow);
      }

      .admin-kpi p,
      .admin-kpi span {
        margin: 0;
        color: var(--ui-text-secondary);
        font-size: 0.86rem;
        font-weight: 750;
      }

      .admin-kpi strong {
        display: block;
        margin: 0.75rem 0 0.5rem;
        color: var(--ui-navy);
        font-size: clamp(1.8rem, 3vw, 2.4rem);
        line-height: 1;
        font-weight: 900;
      }

      .admin-kpi.green {
        border-top: 4px solid var(--ui-teal-strong);
      }

      .admin-kpi.blue {
        border-top: 4px solid var(--ui-primary-strong);
      }

      .admin-kpi.slate {
        border-top: 4px solid var(--ui-text-secondary);
      }

      .admin-kpi.amber {
        border-top: 4px solid var(--ui-warning);
      }

      .dashboard-grid {
        display: grid;
        grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr);
        gap: 1rem;
        margin-top: 1rem;
      }

      .admin-panel {
        border-radius: 8px !important;
      }

      .data-list {
        display: grid;
        gap: 0.7rem;
        padding-top: 0.75rem;
      }

      .data-row {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 1rem;
        padding: 0.9rem 1rem;
        border: 1px solid color-mix(in srgb, var(--ui-primary-strong) 12%, transparent);
        border-radius: 8px;
        background: var(--ui-surface-subtle);
        color: var(--ui-navy);
      }

      .data-row span {
        font-weight: 800;
      }

      .data-row strong {
        color: var(--ui-primary-strong);
        white-space: nowrap;
      }

      .resource-grid {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 0.8rem;
        padding-top: 0.75rem;
      }

      .resource-grid div {
        min-height: 110px;
        padding: 1rem;
        border-radius: 8px;
        background: var(--ui-surface);
        border: 1px solid color-mix(in srgb, var(--ui-navy) 8%, transparent);
      }

      .resource-grid span {
        display: block;
        color: var(--ui-text-secondary);
        font-size: 0.86rem;
        font-weight: 750;
      }

      .resource-grid strong {
        display: block;
        margin-top: 0.65rem;
        color: var(--ui-navy);
        font-size: 1.9rem;
        line-height: 1;
        font-weight: 900;
      }

      .empty-state {
        margin: 0;
        color: var(--ui-text-secondary);
      }

      @media (max-width: 940px) {
        .admin-hero,
        .dashboard-grid {
          grid-template-columns: 1fr;
        }

        .admin-hero {
          display: grid;
          align-items: start;
        }

        .admin-actions {
          justify-content: flex-start;
        }

        .kpi-grid {
          grid-template-columns: repeat(2, minmax(0, 1fr));
        }
      }

      @media (max-width: 640px) {
        .kpi-grid,
        .resource-grid {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class AdminHomePage {
  private readonly trajetsApi = inject(TrajetsApiService);
  private readonly voyageursApi = inject(VoyageursApiService);
  private readonly reservationsApi = inject(ReservationsApiService);
  private readonly agencesApi = inject(AgencesApiService);
  private readonly vehiculesApi = inject(VehiculesApiService);
  readonly adminSession = inject(AdminSessionService);
  readonly language = inject(LanguageService);

  readonly loading = signal(false);
  readonly errorMessage = signal('');
  readonly trajets = signal<TrajetResponse[]>([]);
  readonly voyageurs = signal<VoyageurResponse[]>([]);
  readonly reservations = signal<ReservationResponse[]>([]);
  readonly agences = signal<AgenceResponse[]>([]);
  readonly vehicules = signal<VehiculeResponse[]>([]);

  readonly revenue = computed(() =>
    this.reservations()
      .filter((reservation) => reservation.paiement?.statut === 'PAYE')
      .reduce((sum, reservation) => sum + (reservation.paiement?.montant ?? 0), 0),
  );
  readonly completeTrajetsCount = computed(
    () => this.trajets().filter((trajet) => trajet.statut === 'COMPLET').length,
  );
  readonly pendingReservationsCount = computed(
    () => this.reservations().filter((reservation) => reservation.statut === 'EN_ATTENTE').length,
  );
  readonly occupancyByAgence = computed(() => {
    const map = new Map<string, number>();
    this.trajets().forEach((trajet) =>
      map.set(trajet.agenceNom, (map.get(trajet.agenceNom) ?? 0) + 1),
    );
    return Array.from(map.entries()).map(([agence, count]) => ({ agence, count }));
  });

  constructor() {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    forkJoin({
      trajets: this.trajetsApi.getAll(),
      voyageurs: this.voyageursApi.getAll(),
      agences: this.agencesApi.getAll(),
      vehicules: this.vehiculesApi.getAll(),
    }).subscribe({
      next: ({ trajets, voyageurs, agences, vehicules }) => {
        const filteredAgences = this.filterAgences(agences);
        const filteredVehicules = this.filterVehicules(vehicules);
        const filteredVoyageurs = this.filterVoyageurs(voyageurs);
        const filteredTrajets = this.filterTrajets(trajets, filteredAgences);

        this.agences.set(filteredAgences);
        this.vehicules.set(filteredVehicules);
        this.voyageurs.set(filteredVoyageurs);
        this.trajets.set(filteredTrajets);

        if (!filteredTrajets.length) {
          this.reservations.set([]);
          this.loading.set(false);
          return;
        }

        forkJoin(
          filteredTrajets.map((trajet) => this.reservationsApi.getByTrajet(trajet.id)),
        ).subscribe({
          next: (allReservations) => {
            this.reservations.set(allReservations.flat());
            this.loading.set(false);
          },
          error: (error) => {
            this.loading.set(false);
            this.errorMessage.set(
              extractApiErrorMessage(
                error,
                this.language.t('admin.home.reservationsLoadError'),
                (key) => this.language.t(key),
              ),
            );
          },
        });
      },
      error: (error) => {
        this.loading.set(false);
        this.errorMessage.set(
          extractApiErrorMessage(error, this.language.t('admin.home.dashboardLoadError'), (key) =>
            this.language.t(key),
          ),
        );
      },
    });
  }

  private filterAgences(agences: AgenceResponse[]): AgenceResponse[] {
    const agenceId = this.adminSession.agenceId();
    if (this.adminSession.isGlobalAdmin() || !agenceId) {
      return agences;
    }
    return agences.filter((agence) => agence.id === agenceId);
  }

  private filterVehicules(vehicules: VehiculeResponse[]): VehiculeResponse[] {
    const agenceId = this.adminSession.agenceId();
    if (this.adminSession.isGlobalAdmin() || !agenceId) {
      return vehicules;
    }
    return vehicules.filter((vehicule) => vehicule.agenceId === agenceId);
  }

  private filterVoyageurs(voyageurs: VoyageurResponse[]): VoyageurResponse[] {
    const agenceId = this.adminSession.agenceId();
    if (this.adminSession.isGlobalAdmin() || !agenceId) {
      return voyageurs;
    }
    return voyageurs.filter(
      (voyageur) => voyageur.agenceId === agenceId || voyageur.agenceId === null,
    );
  }

  private filterTrajets(trajets: TrajetResponse[], agences: AgenceResponse[]): TrajetResponse[] {
    if (this.adminSession.isGlobalAdmin()) {
      return trajets;
    }

    const agenceIds = new Set(agences.map((agence) => agence.id));
    return trajets.filter((trajet) => trajet.agenceId != null && agenceIds.has(trajet.agenceId));
  }
}
