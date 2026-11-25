import { StatutPaiement, StatutReservation } from './enums.model';

export interface ReservationRequest {
  trajetId: number;
  voyageurId: number;
  requesterId: number;
}

export interface ReservationInviteRequest {
  trajetId: number;
  inviteeMatricules: string[];
}

export interface PaiementResponse {
  id: number;
  montant: number;
  statut: StatutPaiement;
  datePaiement: string | null;
}

export interface ReservationResponse {
  absenceDeclareeLe?: string | null;
  motifAbsence?: string | null;
  villeDepart?: string | null;
  villeArrivee?: string | null;
  id: number;
  trajetId: number;
  trajetDateTime: string;
  voyageurId: number;
  voyageurNom: string;
  statut: StatutReservation;
  paiement: PaiementResponse | null;
}

