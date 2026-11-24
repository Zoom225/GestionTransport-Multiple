import { TypeVoyageur } from './enums.model';

export interface LoginRequest {
  matricule: string;
  password: string; // Added password field
}

export interface VoyageurRequest {
  peutProposerTrajet?: boolean;
  matricule: string;
  nom: string;
  prenom: string;
  email: string;
  typeVoyageur: TypeVoyageur;
  agenceId?: number;
  password?: string;
}

export interface VoyageurResponse {
  participation?: ParticipationStatus;
  peutProposerTrajet?: boolean;
  id: number;
  matricule: string;
  nom: string;
  prenom: string;
  email: string;
  typeVoyageur: TypeVoyageur;
  agenceId: number | null;
  agenceNom: string | null;
  solde: number;
  token?: string;
}

export interface LoginResponse extends VoyageurResponse {
  token: string;
}

export interface ParticipationStatus {
  peutParticiper: boolean;
  suspensionActive: boolean;
  finSuspension: string | null;
  debutSuspension: string | null;
  soldeDu: number;
  detteBloquante: boolean;
  peutCreerMatch: boolean;
  peutRejoindreMatch: boolean;
  motifRestriction: string | null;
  code: string | null;
  message: string;
}

export interface SanctionResponse {
  id: number;
  trajetId: number | null;
  motif: string;
  debut: string | null;
  fin: string | null;
  montant: number | null;
  payee: boolean;
  active: boolean;
}
export interface RestrictionResponse {
  id: number;
  motif: string;
  sourceEvenement: string | null;
  dateDebut: string | null;
  dateFin: string;
  active: boolean;
}
