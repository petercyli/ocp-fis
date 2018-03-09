package gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel;

import gov.samhsa.ocp.ocpfis.service.dto.RelatedPersonDto;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
        List<ContactPoint> contactPoints = relatedPersonDto.getTelecoms().stream()
                .map(telecomDto -> {
                    ContactPoint contactPoint = new ContactPoint();
                    telecomDto.getSystem().ifPresent(system -> contactPoint.setSystem(ContactPoint.ContactPointSystem.valueOf(system.toUpperCase())));
                    telecomDto.getValue().ifPresent(contactPoint::setValue);
                    telecomDto.getUse().ifPresent(user -> contactPoint.setUse(ContactPoint.ContactPointUse.valueOf(user.toUpperCase())));
                    return contactPoint;
                })
                .collect(Collectors.toList());
        relatedPerson.setTelecom(contactPoints);

        //gender
        Enumerations.AdministrativeGender gender = FhirUtil.getPatientGender(relatedPersonDto.getGenderCode());
        relatedPerson.setGender(gender);

        //birthdate
        relatedPerson.setBirthDate(DateUtil.convertStringToDate(relatedPersonDto.getBirthDate()));

        //addressess
        List<Address> addresses = relatedPersonDto.getAddresses().stream()
                .map(addressDto -> {
                    Address address = new Address();
                    address.addLine(addressDto.getLine1());
                    address.addLine(addressDto.getLine2());
                    address.setCity(addressDto.getCity());
                    address.setState(addressDto.getStateCode());
                    address.setPostalCode(addressDto.getPostalCode());
                    address.setCountry(addressDto.getCountryCode());
                    return address;
                })
                .collect(Collectors.toList());
        relatedPerson.setAddress(addresses);

        //period
        Period period = new Period();
        period.setStart(DateUtil.convertStringToDate(relatedPersonDto.getStartDate()));
        period.setEnd(DateUtil.convertStringToDate(relatedPersonDto.getEndDate()));
        relatedPerson.setPeriod(period);

        return relatedPerson;
    }
}
