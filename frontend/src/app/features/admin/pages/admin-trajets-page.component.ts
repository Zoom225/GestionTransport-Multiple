import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { TrajetsApiService } from '../../../core/api/trajets-api.service';
import { ReservationsApiService } from '../../../core/api/reservations-api.service';
import { AgencesApiService } from '../../../core/api/agences-api.service';
import { AdminSessionService } from '../../../core/auth/admin-session.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { TrajetResponse } from '../../../shared/models/trajet.model';
import { ReservationResponse } from '../../../shared/models/reservation.model';
import { AgenceResponse } from '../../../shared/models/agence-vehicule.model';
import { extractApiErrorMessage } from '../../../shared/utils/api-error.util';

@Component({
  selector: 'app-admin-trajets-page',
  standalone: true,
  imports: [CommonModule, RouterLink, MatCardModule, MatButtonModule, MatProgressSpinnerModule],
  template: `
    <!-- En-tête trajets -->
    <div class="adm-trajet-header">
      <div class="adm-trajet-header-inner">
        <div class="adm-trajet-title-block">
<span class="adm-trajet-icon">🚗</span>
          <div>
            <h1 class="adm-trajet-title">{{ language.t('admin.trajets.title') }}</h1>
            <p class="adm-trajet-sub">{{ language.t('admin.trajets.subtitle') }}</p>
          </div>
        </div>
        <a routerLink="/admin" class="adm-trajet-back-btn">{{ language.t('common.back') }}</a>
      </div>
    </div>

    <section class="page-shell">
      <a mat-flat-button routerLink="/admin/trajets/new">Créer un trajet avec chauffeur</a>
      @if (loading()) {
        <mat-spinner diameter="32"></mat-spinner>
      }
      @if (message()) {
        <p class="status-success">✅ {{ message() }}</p>
      }
      @if (errorMessage()) {
        <p class="status-error">❌ {{ errorMessage() }}</p>
      }

      <div class="grid gap-5 lg:grid-cols-2">
        @for (trajet of filteredTrajets(); track trajet.id) {
          <mat-card class="adm-trajet-card">
            <mat-card-header class="adm-trajet-card-header">
              <div class="adm-trajet-card-title-row">
                <div>
                  <mat-card-title class="adm-trajet-card-title">{{ trajet.vehiculeNom }}</mat-card-title>
                  <mat-card-subtitle class="adm-trajet-card-sub">🏢 {{ trajet.agenceNom }}</mat-card-subtitle>
                </div>
                <span class="ds-badge" [class]="typeBadgeClass(trajet.typeTrajet)">{{ typeLabel(trajet.typeTrajet) }}</span>
              </div>
              <div class="adm-trajet-date-row">
                <span>📅 {{ trajet.date }}</span>
                <span>🕐 {{ trajet.heureDebut }} – {{ trajet.heureFin }}</span>
              </div>
            </mat-card-header>

            <mat-card-content class="adm-trajet-content">
              <div class="adm-trajet-info-grid">
                <div class="adm-info-item">
<span class="adm-info-label">{{ language.t('admin.trajets.organizer') }}</span>
                  <span class="adm-info-value">{{ trajet.organisateurNom }}</span>
                </div>
                <div class="adm-info-item">
<span class="adm-info-label">{{ language.t('admin.trajets.status') }}</span>
                  <span class="ds-badge" [class]="statusBadgeClass(trajet.statut)">{{ trajetStatusLabel(trajet.statut) }}</span>
                </div>
                <div class="adm-info-item adm-passagers-item">
<span class="adm-info-label">{{ language.t('admin.trajets.passagers') }}</span>
                  <div class="adm-passagers-bar">
                    <div class="adm-passagers-fill" [style.width.%]="(trajet.nbPassagersActuels / 4) * 100">
                      <span>{{ trajet.nbPassagersActuels }}/4</span>
                    </div>
                  </div>
                </div>
              </div>

              @if (selectedTrajetId() === trajet.id) {
                <div class="adm-reservations-panel">
<p class="adm-reservations-title">{{ language.t('admin.trajets.reservations') }}</p>
                  @for (reservation of selectedReservations(); track reservation.id) {
                    <div class="adm-reservation-item">
                      <span class="font-medium">{{ reservation.voyageurNom }}</span>
                      <div class="flex gap-2">
                        <span class="ds-badge" [class]="reservationBadgeClass(reservation.statut)">{{ reservationStatusLabel(reservation.statut) }}</span>
                        <span class="ds-badge" [class]="paymentBadgeClass(reservation.paiement?.statut)">{{ paymentStatusLabel(reservation.paiement?.statut) }}</span>
                      </div>
                    </div>
                  } @empty {
<p class="text-sm text-slate-500 text-center py-2">{{ language.t('admin.trajets.noReservations') }}</p>
                  }
                </div>
              }
            </mat-card-content>

            <mat-card-actions class="adm-trajet-actions">
              <button class="adm-trajet-btn-details" type="button" (click)="showReservations(trajet)">
{{ selectedTrajetId() === trajet.id ? language.t('admin.trajets.hideDetails') : language.t('admin.trajets.showDetails') }}
              </button>
              <button
                class="adm-trajet-btn-convert"
                type="button"
                (click)="convertToPublic(trajet.id)"
                [disabled]="trajet.typeTrajet !== 'PRIVE'"
              >
{{ language.t('admin.trajets.convert') }}
              </button>
            </mat-card-actions>
          </mat-card>
        } @empty {
          <div class="adm-trajet-empty lg:col-span-2">
            <span>🚗</span>
<p>{{ language.t('admin.trajets.empty') }}</p>
          </div>
        }
      </div>
    </section>

    <style>
      .adm-trajet-header {
        background: linear-gradient(135deg, #7c2d12 0%, #c2410c 50%, #ea580c 100%);
        padding: 1.5rem 2rem;
        box-shadow: 0 4px 16px rgba(194,65,12,0.3);
      }
      .adm-trajet-header-inner {
        display: flex; align-items: center; justify-content: space-between;
        max-width: 1200px; margin: 0 auto; gap: 1rem;
      }
      .adm-trajet-title-block { display: flex; align-items: center; gap: 1rem; }
      .adm-trajet-icon { font-size: 2.5rem; }
      .adm-trajet-title { font-size: 1.6rem; font-weight: 800; color: #fff; margin: 0; }
      .adm-trajet-sub { color: rgba(255,255,255,0.75); font-size: 0.85rem; margin: 0; }
      .adm-trajet-back-btn {
        background: rgba(255,255,255,0.15); color: #fff; border: 2px solid rgba(255,255,255,0.35);
        border-radius: 9999px; padding: 0.45rem 1.1rem; font-weight: 700;
        font-size: 0.85rem; text-decoration: none; transition: background 0.15s;
        white-space: nowrap;
      }
      .adm-trajet-back-btn:hover { background: rgba(255,255,255,0.25); }

      .adm-trajet-card {
        border-radius: 1.25rem !important;
        border: 1px solid rgba(234,88,12,0.15) !important;
        overflow: hidden;
        border-top: 4px solid #ea580c !important;
        transition: transform 0.15s, box-shadow 0.15s;
      }
      .adm-trajet-card:hover { transform: translateY(-3px); }

      .adm-trajet-card-header { padding: 1rem 1.25rem 0 !important; }
      .adm-trajet-card-title-row { display: flex; align-items: flex-start; justify-content: space-between; gap: 0.75rem; margin-bottom: 0.5rem; }
      .adm-trajet-card-title { font-size: 1.05rem !important; font-weight: 700 !important; color: #1e293b !important; }
      .adm-trajet-card-sub { color: #64748b !important; }
      .adm-trajet-date-row { display: flex; gap: 1rem; font-size: 0.82rem; color: #94a3b8; padding-bottom: 0.5rem; flex-wrap: wrap; }

      .adm-trajet-content { padding: 0.75rem 1.25rem !important; }
      .adm-trajet-info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
      .adm-info-item { display: flex; flex-direction: column; gap: 0.2rem; }
      .adm-passagers-item { grid-column: 1 / -1; }
      .adm-info-label { font-size: 0.72rem; font-weight: 600; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.04em; }
      .adm-info-value { font-size: 0.9rem; font-weight: 600; color: #1e293b; }

      .adm-passagers-bar {
        background: #f1f5f9; border-radius: 9999px; height: 1.25rem;
        overflow: hidden; border: 1px solid #e2e8f0; margin-top: 0.2rem;
      }
      .adm-passagers-fill {
        height: 100%; background: linear-gradient(90deg, #f97316, #ea580c);
        border-radius: 9999px; display: flex; align-items: center;
        justify-content: center; min-width: 2rem;
        font-size: 0.7rem; font-weight: 700; color: #fff;
        transition: width 0.4s ease;
      }

      .adm-reservations-panel {
        margin-top: 0.75rem; border-radius: 0.75rem;
        border: 1px solid #fed7aa; background: #fff7ed; padding: 0.75rem;
      }
      .adm-reservations-title { font-weight: 700; color: #9a3412; font-size: 0.9rem; margin: 0 0 0.5rem; }
      .adm-reservation-item {
        display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 0.5rem;
        padding: 0.5rem 0.75rem; border-radius: 0.5rem; background: #fff;
        margin-bottom: 0.4rem; font-size: 0.85rem; border: 1px solid #fed7aa;
      }

      .adm-trajet-actions {
        display: flex; flex-wrap: wrap; gap: 0.75rem;
        padding: 0.75rem 1.25rem 1rem !important;
        border-top: 1px solid #fef0e6;
      }
      .adm-trajet-btn-details {
        flex: 1; min-width: 130px; padding: 0.5rem 1rem; border-radius: 0.6rem;
        background: #fff7ed; color: #9a3412; border: 1.5px solid #fed7aa;
        font-weight: 700; font-size: 0.85rem; cursor: pointer; transition: background 0.15s;
      }
      .adm-trajet-btn-details:hover { background: #ffedd5; }
      .adm-trajet-btn-convert {
        flex: 1; min-width: 160px; padding: 0.5rem 1rem; border-radius: 0.6rem;
        background: linear-gradient(135deg, #0369a1, #2563eb); color: #fff; border: none;
        font-weight: 700; font-size: 0.85rem; cursor: pointer;
        box-shadow: 0 3px 10px rgba(37,99,235,0.3); transition: transform 0.15s;
      }
      .adm-trajet-btn-convert:hover:not(:disabled) { transform: translateY(-1px); }
      .adm-trajet-btn-convert:disabled { opacity: 0.4; cursor: not-allowed; }

      .adm-trajet-empty {
        text-align: center; padding: 3rem; color: #94a3b8;
        font-size: 1.1rem; display: flex; flex-direction: column; align-items: center; gap: 0.5rem;
      }
      .adm-trajet-empty span { font-size: 3rem; }
    </style>
  `
})
export class AdminTrajetsPage {
  private readonly trajetsApi = inject(TrajetsApiService);
  private readonly reservationsApi = inject(ReservationsApiService);
  private readonly agencesApi = inject(AgencesApiService);
  private readonly adminSession = inject(AdminSessionService);
  readonly language = inject(LanguageService);

