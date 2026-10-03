package com.microvault.dashboard.serviceimpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microvault.dashboard.client.AdminClient;
import com.microvault.dashboard.client.AuthClient;
import com.microvault.dashboard.client.FinanceClient;
import com.microvault.dashboard.dto.DashboardResponse;
import com.microvault.dashboard.exception.ResourceNotFoundException;
import com.microvault.dashboard.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private AuthClient authClient;
    @Mock
    private FinanceClient financeClient;
    @Mock
    private AdminClient adminClient;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldCombineTheWorkspace() {
        UUID id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        ObjectNode user = mapper.createObjectNode();
        user.put("id", id.toString());
        user.put("fullName", "Aarav Sharma");
        user.put("role", "user");

        ObjectNode financeProfile = mapper.createObjectNode();
        financeProfile.put("monthlyIncome", 50000);
        financeProfile.put("monthlyExpenses", 20000);
        financeProfile.put("monthlyBudget", 15000);

        ObjectNode finance = mapper.createObjectNode();
        finance.set("finance", financeProfile);
        finance.set("transactions", mapper.createArrayNode());
        finance.set("goals", mapper.createArrayNode());
        finance.set("savings", mapper.createArrayNode().add(mapper.createObjectNode().put("amount", 1000)));
        finance.set("budgets", mapper.createArrayNode());
        finance.put("setupSkipped", false);

        ObjectNode admin = mapper.createObjectNode();
        admin.set("notifications", mapper.createArrayNode());
        admin.set("feedback", mapper.createArrayNode());
        admin.set("news", mapper.createArrayNode());

        when(authClient.currentUser("Bearer token")).thenReturn(user);
        when(financeClient.workspace(id)).thenReturn(finance);
        when(adminClient.workspace(id, "user")).thenReturn(admin);

        DashboardResponse response = dashboardService.load("Bearer token");

        assertEquals("Aarav Sharma", response.getUser().path("fullName").asText());
        assertEquals(1000, response.getStats().getTotalSavings().intValue());
        assertEquals(15000, response.getStats().getMonthBudget().intValue());
    }

    @Test
    void shouldRequireAnIdentity() {
        when(authClient.currentUser(null)).thenThrow(new UnauthorizedException("Login is required"));
        assertThrows(UnauthorizedException.class, () -> dashboardService.load(null));
    }

    @Test
    void shouldSurfaceAMissingUser() {
        when(authClient.currentUser("Bearer token"))
                .thenThrow(new ResourceNotFoundException("No user found for this dashboard"));
        assertThrows(ResourceNotFoundException.class, () -> dashboardService.load("Bearer token"));
    }
}
