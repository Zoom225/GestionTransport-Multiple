package com.transport.gestiontransport.dto.response;

import java.time.LocalDate;

public record RestrictionResponse(Long id, String motif, String sourceEvenement,
        LocalDate dateDebut, LocalDate dateFin, boolean active) {}
