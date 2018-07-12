package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.samhsa.ocp.ocpfis.service.dto.structuredefinition.Contact;
import gov.samhsa.ocp.ocpfis.service.dto.structuredefinition.Jurisdiction;
import gov.samhsa.ocp.ocpfis.service.dto.structuredefinition.Mapping;
import gov.samhsa.ocp.ocpfis.service.dto.structuredefinition.StructureDefinitionDto;
import gov.samhsa.ocp.ocpfis.service.dto.structuredefinition.Telecom;
import gov.samhsa.ocp.ocpfis.util.FhirUtil;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ContactDetail;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.Narrative;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
public class StructureDefinitionServiceImpl implements StructureDefinitionService {

    @Autowired
    private IGenericClient fhirClient;

    @Override
    public void createStructureDefinition(StructureDefinitionDto structureDefinitionDto) {
        try{
            log.info(structureDefinitionDto.getId());
            StructureDefinition structureDefinition=map(structureDefinitionDto);
            FhirUtil.updateFhirResource(fhirClient,structureDefinition, ResourceType.StructureDefinition.name());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private StructureDefinition map(StructureDefinitionDto dto) throws Exception{
        StructureDefinition structureDefinition=new StructureDefinition();

        //id
        structureDefinition.setId(dto.getId());

        //Text
        Narrative narrative = new Narrative();
        Narrative.NarrativeStatus status = Narrative.NarrativeStatus.fromCode(dto.getText().getStatus());

        narrative.setStatus(status);
        XhtmlNode xhtmlNode = new XhtmlNode();
        xhtmlNode.setValue(dto.getText().getDiv());
        narrative.setDiv(xhtmlNode);
        structureDefinition.setText(narrative);

        //url
        structureDefinition.setUrl(dto.getUrl());

        //version
        structureDefinition.setVersion(dto.getVersion());

        //name
        structureDefinition.setName(dto.getName());

        //status
        Enumerations.PublicationStatus publicationStatus = Enumerations.PublicationStatus.fromCode(dto.getStatus());
        structureDefinition.setStatus(publicationStatus);

        //experimental
        structureDefinition.setExperimental(Boolean.valueOf(dto.getStatus()));

        //date
        structureDefinition.setDate(new Date());

        //publisher
        structureDefinition.setPublisher(dto.getPublisher());

        //derivation
        structureDefinition.setDerivation(StructureDefinition.TypeDerivationRule.fromCode(dto.getDerivation()));

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
                    contactDetail.setTelecom(Arrays.asList(contactPoint));
                    structureDefinition.setContact(Arrays.asList(contactDetail));
                }
            }
        }

        //description
        structureDefinition.setDescription(dto.getDescription());

        //jurisdiction
        Optional<Jurisdiction> oJurisdiction = dto.getJurisdiction().stream().findFirst();

        if (oJurisdiction.isPresent()) {
            Jurisdiction jurisdiction = oJurisdiction.get();

            Optional<gov.samhsa.ocp.ocpfis.service.dto.structuredefinition.Coding> oCoding = jurisdiction.getCoding().stream().findFirst();

            if (oCoding.isPresent()) {
                CodeableConcept codeableConcept = new CodeableConcept();
                Coding coding = new Coding();
                coding.setSystem(oCoding.get().getSystem());
                coding.setCode(oCoding.get().getCode());
                codeableConcept.setCoding(Arrays.asList(coding));
                structureDefinition.setJurisdiction(Arrays.asList(codeableConcept));
            }
        }

        structureDefinition.setMapping(mapsToStructureDefinitionListMapping(dto.getMapping()));


        return structureDefinition;
    }

    private List<StructureDefinition.StructureDefinitionMappingComponent> mapsToStructureDefinitionListMapping(List<Mapping> maps){
        return maps.stream().map(m->mapToSD(m)).collect(toList());
    }

    private StructureDefinition.StructureDefinitionMappingComponent mapToSD(Mapping map){
        StructureDefinition.StructureDefinitionMappingComponent sd=new StructureDefinition.StructureDefinitionMappingComponent();
        sd.setIdentity(map.getIdentity());
        sd.setUri(map.getUri());
        sd.setName(map.getName());
        return sd;
    }
}