  readonly loading = signal(false);
  readonly message = signal('');
  readonly errorMessage = signal('');
  readonly trajets = signal<TrajetResponse[]>([]);
  readonly agences = signal<AgenceResponse[]>([]);
  readonly selectedTrajetId = signal<number | null>(null);
  readonly selectedReservations = signal<ReservationResponse[]>([]);

  readonly filteredTrajets = computed(() => {
    if (this.adminSession.isGlobalAdmin()) {
      return this.trajets();
    }
    const agenceIds = new Set(this.agences().map((agence) => agence.id));
    return this.trajets().filter((trajet) => trajet.agenceId != null && agenceIds.has(trajet.agenceId));
  });

  constructor() {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    forkJoin({
      trajets: this.trajetsApi.getAll(),
      agences: this.agencesApi.getAll()
    }).subscribe({
      next: ({ trajets, agences }) => {
        this.trajets.set(trajets);
        this.agences.set(this.adminSession.isGlobalAdmin() ? agences : agences.filter((agence) => agence.id === this.adminSession.agenceId()));
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.errorMessage.set(extractApiErrorMessage(error, this.language.t('admin.trajets.loadError')));
      }
    });
  }

  showReservations(trajet: TrajetResponse): void {
    if (this.selectedTrajetId() === trajet.id) {
      this.selectedTrajetId.set(null);
      this.selectedReservations.set([]);
      return;
    }

    this.selectedTrajetId.set(trajet.id);
    this.reservationsApi.getByTrajet(trajet.id).subscribe({
      next: (reservations) => this.selectedReservations.set(reservations),
      error: (error) => {
        this.errorMessage.set(extractApiErrorMessage(error, this.language.t('admin.trajets.reservationsError')));
      }
    });
  }

