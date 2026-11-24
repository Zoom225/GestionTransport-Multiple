import { CommonModule } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { VoyageursApiService } from '../../../core/api/voyageurs-api.service';
import { VoyageurSessionService } from '../../../core/auth/voyageur-session.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { SanctionResponse, VoyageurResponse } from '../../../shared/models/voyageur.model';
import { ReservationsApiService } from '../../../core/api/reservations-api.service';
import { TrajetsApiService } from '../../../core/api/trajets-api.service';
import { ReservationResponse } from '../../../shared/models/reservation.model';
import { TrajetResponse } from '../../../shared/models/trajet.model';
import { extractApiErrorMessage } from '../../../shared/utils/api-error.util';

@Component({
  selector: 'app-voyageur-profile-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatChipsModule,
  ],
  templateUrl: './voyageur-profile-page.component.html',
  styleUrl: './voyageur-profile-page.component.scss',
})
export class VoyageurProfilePage {
  private readonly voyageursApi = inject(VoyageursApiService);
  private readonly voyageurSession = inject(VoyageurSessionService);
  private readonly router = inject(Router);
  private readonly reservationsApi = inject(ReservationsApiService);
  private readonly trajetsApi = inject(TrajetsApiService);
  private readonly destroyRef = inject(DestroyRef);
  readonly language = inject(LanguageService);

  readonly loading = signal(false);
  readonly errorMessage = signal('');
  readonly profile = signal<VoyageurResponse | null>(null);
  readonly participation = this.voyageurSession.participation;
  readonly sanctions = signal<SanctionResponse[]>([]);
  readonly reglementEnCours = signal<number | null>(null);
  readonly reglementMessage = signal('');
  readonly reservations = signal<ReservationResponse[]>([]);
  readonly trajetsProposes = signal<TrajetResponse[]>([]);
  readonly annulationId = signal<number | null>(null);
  readonly prochainsVoyages = computed(() =>
    this.reservations().filter(
      (r) => r.statut === 'CONFIRMEE' && new Date(r.trajetDateTime).getTime() > Date.now(),
    ),
  );
  readonly historique = computed(() =>
    this.reservations().filter(
      (r) =>
        r.statut === 'ANNULEE' ||
        r.statut === 'EXPIREE' ||
        new Date(r.trajetDateTime).getTime() <= Date.now(),
    ),
  );
  readonly paiementsValides = computed(
    () => this.reservations().filter((r) => r.paiement?.statut === 'PAYE').length,
  );
  readonly voyageurId = computed(() => this.voyageurSession.voyageurId());

  constructor() {
    this.reload();
  }

  reload(): void {
    const voyageurId = this.voyageurId();
    if (!voyageurId) {
      this.logout();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    forkJoin({
      profile: this.voyageursApi.getCurrent(),
      sanctions: this.voyageursApi.getSanctions(),
      reservations: this.reservationsApi.getByVoyageur(voyageurId),
      trajets: this.trajetsApi.getByOrganisateur(voyageurId),
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: ({ profile, sanctions, reservations, trajets }) => {
        this.reservations.set(reservations);
        this.trajetsProposes.set(trajets);
        this.profile.set(profile);
        this.sanctions.set(sanctions);
        this.voyageurSession.setVoyageur(profile, this.voyageurSession.token() ?? undefined);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set(this.language.t('voyageur.profile.loadError'));
      },
    });
  }

  payerPenalite(sanction: SanctionResponse): void {
    const trajetId = sanction.trajetId;
    if (trajetId === null || sanction.payee || !sanction.montant || this.reglementEnCours() !== null) return;

    this.reglementEnCours.set(trajetId);
    this.reglementMessage.set('');
    this.errorMessage.set('');
    this.trajetsApi.payerPenalite(trajetId).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.reglementEnCours.set(null)),
    ).subscribe({
      next: () => {
        this.sanctions.update((sanctions) => sanctions.map((item) =>
          item.id === sanction.id ? { ...item, payee: true } : item,
        ));
        this.reglementMessage.set('Règlement interne enregistré.');
        this.reload();
      },
      error: (erreur) => this.errorMessage.set(
        extractApiErrorMessage(erreur, 'Impossible de régler cette pénalité.', (key) => this.language.t(key)),
      ),
    });
  }

  estFutur(trajet: TrajetResponse): boolean {
    return new Date(trajet.date + 'T' + trajet.heureDebut).getTime() > Date.now();
  }

  annulerTrajet(trajet: TrajetResponse): void {
    if (!this.peutAnnuler(trajet)) return;
    const chauffeur = this.voyageurId();
    if (!chauffeur || !confirm('Annuler ce trajet et rembourser les clients confirmés ?')) return;
    this.annulationId.set(trajet.id);
    this.trajetsApi.cancel(trajet.id, chauffeur).subscribe({
      next: () => {
        this.annulationId.set(null);
        this.reload();
      },
      error: (erreur) => {
        this.annulationId.set(null);
        this.errorMessage.set(
          extractApiErrorMessage(erreur, 'Impossible d’annuler ce trajet.', (key) =>
            this.language.t(key),
          ),
        );
      },
    });
  }

  peutAnnuler(trajet: TrajetResponse): boolean {
    const id = this.voyageurId();
    return !!id && (trajet.organisateurId === id || trajet.chauffeurId === id)
      && trajet.statut !== 'ANNULE' && this.estFutur(trajet);
  }

  logout(): void {
    this.voyageurSession.clearVoyageur();
    this.router.navigateByUrl('/voyageur');
  }

  voyageurTypeBadgeClass(type: VoyageurResponse['typeVoyageur']): string {
    if (type === 'VIP') {
      return 'ds-badge-info';
    }
    if (type === 'LOCAL') {
      return 'ds-badge-success';
    }
    return 'ds-badge-warning';
  }
}
