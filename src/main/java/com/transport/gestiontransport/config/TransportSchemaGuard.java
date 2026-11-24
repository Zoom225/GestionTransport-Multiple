package com.transport.gestiontransport.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/** Empêche ddl-auto=update de créer des tables parallèles avant une migration explicite. */
@Configuration(proxyBeanMethods = false)
public class TransportSchemaGuard {
    @Bean
    static EntityManagerFactoryDependsOnPostProcessor schemaGuardBeforeHibernate() {
        return new EntityManagerFactoryDependsOnPostProcessor("transportSchemaPreflight");
    }

    @Bean
    InitializingBean transportSchemaPreflight(DataSource dataSource) {
        return () -> verify(dataSource);
    }

    static void verify(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("PostgreSQL")) return;
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT count(*) FROM information_schema.tables "
                         + "WHERE table_schema='public' AND table_name IN "
                         + "('sites','terrains','membres','matches','penalites','jours_fermeture')")) {
                result.next();
                if (result.getLong(1) > 0) {
                    throw new IllegalStateException("Schéma SQL historique détecté : démarrage arrêté avant Hibernate. "
                            + "Sauvegarder, valider la migration Transport sur copie puis obtenir la validation métier. "
                            + "Aucune création automatique de tables parallèles n'est autorisée.");
                }
            }
        }
    }
}