  convertToPublic(trajetId: number): void {
    this.message.set('');
    this.errorMessage.set('');

    this.trajetsApi.convertToPublic(trajetId).subscribe({
      next: () => {
        this.message.set(this.language.t('admin.trajets.convertSuccess'));
        this.loadData();
      },
      error: (error) => {
        this.errorMessage.set(extractApiErrorMessage(error, this.language.t('admin.trajets.convertError')));
      }
    });
  }

  typeBadgeClass(type: TrajetResponse['typeTrajet']): string {
    return type === 'PRIVE' ? 'ds-badge-info' : 'ds-badge-neutral';
  }

  typeLabel(type: TrajetResponse['typeTrajet']): string {
    return type === 'PRIVE' ? this.language.t('voyageur.create.private') : this.language.t('voyageur.create.public');
  }

  statusBadgeClass(statut: TrajetResponse['statut']): string {
    if (statut === 'PLANIFIE') {
      return 'ds-badge-success';
    }
    if (statut === 'COMPLET') {
      return 'ds-badge-warning';
    }
    return 'ds-badge-danger';
  }

  trajetStatusLabel(statut: TrajetResponse['statut']): string {
    if (statut === 'PLANIFIE') {
      return this.language.t('common.trajetPlanned');
    }
    if (statut === 'COMPLET') {
      return this.language.t('common.trajetFull');
    }
    return this.language.t('common.trajetCanceled');
  }

