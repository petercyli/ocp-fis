package gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel;

import gov.samhsa.ocp.ocpfis.service.dto.AddressDto;
import gov.samhsa.ocp.ocpfis.service.dto.RelatedPersonDto;
import gov.samhsa.ocp.ocpfis.service.dto.TelecomDto;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import gov.samhsa.ocp.ocpfis.util.FhirUtil;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.RelatedPerson;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RelatedPersonDtoToRelatedPersonConverter {

    public static RelatedPerson map(RelatedPersonDto relatedPersonDto) throws ParseException {

        RelatedPerson relatedPerson = new RelatedPerson();

        //id
        relatedPerson.setId(relatedPersonDto.getRelatedPersonId());

        //identifier
        Identifier identifier = new Identifier();
        identifier.setSystem(relatedPersonDto.getIdentifierType());
        identifier.setValue(relatedPersonDto.getIdentifierValue());
        relatedPerson.setIdentifier(Collections.singletonList(identifier));

        //active
        relatedPerson.setActive(relatedPersonDto.isActive());

        //patient
        relatedPerson.getPatient().setReference("Patient/" + relatedPersonDto.getPatient());

        //relationship
        Coding codingRelationship = new Coding();
        codingRelationship.setCode(relatedPersonDto.getRelationshipCode());
        codingRelationship.setDisplay(relatedPersonDto.getRelationshipValue());
        CodeableConcept codeableConceptRelationship = new CodeableConcept().addCoding(codingRelationship);
        relatedPerson.setRelationship(codeableConceptRelationship);

        //name
        HumanName humanName = new HumanName().addGiven(relatedPersonDto.getFirstName()).setFamily(relatedPersonDto.getLastName());
        relatedPerson.setName(Collections.singletonList(humanName));

        //telecoms
        if (relatedPersonDto.getTelecoms() != null) {

            List<TelecomDto> telecomDtos = relatedPersonDto.getTelecoms();
            List<ContactPoint> contactPoints = new ArrayList<>();

            if (telecomDtos != null && telecomDtos.size() > 0) {
                int rank = 0;
                for (TelecomDto telecomDto : telecomDtos) {
                    ContactPoint contactPoint = new ContactPoint();

                    contactPoint.setRank(++rank);

                    if (telecomDto.getSystem().isPresent()) {
                        contactPoint.setSystem(ContactPoint.ContactPointSystem.valueOf(telecomDto.getSystem().get()));
                    }
                    if (telecomDto.getUse().isPresent()) {
                        contactPoint.setUse(ContactPoint.ContactPointUse.valueOf(telecomDto.getUse().get()));
                    }

                    if (telecomDto.getValue().isPresent()) {
                        contactPoint.setValue(telecomDto.getValue().get());
                    }

                    contactPoints.add(contactPoint);
                }
                relatedPerson.setTelecom(contactPoints);
            }
        }

        //gender
        Enumerations.AdministrativeGender gender = FhirUtil.getPatientGender(relatedPersonDto.getGenderCode());
        relatedPerson.setGender(gender);

        //birthdate
        relatedPerson.setBirthDate(DateUtil.convertStringToDate(relatedPersonDto.getBirthDate()));

        //addressess
        if (relatedPersonDto.getAddresses() != null) {

            List<AddressDto> addressDtos = relatedPersonDto.getAddresses();
            List<Address> addresses = new ArrayList<>();

            if (addressDtos != null && addressDtos.size() > 0) {
                for (AddressDto addressDto : addressDtos) {
                    Address address = new Address();
                    if (addressDto.getLine1() != null)
                        address.addLine(addressDto.getLine1());
                    if (addressDto.getLine2() != null)
                        address.addLine(addressDto.getLine2());
                    if (addressDto.getCity() != null)
                        address.setCity(addressDto.getCity());
                    if (addressDto.getPostalCode() != null)
                        address.setPostalCode(addressDto.getPostalCode());
                    if (addressDto.getStateCode() != null)
                        address.setState(addressDto.getStateCode());
                    if (addressDto.getCountryCode() != null)
                        address.setCountry(addressDto.getCountryCode());

                    addresses.add(address);
                }
                relatedPerson.setAddress(addresses);

            }
        }

        //period
        Period period = new Period();
        period.setStart(DateUtil.convertStringToDate(relatedPersonDto.getStartDate()));
        period.setEnd(DateUtil.convertStringToDate(relatedPersonDto.getEndDate()));
        relatedPerson.setPeriod(period);

        return relatedPerson;
    }
}
