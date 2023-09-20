CREATE TABLE dt_registration(
  id character varying(64),
  tenantId character varying(64),
  applicationNumber character varying(64),
  deceasedFirstName character varying(64),
  deceasedLastName character varying(64),
  applicationStatus character varying(64),
  applicantId character varying(64),
  timeOfDeath bigint,
  createdBy character varying(64),
  lastModifiedBy character varying(64),
  createdTime bigint,
  lastModifiedTime bigint,
 CONSTRAINT uk_dt_registration UNIQUE (id)
);
CREATE TABLE dt_address(
   id character varying(64),
   tenantId character varying(64),
   latitude FLOAT,
   longitude FLOAT,
   doorNo character varying(64),
   buildingName character varying(64),
   type character varying(64),
   addressId character varying(64),
   addressNumber character varying(64),
   addressLine1 character varying(256),
   addressLine2 character varying(256),
   landmark character varying(64),
   street character varying(64),
   city character varying(64),
   locality character varying(64),
   pincode character varying(64),
   detail character varying(64),
   registrationId character varying(64),
   createdBy character varying(64),
   lastModifiedBy character varying(64),
   createdTime bigint,
   lastModifiedTime bigint,
   CONSTRAINT uk_dt_address PRIMARY KEY (id),
   CONSTRAINT fk_dt_address FOREIGN KEY (registrationId) REFERENCES dt_registration (id)
     ON UPDATE CASCADE
     ON DELETE CASCADE
);