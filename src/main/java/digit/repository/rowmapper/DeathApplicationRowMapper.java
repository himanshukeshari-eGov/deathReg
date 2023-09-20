package digit.repository.rowmapper;


        import digit.web.models.*;
        import org.springframework.dao.DataAccessException;
        import org.springframework.jdbc.core.ResultSetExtractor;
        import org.springframework.stereotype.Component;

        import java.sql.ResultSet;
        import java.sql.SQLException;
        import java.util.ArrayList;
        import java.util.LinkedHashMap;
        import java.util.List;
        import java.util.Map;
// Row Mapper
@Component
public class DeathApplicationRowMapper implements ResultSetExtractor<List<DeathRegistrationApplication>> {
    public List<DeathRegistrationApplication> extractData(ResultSet rs) throws SQLException, DataAccessException {
        Map<String,DeathRegistrationApplication> deathRegistrationApplicationMap = new LinkedHashMap<>();

        while (rs.next()){
            System.out.println("RESULT SET IS "+rs.getObject("dapplicationnumber",String.class));
            String uuid = rs.getString("dapplicationnumber");
            DeathRegistrationApplication deathRegistrationApplication = deathRegistrationApplicationMap.get(uuid);

            if(deathRegistrationApplication == null) {

                Long lastModifiedTime = rs.getLong("dlastModifiedTime");
                if (rs.wasNull()) {
                    lastModifiedTime = null;
                }

                AuditDetails auditdetails = AuditDetails.builder()
                        .createdBy(rs.getString("dcreatedBy"))
                        .createdTime(rs.getLong("dcreatedTime"))
                        .lastModifiedBy(rs.getString("dlastModifiedBy"))
                        .lastModifiedTime(lastModifiedTime)
                        .build();

                deathRegistrationApplication = DeathRegistrationApplication.builder()
                        .applicationNumber(rs.getString("dapplicationnumber"))
                        .tenantId(rs.getString("dtenantid"))
                        .id(rs.getString("did"))
                        .deceasedFirstName(rs.getString("ddeceasedFirstName"))
                        .deceasedLastName(rs.getString("ddeceasedLastName"))
                        .timeOfDeath(rs.getInt("dtimeOfDeath"))
                        .applicantId(rs.getString("dapplicantId"))
                        .auditDetails(auditdetails)
                        .build();
            }
            addChildrenToProperty(rs, deathRegistrationApplication);
            deathRegistrationApplicationMap.put(uuid, deathRegistrationApplication);
        }
        return new ArrayList<>(deathRegistrationApplicationMap.values());
    }

    private void addChildrenToProperty(ResultSet rs, DeathRegistrationApplication deathRegistrationApplication)
            throws SQLException {
        addAddressToApplication(rs, deathRegistrationApplication);
    }
    // Function for adding the Address
    private void addAddressToApplication(ResultSet rs, DeathRegistrationApplication deathRegistrationApplication) throws SQLException {

        System.out.println(rs);

        Address address = Address.builder()
                .id(rs.getString("aid"))
                .tenantId(rs.getString("dtenantId"))
                .latitude(rs.getDouble("alatitude"))
                .longitude(rs.getDouble("alongitude"))
                .addressId(rs.getString("aaddressid"))
                .addressNumber(rs.getString("aaddressnumber"))
                .addressLine1(rs.getString("aaddressline1"))
                .addressLine2(rs.getString("aaddressline2"))
                .landmark(rs.getString("alandmark"))
                .city(rs.getString("acity"))
                .pincode(rs.getString("apincode"))
                .detail(rs.getString("adetail"))
                .registrationId(rs.getString("aregistrationid") )
                .build();

        deathRegistrationApplication.setAddressOfDeceased(address);

    }

}
