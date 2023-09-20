package digit.enrichment;



import digit.models.coremodels.UserDetailResponse;
//import digit.service.UserService;
//import digit.util.IdgenUtil;
//import digit.util.UserUtil;
import digit.util.IdgenUtil;
import digit.web.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
@Component
@Slf4j
public class DeathApplicationEnrichment {

    @Autowired
    private IdgenUtil idgenUtil;

// Enrich the Death Application
    public void enrichDeathApplication(DeathRegistrationRequest deathRegistrationRequest) {
        List<String> deathRegistrationIdList = idgenUtil.getIdList(deathRegistrationRequest.getRequestInfo(), deathRegistrationRequest.getDeathRegistrationApplications().get(0).getTenantId(), "himanshu.registrationid", "HK-DTR-[cy:yyyy-MM-dd]-[SEQ_EG_DTR_ID]", deathRegistrationRequest.getDeathRegistrationApplications().size());
        Integer index = 0;
        for(DeathRegistrationApplication application : deathRegistrationRequest.getDeathRegistrationApplications()){
            // Enrich audit details
            AuditDetails auditDetails = AuditDetails.builder().createdBy(deathRegistrationRequest.getRequestInfo().getUserInfo().getUuid()).createdTime(System.currentTimeMillis()).lastModifiedBy(deathRegistrationRequest.getRequestInfo().getUserInfo().getUuid()).lastModifiedTime(System.currentTimeMillis()).build();
            application.setAuditDetails(auditDetails);
            // Enrich UUID
            application.setId(UUID.randomUUID().toString());
            // Enrich registration Id
            application.getAddressOfDeceased().setRegistrationId(application.getId());
            // Enrich address UUID
            application.getAddressOfDeceased().setId(UUID.randomUUID().toString());
            System.out.println(application.getAddressOfDeceased().getId());
            //Enrich application number from IDgen
            application.setApplicationNumber(deathRegistrationIdList.get(index++));
        }
    }




}