package com.microvault.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class GatewayApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void gatewayStartsWithRoutes() {
        assertNotNull(applicationContext);
        assertNotNull(routeLocator);
    }
}
