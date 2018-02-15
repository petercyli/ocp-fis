package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.RelatedPersonDto;
import gov.samhsa.ocp.ocpfis.util.FhirUtils;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.RelatedPerson;
import org.hl7.fhir.dstu3.model.StringType;

import java.util.List;
import java.util.Optional;

public class RelatedPersonToRelatedPersonDtoConverter {

    public static RelatedPersonDto map(RelatedPerson relatedPerson) {

        RelatedPersonDto relatedPersonDto = new RelatedPersonDto();

        //id
        relatedPersonDto.setRelatedPersonId(relatedPerson.getIdElement().getIdPart());

        //identifier
        Optional<Identifier> identifier = relatedPerson.getIdentifier().stream().findFirst();
        if (identifier.isPresent()) {
            relatedPersonDto.setIdentifierType(identifier.get().getSystem());
            relatedPersonDto.setIdentifierValue(identifier.get().getValue());
        }

        //active
        relatedPersonDto.setActive(relatedPerson.getActive());

        //patient
        relatedPersonDto.setPatient(relatedPerson.getPatient().getReference().replace("Patient/", ""));

        //relationship
        CodeableConcept relationshipCodeableConcept = relatedPerson.getRelationship();
        List<Coding> relationshipsCodings = relationshipCodeableConcept.getCoding();
        Coding relationshipCoding = relationshipsCodings.stream().findFirst().orElse(null);
        if (relationshipCoding != null) {
            relatedPersonDto.setRelationshipCode(relationshipCoding.getCode());
            relatedPersonDto.setRelationshipValue(relationshipCoding.getDisplay());
        }

        //name
        if (relatedPerson.getName() != null && relatedPerson.getName().get(0) != null) {
            relatedPersonDto.setFirstName(relatedPerson.getName().get(0).getGiven().stream().findFirst().orElse(new StringType("")).toString());
            relatedPersonDto.setLastName(relatedPerson.getName().get(0).getFamily());
        }

        //telecom
        Optional<ContactPoint> contactPoint = relatedPerson.getTelecom().stream().findFirst();
        if (contactPoint.isPresent()) {
            if (contactPoint.get().getSystem() != null) {
                relatedPersonDto.setTelecomCode(contactPoint.get().getSystem().toCode());
            }
            if (contactPoint.get().getUse() != null) {
                relatedPersonDto.setTelecomUse(contactPoint.get().getUse().toCode());
            }
            relatedPersonDto.setTelecomValue(contactPoint.get().getValue());
        }

        //gender
        if (relatedPerson.getGender() != null) {
            relatedPersonDto.setGenderCode(relatedPerson.getGender().toCode());
            relatedPersonDto.setGenderValue(relatedPerson.getGender().getDisplay());
        }

        //birthdate
        relatedPersonDto.setBirthDate(FhirUtils.convertToString(relatedPerson.getBirthDate()));

        //address
        List<Address> addresses = relatedPerson.getAddress();
        Optional<Address> address = addresses.stream().findFirst();

        if (address.isPresent()) {
            relatedPersonDto.setAddress1(address.get().getLine().size() > 0 ? address.get().getLine().get(0).toString() : "");
            relatedPersonDto.setAddress2(address.get().getLine().size() > 1 ? address.get().getLine().get(1).toString() : "");
            relatedPersonDto.setCity(address.get().getCity());
            relatedPersonDto.setState(address.get().getState());
            relatedPersonDto.setZip(address.get().getPostalCode());
            relatedPersonDto.setCountry(address.get().getCountry());
        }

        //period
        Period period = relatedPerson.getPeriod();
        if (period != null) {
            relatedPersonDto.setStartDate(FhirUtils.convertToString(period.getStart()));
            relatedPersonDto.setEndDate(FhirUtils.convertToString(period.getEnd()));
        }

        return relatedPersonDto;
    }


}
