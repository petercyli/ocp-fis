package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.samhsa.ocp.ocpfis.service.dto.valueset.Concept;
import gov.samhsa.ocp.ocpfis.service.dto.valueset.Contact;
import gov.samhsa.ocp.ocpfis.service.dto.valueset.Include;
import gov.samhsa.ocp.ocpfis.service.dto.valueset.Jurisdiction;
import gov.samhsa.ocp.ocpfis.service.dto.valueset.Telecom;
import gov.samhsa.ocp.ocpfis.service.dto.valueset.ValueSetDto;
import gov.samhsa.ocp.ocpfis.util.FhirOperationUtil;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ContactDetail;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.Narrative;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
public class ValueSetServiceImpl implements ValueSetService {

    private final IGenericClient fhirClient;

    @Autowired
    public ValueSetServiceImpl(IGenericClient fhirClient) {
        this.fhirClient = fhirClient;
    }

    /**
     * This should only be called by the data loader to load the values.
     *
     */
    @Override
    public void createValueSet(ValueSetDto valueSetDto) {
        try {
            log.info(valueSetDto.getId());
            ValueSet valueSet = map(valueSetDto);
            //Intentionally using update method to force the ID coming in from the DTO
            FhirOperationUtil.updateFhirResource(fhirClient, valueSet, ResourceType.ValueSet.name());

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    private ValueSet map(ValueSetDto dto) throws Exception {
        ValueSet valueSet = new ValueSet();

        //id
        valueSet.setId(dto.getId());

        //text
        Narrative narrative = new Narrative();
        Narrative.NarrativeStatus status = Narrative.NarrativeStatus.fromCode(dto.getText().getStatus());

        narrative.setStatus(status);
        XhtmlNode xhtmlNode = new XhtmlNode();
        xhtmlNode.setValue(dto.getText().getDiv());
        narrative.setDiv(xhtmlNode);
        valueSet.setText(narrative);

        //url
        valueSet.setUrl(dto.getUrl());

        //version
        valueSet.setVersion(dto.getVersion());

        //name
        valueSet.setName(dto.getName());

        //status
        Enumerations.PublicationStatus publicationStatus = Enumerations.PublicationStatus.fromCode(dto.getStatus());
        valueSet.setStatus(publicationStatus);

        //experimental
        valueSet.setExperimental(Boolean.valueOf(dto.getStatus()));

        //date
        //valueSet.setDate(DateUtil.convertStringToDate(dto.getDate()));
        valueSet.setDate(new Date());

        //publisher
        valueSet.setPublisher(dto.getDate());

        //contact
        if (dto.getContact() != null) {
            Optional<Contact> oContact = dto.getContact().stream().findFirst();
            if (oContact.isPresent()) {
                Contact contactDto = oContact.get();
                Optional<Telecom> oTelecom = contactDto.getTelecom().stream().findFirst();

                if (oTelecom.isPresent()) {

                    Telecom telecomDto = oTelecom.get();
                    ContactDetail contactDetail = new ContactDetail();
                    ContactPoint contactPoint = new ContactPoint();
                    ContactPoint.ContactPointSystem contactPointSystem = ContactPoint.ContactPointSystem.fromCode(telecomDto.getSystem());
                    contactPoint.setSystem(contactPointSystem);
                    contactPoint.setValue(telecomDto.getValue());
                    contactDetail.setTelecom(Collections.singletonList(contactPoint));
                    valueSet.setContact(Collections.singletonList(contactDetail));
                }
            }
        }

        //description
        valueSet.setDescription(dto.getDescription());

        //jurisdiction
        Optional<Jurisdiction> oJurisdiction = dto.getJurisdiction().stream().findFirst();

        if (oJurisdiction.isPresent()) {
            Jurisdiction jurisdiction = oJurisdiction.get();

            Optional<gov.samhsa.ocp.ocpfis.service.dto.valueset.Coding> oCoding = jurisdiction.getCoding().stream().findFirst();

            if (oCoding.isPresent()) {
                CodeableConcept codeableConcept = new CodeableConcept();
                Coding coding = new Coding();
                coding.setSystem(oCoding.get().getSystem());
                coding.setCode(oCoding.get().getCode());
                codeableConcept.setCoding(Collections.singletonList(coding));
                valueSet.setJurisdiction(Collections.singletonList(codeableConcept));
            }
        }

        //compose.include
        List<Include> includeDtos = dto.getCompose().getInclude();

        Optional<Include> oIncludeDto = includeDtos.stream().findFirst();

        if (oIncludeDto.isPresent()) {
            Include includeDto = oIncludeDto.get();

            String system = includeDto.getSystem();
            List<Concept> conceptDtos = includeDto.getConcept();

            List<ValueSet.ConceptReferenceComponent> concepts = conceptDtos.stream().map(x -> {
                ValueSet.ConceptReferenceComponent concept = new ValueSet.ConceptReferenceComponent();
                concept.setCode(x.getCode());
                concept.setDisplay(x.getDisplay());
                return concept;
            }).collect(toList());

            //set the values in the destination
            ValueSet.ValueSetComposeComponent include = valueSet.getCompose();
            List<ValueSet.ConceptSetComponent> includeItems = include.getInclude();

            ValueSet.ConceptSetComponent conceptSetComponent = new ValueSet.ConceptSetComponent();
            conceptSetComponent.setSystem(system);
            conceptSetComponent.setConcept(concepts);
            includeItems.add(conceptSetComponent);
        }

        return valueSet;
    }

}
