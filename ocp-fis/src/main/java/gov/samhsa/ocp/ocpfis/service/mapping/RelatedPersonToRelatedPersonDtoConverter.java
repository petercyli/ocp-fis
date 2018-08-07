package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.RelatedPersonDto;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
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
            relatedPersonDto.setRelationshipSystem(relationshipCoding.getSystem());
        }

        //name
        if (relatedPerson.getName() != null && relatedPerson.getName().get(0) != null) {
            relatedPersonDto.setFirstName(relatedPerson.getName().get(0).getGiven().stream().findFirst().orElse(new StringType("")).toString());
            relatedPersonDto.setLastName(relatedPerson.getName().get(0).getFamily());
        }

        //telecom
        if (relatedPerson.hasTelecom()){
            relatedPersonDto.setTelecoms(FhirDtoUtil.convertTelecomListToTelecomDtoList(relatedPerson.getTelecom()));
        }

        if (relatedPerson.hasAddress()){
            relatedPersonDto.setAddresses(FhirDtoUtil.convertAddressListToAddressDtoList(relatedPerson.getAddress()));
        }

        //gender
        if (relatedPerson.getGender() != null) {
            relatedPersonDto.setGenderCode(relatedPerson.getGender().toCode());
            relatedPersonDto.setGenderValue(relatedPerson.getGender().getDisplay());
        }

        //birthdate
        relatedPersonDto.setBirthDate(DateUtil.convertDateToString(relatedPerson.getBirthDate()));

        //period
        Period period = relatedPerson.getPeriod();
        if (period != null) {
            relatedPersonDto.setStartDate(DateUtil.convertDateToString(period.getStart()));
            relatedPersonDto.setEndDate(DateUtil.convertDateToString(period.getEnd()));
        }

        return relatedPersonDto;
    }


}
