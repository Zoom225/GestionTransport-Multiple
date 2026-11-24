/// <reference types="vite/client" />
import { computed, signal, ɵresolveComponentResources } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ReservationsApiService } from '../../../core/api/reservations-api.service';
import { TrajetsApiService } from '../../../core/api/trajets-api.service';
import { VoyageursApiService } from '../../../core/api/voyageurs-api.service';
import { VoyageurSessionService } from '../../../core/auth/voyageur-session.service';
import { SanctionResponse, VoyageurResponse } from '../../../shared/models/voyageur.model';
import { VoyageurProfilePage } from './voyageur-profile-page.component';
import template from './voyageur-profile-page.component.html?raw';

describe('Participation dans le profil voyageur', () => {
  const paiement = vi.fn();
  const etat = signal<VoyageurResponse>({
    id: 1, matricule: 'V1001', nom: 'Martin', prenom: 'Lou', email: '',
    typeVoyageur: 'VIP', agenceId: null, agenceNom: null, solde: 100, peutProposerTrajet: true,
  });
  let historique: SanctionResponse[];

  beforeEach(async () => {
    await ɵresolveComponentResources(async (url) => url.endsWith('.html') ? template : '');
    paiement.mockReset();
    etat.update((profil) => ({
      ...profil,
      participation: {
        peutParticiper: false, peutCreerTrajet: false, peutRejoindreTrajet: false,
        suspensionActive: true, soldeDu: 100, detteBloquante: true, code: 'VOYAGEUR_SUSPENDU',
        debutSuspension: '2026-03-27T12:00:00Z', finSuspension: '2026-04-03T12:00:00Z',
        message: 'Votre compte est suspendu.', motifRestriction: 'Trajet privé incomplet',
      },
    }));
    historique = [{
      id: 2, trajetId: 10, motif: 'Trajet privé incomplet', debut: '2026-03-27T12:00:00Z',
      fin: '2026-04-03T12:00:00Z', montant: 100, payee: false, active: true,
    }];
    TestBed.configureTestingModule({
      imports: [VoyageurProfilePage],
      providers: [
        provideRouter([]),
        { provide: VoyageurSessionService, useValue: {
          voyageurId: () => 1, token: () => null,
          participation: computed(() => etat().participation),
          setVoyageur: (profil: VoyageurResponse) => etat.set(profil),
        } },
        { provide: VoyageursApiService, useValue: {
          getCurrent: () => of(etat()), getSanctions: () => of(historique),
        } },
        { provide: ReservationsApiService, useValue: { getByVoyageur: () => of([]) } },
        { provide: TrajetsApiService, useValue: { getByOrganisateur: () => of([]), payerPenalite: paiement } },
      ],
    });
  });

  it('actualise le solde et conserve le blocage après règlement, puis réaffiche la création au retour des droits', () => {
    paiement.mockImplementation(() => {
      historique = historique.map((sanction) => ({ ...sanction, payee: true }));
      etat.update((profil) => ({
        ...profil, solde: 0, participation: {
          ...profil.participation!, soldeDu: 0, detteBloquante: false,
          message: 'Votre suspension reste toutefois active.',
        },
      }));
      return of(undefined);
    });
    const fixture = TestBed.createComponent(VoyageurProfilePage);
    fixture.detectChanges();
    const page: HTMLElement = fixture.nativeElement;
    const reglement = [...page.querySelectorAll('button')].find((b) =>
      b.textContent?.includes('Enregistrer le règlement interne'),
    )!;
    expect(page.textContent).toContain('100.00');
    expect(page.textContent).toContain('Trajet privé incomplet');
    expect(page.querySelector('a[href="/voyageur/trajets/new"]')).toBeNull();
    reglement.click();
    fixture.detectChanges();
    expect(paiement).toHaveBeenCalledExactlyOnceWith(10);
    expect(page.textContent).toContain('Votre suspension reste toutefois active.');
    expect(page.textContent).toContain('Règlement interne enregistré.');
    expect(page.textContent).not.toContain('Enregistrer le règlement interne');
    expect(page.querySelector('a[href="/voyageur/trajets/new"]')).toBeNull();

    etat.update((profil) => ({
      ...profil, participation: {
        ...profil.participation!, code: null, suspensionActive: false,
        peutParticiper: true, peutCreerTrajet: true, peutRejoindreTrajet: true,
        message: 'Vous pouvez à nouveau créer et rejoindre des trajets.',
      },
    }));
    fixture.detectChanges();
    expect(page.querySelector('a[href="/voyageur/trajets/new"]')).not.toBeNull();
  });

  it('affiche le refus métier et laisse le règlement disponible pour une nouvelle tentative', () => {
    paiement.mockReturnValue(throwError(() => ({ error: { message: 'Le solde doit être régularisé.' } })));
    const fixture = TestBed.createComponent(VoyageurProfilePage);
    fixture.detectChanges();
    fixture.componentInstance.payerPenalite(historique[0]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Le solde doit être régularisé.');
    expect(fixture.componentInstance.reglementEnCours()).toBeNull();
    expect(fixture.componentInstance.sanctions()[0].payee).toBe(false);
  });
});
