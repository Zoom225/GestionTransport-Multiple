package com.transport.gestiontransport.dto.response;

import java.time.LocalDate;

public record IndisponibiliteServiceResponse(
        Long id,
        LocalDate date,
        String raison,
        Boolean global,
        Long agenceId,
        String agenceNom
) {
}
