package digit.service;
import digit.config.DTRConfiguration;
import digit.util.UserUtil;
import digit.web.models.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
@Service
@Slf4j
public class UserService {
    private UserUtil userUtils;
    private DTRConfiguration config;
    @Autowired
    public UserService(UserUtil userUtils, DTRConfiguration config) {
        this.userUtils = userUtils;
        this.config = config;
    }

    //Calls user service to enrich user from search or upsert user
    public void callUserService(DeathRegistrationRequest request){
        request.getDeathRegistrationApplications().forEach(application -> {
            if(!StringUtils.isEmpty(application.getApplicant().getId()))
                enrichUser(application, request.getRequestInfo());
            else {
                User user = createApplicantUserApplication(application);
                application.getApplicant().setId(upsertUser(user, request.getRequestInfo()));
            }
        });

    }

    public User createApplicantUser(User applicant){
        User user = User.builder().userName(applicant.getUserName())
                .id(applicant.getId())
                .name(applicant.getUserName())
                .mobileNumber(applicant.getMobileNumber())
                .emailId(applicant.getEmailId())
                .altContactNumber(applicant.getAltContactNumber())
                .tenantId(applicant.getTenantId())
                .type(applicant.getType())
                .roles(applicant.getRoles())
                .build();
        String tenantId = applicant.getTenantId();
        return user;
    }

    public User createApplicantUserApplication(DeathRegistrationApplication application){
        Applicant applicant = application.getApplicant();
        User user = User.builder().userName(applicant.getUserName())
                .name(applicant.getUserName())
                .mobileNumber(applicant.getMobileNumber())
                .emailId(applicant.getEmailId())
                .altContactNumber(applicant.getAltContactNumber())
                .tenantId(applicant.getTenantId())
                .type(applicant.getType())
                .roles(applicant.getRoles())
                .build();
        String tenantId = applicant.getTenantId();
        return user;
    }

    private String upsertUser(User user, RequestInfo requestInfo){

        String tenantId = user.getTenantId();
        User userServiceResponse = null;

        // Search on mobile number as user name
        UserDetailResponse userDetailResponse = searchUser(userUtils.getStateLevelTenant(tenantId),null, user.getMobileNumber());
        if (!userDetailResponse.getUser().isEmpty()) {
            User userFromSearch = userDetailResponse.getUser().get(0);
            log.info(userFromSearch.toString());
            if(!user.getUserName().equalsIgnoreCase(userFromSearch.getUserName())){
                userServiceResponse = updateUser(requestInfo,user,userFromSearch);
            }
            else userServiceResponse = userDetailResponse.getUser().get(0);
        }
        else {
            userServiceResponse = createUser(requestInfo,tenantId,user);
        }

        return userServiceResponse.getUuid();
    }

    //Creates the user from the given userInfo by calling user service
    private User createUser(RequestInfo requestInfo, String tenantId, User userInfo) {
        userUtils.addUserDefaultFields(userInfo.getMobileNumber(),tenantId, userInfo);
        StringBuilder uri = new StringBuilder(config.getUserHost())
                .append(config.getUserContextPath())
                .append(config.getUserCreateEndpoint());

        CreateUserRequest user = new CreateUserRequest(requestInfo, userInfo);
        log.info(user.getUser().toString());
        UserDetailResponse userDetailResponse = userUtils.userCall(user, uri);

        return userDetailResponse.getUser().get(0);

    }
 //Updates the given user by calling user service

    private User updateUser(RequestInfo requestInfo, User user, User userFromSearch) {

        userFromSearch.setUserName(user.getName());
        userFromSearch.setActive(true);

        StringBuilder uri = new StringBuilder(config.getUserHost())
                .append(config.getUserContextPath())
                .append(config.getUserUpdateEndpoint());


        UserDetailResponse userDetailResponse = userUtils.userCall(new CreateUserRequest(requestInfo, userFromSearch), uri);

        return userDetailResponse.getUser().get(0);

    }
//calls the user search API based on the given accountId and userName

    public UserDetailResponse searchUser(String stateLevelTenant, String accountId, String userName){

        UserSearchRequest userSearchRequest =new UserSearchRequest();
        userSearchRequest.setActive(true);
        userSearchRequest.setUserType("CITIZEN");
        userSearchRequest.setTenantId(stateLevelTenant);

        if(StringUtils.isEmpty(accountId) && StringUtils.isEmpty(userName))
            return null;

        if(!StringUtils.isEmpty(accountId))
            userSearchRequest.setUuid(Collections.singletonList(accountId));

        if(!StringUtils.isEmpty(userName))
            userSearchRequest.setUserName(userName);

        StringBuilder uri = new StringBuilder(config.getUserHost()).append(config.getUserSearchEndpoint());
        return userUtils.userCall(userSearchRequest,uri);

    }

}