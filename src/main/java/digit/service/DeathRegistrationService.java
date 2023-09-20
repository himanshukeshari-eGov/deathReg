package digit.service;
import com.google.common.collect.ImmutableList;
import digit.config.DTRConfiguration;
import digit.enrichment.DeathApplicationEnrichment;
import digit.kafka.Producer;
import digit.repository.DeathRegistrationRepository;
import digit.util.UserUtil;
import digit.validators.DeathApplicationValidator;
import digit.web.models.*;
import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.request.RequestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
@Service
@Slf4j
public class DeathRegistrationService {
    @Autowired
    private DeathApplicationValidator validator;
    @Autowired
    private UserUtil userUtil;
    @Autowired
    private DeathApplicationEnrichment enrichmentUtil;
    @Autowired
    private UserService userService;
    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private DeathRegistrationRepository deathRegistrationRepository;
    @Autowired
    private Producer producer;
@Autowired
    private DTRConfiguration configuration;

    public List<DeathRegistrationApplication> registerDtRequest(DeathRegistrationRequest deathRegistrationRequest) {
        // Validate applications
        validator.validateDeathApplication(deathRegistrationRequest);

        // Enrich applications
        enrichmentUtil.enrichDeathApplication(deathRegistrationRequest);

        // Enrich/Upsert user in upon death registration
        userService.callUserService(deathRegistrationRequest);

        // Initiate workflow for the new application
        workflowService.updateWorkflowStatus(deathRegistrationRequest);

        // Push the application to the topic for persister to listen and persist
        producer.push("save-dt-application", deathRegistrationRequest);

        // Return the response back to user
        return deathRegistrationRequest.getDeathRegistrationApplications();
    }

    public List<DeathRegistrationApplication> searchDtApplications(RequestInfo requestInfo, DeathApplicationSearchCriteria deathApplicationSearchCriteria) {
        // Fetch applications from database according to the given search criteria
        List<DeathRegistrationApplication> applications = deathRegistrationRepository.getApplications(deathApplicationSearchCriteria);

        // If no applications are found matching the given criteria, return an empty list
        if(CollectionUtils.isEmpty(applications))
            return new ArrayList<>();


        applications.forEach(application -> {
            UserDetailResponse userDetailResponse = userService.searchUser("pb",application.getApplicantId(),null);
            User applicant = userService.createApplicantUser(userDetailResponse.getUser().get(0));
            applicant.setUuid(application.getApplicantId());
            Applicant applicantReal=Applicant.builder().userName(applicant.getUserName())
                    .name(applicant.getName())
                    .id(applicant.getId().toString())
                    .uuid(application.getApplicantId())
                    .mobileNumber(applicant.getMobileNumber())
                    .emailId(applicant.getEmailId())
                    .altContactNumber(applicant.getAltContactNumber())
                    .tenantId(applicant.getTenantId())
                    .type(applicant.getType())
                    .roles(applicant.getRoles()).build();
            application.setApplicant(applicantReal);
        });

        //WORKFLOW INTEGRATION
        applications.forEach(application -> {
            ProcessInstance obj=workflowService.getCurrentWorkflow(requestInfo, application.getTenantId(), application.getApplicationNumber());
            application.setWorkflow(Workflow.builder().status(obj.getState().getState()).build());
        });

        // Otherwise return the found applications
        return applications;
    }

    public DeathRegistrationApplication updateDtApplication(DeathRegistrationRequest deathRegistrationRequest) {
        // Validate whether the application that is being requested for update indeed exists
        DeathRegistrationApplication existingApplication = validator.validateApplicationExistence(deathRegistrationRequest.getDeathRegistrationApplications().get(0));
        existingApplication.setWorkflow(deathRegistrationRequest.getDeathRegistrationApplications().get(0).getWorkflow());
        if(deathRegistrationRequest.getDeathRegistrationApplications().get(0).getWorkflow() == null){
            producer.push("update-dt-application", deathRegistrationRequest);
return deathRegistrationRequest.getDeathRegistrationApplications().get(0);
        }
        workflowService.updateWorkflowStatus(deathRegistrationRequest);
        // Just like create request, update request will be handled asynchronously by the persister
        producer.push("update-dt-application", deathRegistrationRequest);
        return deathRegistrationRequest.getDeathRegistrationApplications().get(0);
    }
}
