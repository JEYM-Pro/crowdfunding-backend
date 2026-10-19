package co.udea.crowdfunding.controller;

import co.udea.crowdfunding.config.UserPrincipal;
import co.udea.crowdfunding.dto.CampaignResponse;
import co.udea.crowdfunding.dto.CategoryResponse;
import co.udea.crowdfunding.dto.CreateCampaignRequest;
import co.udea.crowdfunding.dto.UpdateCampaignRequest;
import co.udea.crowdfunding.entity.CampaignStatus;
import co.udea.crowdfunding.entity.Category;
import co.udea.crowdfunding.repository.CategoryRepository;
import co.udea.crowdfunding.service.CampaignService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CAMPAIGN_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final Instant DEADLINE = Instant.parse("2026-11-09T12:00:00Z");

    @Mock
    private CampaignService campaignService;
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CampaignController campaignController;

    private final UserPrincipal principal = new UserPrincipal(USER_ID, "ana@correo.com", "sponsor");

    @Test
    void publish_returnsCreatedWithTheNewCampaign() {
        CreateCampaignRequest request = new CreateCampaignRequest("Título", "Descripción",
                new BigDecimal("1000000"), DEADLINE, CATEGORY_ID, null, null);
        CampaignResponse created = response();
        when(campaignService.create(USER_ID, request)).thenReturn(created);

        ResponseEntity<CampaignResponse> result = campaignController.publish(principal, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isSameAs(created);
    }

    @Test
    void update_returnsOkWithTheUpdatedCampaign() {
        UpdateCampaignRequest request = new UpdateCampaignRequest("Título", "Descripción", null, null, null, null, null);
        CampaignResponse updated = response();
        when(campaignService.update(USER_ID, CAMPAIGN_ID, request)).thenReturn(updated);

        ResponseEntity<CampaignResponse> result = campaignController.update(principal, CAMPAIGN_ID, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(updated);
    }

    @Test
    void getById_returnsOkWithTheCampaign() {
        CampaignResponse found = response();
        when(campaignService.getById(CAMPAIGN_ID)).thenReturn(found);

        ResponseEntity<CampaignResponse> result = campaignController.getById(CAMPAIGN_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(found);
    }

    @Test
    void listActive_returnsOkWithActiveCampaigns() {
        when(campaignService.exploreActiveCampaigns(null, null, null)).thenReturn(List.of(response()));

        ResponseEntity<List<CampaignResponse>> result = campaignController.listActive(null, null, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
        verify(campaignService).exploreActiveCampaigns(null, null, null);
    }

    @Test
    void listManaged_returnsCampaignsForAuthenticatedOwner() {
        when(campaignService.listManaged(USER_ID, false)).thenReturn(List.of(response()));

        ResponseEntity<List<CampaignResponse>> result = campaignController.listManaged(principal);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
        verify(campaignService).listManaged(USER_ID, false);
    }

    @Test
    void listManaged_requestsAllCampaignsForAdmin() {
        UserPrincipal admin = new UserPrincipal(USER_ID, "admin@correo.com", "admin");
        when(campaignService.listManaged(USER_ID, true)).thenReturn(List.of(response()));

        ResponseEntity<List<CampaignResponse>> result = campaignController.listManaged(admin);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(campaignService).listManaged(USER_ID, true);
    }

    @Test
    void categories_areListedAlphabeticallyAsIdAndName() {
        Category category = new Category("Educación");
        ReflectionTestUtils.setField(category, "id", CATEGORY_ID);
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(category));

        ResponseEntity<List<CategoryResponse>> result = new CategoryController(categoryRepository).list();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).containsExactly(new CategoryResponse(CATEGORY_ID, "Educación"));
    }

    private CampaignResponse response() {
        return new CampaignResponse(CAMPAIGN_ID, "Título", "Descripción", new BigDecimal("1000000"),
                BigDecimal.ZERO, DEADLINE, CampaignStatus.ACTIVE, CATEGORY_ID, "Tecnología", List.of(),
                USER_ID, "Ana Pérez", "ana@correo.com", false, List.of(), 0,
                Instant.parse("2026-10-09T12:00:00Z"), null, List.of());
    }
}
