package com.firefly.core.banking.cards.authorization.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Card Transaction Authorization Center.
 * This microservice is responsible for orchestrating and executing the decision
 * of authorization online (approve/decline + reason) for each payment attempt,
 * ATM withdrawal, or balance inquiry.
 */
@SpringBootApplication(scanBasePackages = "com.firefly.core.banking.cards.authorization")
@EnableR2dbcRepositories(basePackages = "com.firefly.core.banking.cards.authorization.models.repositories")
@EntityScan(basePackages = "com.firefly.core.banking.cards.authorization.models.entities")
@EnableScheduling
@OpenAPIDefinition(
        info = @Info(
                title = "Card Transaction Authorization Center API",
                version = "1.0.0",
                description = "API for card transaction authorization processing",
                contact = @Contact(
                        name = "Firefly Support",
                        email = "support@getfirefly.io"
                ),
                license = @License(
                        name = "Apache 2.0",
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                )
        )
)
public class CoreCardsAuthroizationCenterApplication {

    /**
     * Main method to start the application.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(CoreCardsAuthroizationCenterApplication.class, args);
    }
}
