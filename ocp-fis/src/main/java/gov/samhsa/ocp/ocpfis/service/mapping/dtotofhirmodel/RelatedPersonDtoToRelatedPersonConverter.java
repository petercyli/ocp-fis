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

        //telecom
        ContactPoint contactPoint = new ContactPoint();
        if (relatedPersonDto.getTelecomCode() != null) {
            contactPoint.setSystem(ContactPoint.ContactPointSystem.valueOf(relatedPersonDto.getTelecomCode().toUpperCase()));
        }
        if (relatedPersonDto.getTelecomUse() != null) {
            contactPoint.setUse(ContactPoint.ContactPointUse.valueOf(relatedPersonDto.getTelecomUse().toUpperCase()));
        }
        contactPoint.setValue(relatedPersonDto.getTelecomValue());
        relatedPerson.setTelecom(Collections.singletonList(contactPoint));

        //gender
        Enumerations.AdministrativeGender gender = FhirUtil.getPatientGender(relatedPersonDto.getGenderCode());
        relatedPerson.setGender(gender);

        //birthdate
        relatedPerson.setBirthDate(DateUtil.convertStringToDate(relatedPersonDto.getBirthDate()));

        //address
        Address address = new Address();
        address.addLine(relatedPersonDto.getAddress1());
        address.addLine(relatedPersonDto.getAddress2());
        address.setCity(relatedPersonDto.getCity());
        address.setState(relatedPersonDto.getState());
        address.setPostalCode(relatedPersonDto.getZip());
        address.setCountry(relatedPersonDto.getCountry());
        relatedPerson.setAddress(Collections.singletonList(address));

        //period
        Period period = new Period();
        period.setStart(DateUtil.convertStringToDate(relatedPersonDto.getStartDate()));
        period.setEnd(DateUtil.convertStringToDate(relatedPersonDto.getEndDate()));
        relatedPerson.setPeriod(period);

        return relatedPerson;
    }
}
