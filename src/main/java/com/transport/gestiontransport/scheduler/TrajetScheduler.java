package com.transport.gestiontransport.scheduler;

import com.transport.gestiontransport.service.TrajetService;
import com.transport.gestiontransport.service.PaiementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrajetScheduler {

    private final TrajetService trajetService;
    private final PaiementService paiementService;

    // tourne tous les jours à minuit
    @Scheduled(cron = "0 0 0 * * *")
    public void processExpiredPrivateTrajets() {
        log.info("Scheduler started : checking expired private trajets...");
        trajetService.checkAndConvertExpiredPrivateTrajets();
        log.info("Scheduler done : expired private trajets processed");
    }

    // tourne tous les jours à minuit cinq (après le premier)
    @Scheduled(cron = "0 5 0 * * *")
    public void processUnpaidReservations() {
        log.info("Scheduler started : checking unpaid reservations...");
        paiementService.checkUnpaidBeforeTrajet();
        log.info("Scheduler done : unpaid reservations processed");
    }
}