  reservationBadgeClass(statut: ReservationResponse['statut']): string {
    if (statut === 'CONFIRMEE') {
      return 'ds-badge-success';
    }
    if (statut === 'EN_ATTENTE') {
      return 'ds-badge-warning';
    }
    return 'ds-badge-danger';
  }

  reservationStatusLabel(statut: ReservationResponse['statut']): string {
    if (statut === 'CONFIRMEE') {
      return this.language.t('common.reservationConfirmed');
    }
    if (statut === 'EN_ATTENTE') {
      return this.language.t('common.reservationPending');
    }
    return this.language.t('common.reservationCanceled');
  }

  paymentBadgeClass(statut: string | undefined): string {
    if (statut === 'PAYE') {
      return 'ds-badge-success';
    }
    if (statut === 'EN_ATTENTE') {
      return 'ds-badge-warning';
    }
    if (statut === 'REMBOURSE') {
      return 'ds-badge-info';
    }
    return 'ds-badge-neutral';
  }

  paymentStatusLabel(statut: string | undefined): string {
    if (statut === 'PAYE') {
      return this.language.t('common.paymentPaid');
    }
    if (statut === 'EN_ATTENTE') {
      return this.language.t('common.paymentPending');
    }
    if (statut === 'REMBOURSE') {
      return this.language.t('common.paymentRefunded');
    }
    if (statut === 'ANNULE') {
      return this.language.t('common.paymentCanceled');
    }
    return this.language.t('common.none');
  }
}
