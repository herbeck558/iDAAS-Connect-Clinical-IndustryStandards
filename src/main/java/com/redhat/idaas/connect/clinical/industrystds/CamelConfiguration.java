/*
 * Copyright 2019 Red Hat, Inc.
 * <p>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */
package com.redhat.idaas.connect.clinical.industrystds;

import ca.uhn.fhir.store.IAuditDataStore;
import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.hl7.HL7;
import org.apache.camel.component.hl7.HL7MLLPNettyDecoderFactory;
import org.apache.camel.component.hl7.HL7MLLPNettyEncoderFactory;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.kafka.KafkaEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
//import org.springframework.jms.connection.JmsTransactionManager;
//import javax.jms.ConnectionFactory;
import org.springframework.stereotype.Component;
import sun.util.calendar.BaseCalendar;

import java.time.LocalDate;

@Component
public class CamelConfiguration extends RouteBuilder {
  private static final Logger log = LoggerFactory.getLogger(CamelConfiguration.class);

  @Bean
  private HL7MLLPNettyEncoderFactory hl7Encoder() {
    HL7MLLPNettyEncoderFactory encoder = new HL7MLLPNettyEncoderFactory();
    encoder.setCharset("iso-8859-1");
    //encoder.setConvertLFtoCR(true);
    return encoder;
  }
  @Bean
  private HL7MLLPNettyDecoderFactory hl7Decoder() {
    HL7MLLPNettyDecoderFactory decoder = new HL7MLLPNettyDecoderFactory();
    decoder.setCharset("iso-8859-1");
    return decoder;
  }
  @Bean
  private KafkaEndpoint kafkaEndpoint(){
    KafkaEndpoint kafkaEndpoint = new KafkaEndpoint();
    return kafkaEndpoint;
  }
  @Bean
  private KafkaComponent kafkaComponent(KafkaEndpoint kafkaEndpoint){
    KafkaComponent kafka = new KafkaComponent();
    return kafka;
  }


  /*
   * Kafka implementation based upon https://camel.apache.org/components/latest/kafka-component.html
   *
   */
  @Override
  public void configure() throws Exception {

    /*
     * Audit
     *
     * Direct component within platform to ensure we can centralize logic
     * There are some values we will need to set within every route
     * We are doing this to ensure we dont need to build a series of beans
     * and we keep the processing as lightweight as possible
     *
     */
    from("direct:auditing")
        // look at simple for expressions of exchange properties
        // .setHeader("source").simple("Value")
        //.setHeader("source").simple("{$body}")
        .setHeader("messageprocesseddate").simple("${date:now:yyyy-MM-dd}")
        .setHeader("messageprocessedtime").simple("${date:now:HH:mm:ss:SSS}")
        .setHeader("processingtype").exchangeProperty("processingtype")
        .setHeader("industrystd").exchangeProperty("industrystd")
        .setHeader("component").exchangeProperty("component")
        .setHeader("messagetrigger").exchangeProperty("messagetrigger")
        .setHeader("processname").exchangeProperty("processname")
        .setHeader("auditdetails").exchangeProperty("auditdetails")
        .to("kafka:opsMgmt_PlatformTransactions?brokers=localhost:9092")
    ;
    /*
    *  Logging
    */
    from("direct:logging")
      //.log(LoggingLevel.INFO, log, "HL7 Admissions Message: [${body}]")
    ;

    /*
	 *  HL7 v2x Server Implementations
	 *  ------------------------------
	 *  HL7 implementation based upon https://camel.apache.org/components/latest/dataformats/hl7-dataformat.html
	 *  Below is an example of how to leverage the test data without needing external HL7 message data l
	 *  from("file:src/data-in/hl7v2/adt?delete=true?noop=true")
	 */

    /*
    *   Simple language reference
    *   https://camel.apache.org/components/latest/languages/simple-language.html
    */
	  // ADT
	  from("netty4:tcp://0.0.0.0:10001?sync=true&decoder=#hl7Decoder&encoder=#hl7Encoder")
          .routeId("hl7Admissions")
          //Logging
          //.to("direct:logging")
          // set Auditing Properties
          // ${date:now:dd-MM-yyyy HH:mm}
          .setProperty("processingtype").constant("data")
          .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
          .setProperty("industrystd").constant("HL7")
          .setProperty("messagetrigger").constant("ADT")
          .setProperty("componentname").simple("${routeId}")
          .setProperty("processname").constant("Input")
          .setProperty("auditdetails").constant("ADT message received")
          // iDAAS DataHub Processing
          .wireTap("direct:auditing")
          // Send to Topic
          .to("kafka:MCTN_MMS_ADT?brokers=localhost:9092")
          //Response to HL7 Message Sent Built by platform
          .transform(HL7.ack())
          // This would enable persistence of the ACK
    ;

    // ORM
    from("netty4:tcp://0.0.0.0:10002?sync=true&decoder=#hl7Decoder&encoder=#hl7Encoder")
        .routeId("hl7Orders")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("HL7")
        .setProperty("messagetrigger").constant("ORM")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("ORM message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send to Topic
        .to("kafka:MCTN_MMS_ORM?brokers=localhost:9092")
        //Response to HL7 Message Sent Built by platform
        .transform(HL7.ack())
        // This would enable persistence of the ACK
    ;

    // ORU
    from("netty4:tcp://0.0.0.0:10003?sync=true&decoder=#hl7Decoder&encoder=#hl7Encoder")
        .routeId("hl7Results")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("HL7")
        .setProperty("messagetrigger").constant("ORU")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("ORU message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send to Topic
        .to("kafka:MCTN_MMS_ORU?brokers=localhost:9092")
        //Response to HL7 Message Sent Built by platform
        .transform(HL7.ack())
        // This would enable persistence of the ACK
    ;

    // RDE
    from("netty4:tcp://0.0.0.0:10004?sync=true&decoder=#hl7Decoder&encoder=#hl7Encoder")
        .routeId("hl7Pharmacy")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("HL7")
        .setProperty("messagetrigger").constant("RDE")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("RDE message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send to Topic
        .to("kafka:MCTN_MMS_RDE?brokers=localhost:9092")
        //Response to HL7 Message Sent Built by platform
        .transform(HL7.ack())
        // This would enable persistence of the ACK
    ;

    // MFN
    from("netty4:tcp://0.0.0.0:10005?sync=true&decoder=#hl7Decoder&encoder=#hl7Encoder")
        .routeId("hl7MasterFiles")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("HL7")
        .setProperty("messagetrigger").constant("MFN")
        .setProperty("component").simple("{$routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("MFN message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send to Topic
        .to("kafka:MCTN_MMS_MFN?brokers=localhost:9092")
        //Response to HL7 Message Sent Built by platform
        .transform(HL7.ack())
        // This would enable persistence of the ACK
    ;

    // MDM
    from("netty4:tcp://0.0.0.0:10006?sync=true&decoder=#hl7Decoder&encoder=#hl7Encoder")
         .routeId("hl7MasterDocs")
         // set Auditing Properties
         .setProperty("processingtype").constant("data")
         .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
         .setProperty("industrystd").constant("HL7")
         .setProperty("messagetrigger").constant("MDM")
         .setProperty("component").simple("${routeId}")
         .setProperty("processname").constant("Input")
         .setProperty("auditdetails").constant("MDM message received")
         // iDAAS DataHub Processing
         .wireTap("direct:auditing")
         //Send To Topic
         .to("kafka:MCTN_MMS_MDM?brokers=localhost:9092")
         //Response to HL7 Message Sent Built by platform
         .transform(HL7.ack())
        // This would enable persistence of the ACK
    ;

    // SCH
    from("netty4:tcp://0.0.0.0:10007?sync=true&decoder=#hl7Decoder&encoder=#hl7Encoder")
        .routeId("hl7Schedule")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("HL7")
        .setProperty("messagetrigger").constant("SCH")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("SCH message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:MCTN_MMS_SCH?brokers=localhost:9092")
        //Response to HL7 Message Sent Built by platform
        .transform(HL7.ack())
        // This would enable persistence of the ACK
    ;

    // VXU
    from("netty4:tcp://0.0.0.0:10008?sync=true&decoder=#hl7Decoder&encoder=#hl7Encoder")
        .routeId("hl7Vaccination")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("HL7")
        .setProperty("messagetrigger").constant("VXU")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("VXU message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:MCTN_MMS_VXU?brokers=localhost:9092")
        //Response to HL7 Message Sent Built by platform
        .transform(HL7.ack())
        // This would enable persistence of the ACK
    ;

    /*
     *  FHIR
     *  ----
     * these will be accessible within the integration when started the default is
     * <hostname>:8080/idaas/<resource>
     * FHIR Resources:
     *  CodeSystem,DiagnosticResult,Encounter,EpisodeOfCare,Immunization,MedicationRequest
     *  MedicationAdminstration,Observation,Order,Patient,Procedure,Schedule
     */

    from("servlet://adverseevent")
        .routeId("FHIRAdverseEvent")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("AdverseEvent")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("Adverse Event message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_AdverseEvent?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/AdverseEvent")
        // Process Response
    ;
    from("servlet://careplan")
        .routeId("FHIRCodeSystem")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("CarePlan")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("CarePlan message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_CarePlan?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/CarePlan")
        // Process Response
    ;
    from("servlet://codesystem")
        .routeId("FHIRCodeSystem")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("CodeSystem")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("CodeSystem message received")
      // iDAAS DataHub Processing
      .wireTap("direct:auditing")
      // Send To Topic
      .to("kafka:IntgrtnFHIRSvr_CodeSystem?brokers=localhost:9092")
      // Invoke External FHIR Server
      .to("https://localhost:9443/fhir-server/api/v4/CodeSystem")
      // Process Response
     ;
    from("servlet://clincialimpression")
        .routeId("FHIRCodeSystem")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("ClinicalImpression")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("ClinicalImpression message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_ClinicalImpression?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/ClinicalImpression")
        // Process Response
    ;
    from("servlet://communication")
        .routeId("FHIRCommunication")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("Communication")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("Communication message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_Communication?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/Communication")
        // Process Response
    ;
    from("servlet://condition")
        .routeId("FHIRCondition")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("Condition")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("Condition message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_Condition?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/Condition")
        // Process Response
    ;
    from("servlet://devicerequest")
        .routeId("FHIRDeviceRequest")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("DeviceRequest")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("Device Request message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_DeviceRequest?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/DeviceRequest")
        // Process Response
    ;
    from("servlet://deviceusestatement")
        .routeId("FHIRDeviceUseStatement")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("DeviceUseStatement")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("Device Use Statement message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_DeviceUseStatement?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/DeviceUseStatement")
        // Process Response
    ;
    from("servlet://diagnosticresult")
        .routeId("FHIRDiagnosticResult")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("DiagnosticResult")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("DiagnosticResult message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_DiagnosticResult?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/DiagnosticResult")
        //Process Response
    ;
    from("servlet://encounter")
        .routeId("FHIREncounter")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("Encounter")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("Encounter message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_Encounter?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/Encounter")
        //Process Response
    ;
    from("servlet://episodeofcare")
        .routeId("FHIREpisodeOfCare")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("EpisodeOfCare")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("EpisodeOfCare message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_EpisodeOfCare?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/EpisodeOfCare")
        //Process Response
    ;
    from("servlet://goal")
        .routeId("FHIRGoal")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("Goal")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("Goal message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_Goal?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/Goal")
        //Process Response
    ;
    from("servlet://imagingstudy")
        .routeId("FHIRImagingStudy")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("ImagingStudy")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("Imaging Study message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_ImagingStudy?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/ImagingStudy")
        //Process Response
    ;
    from("servlet://medicationdispense")
            .routeId("FHIRMedicationDispense")
            // set Auditing Properties
            .setProperty("processingtype").constant("data")
            .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
            .setProperty("industrystd").constant("FHIR")
            .setProperty("messagetrigger").constant("MedicationDispense")
            .setProperty("component").simple("${routeId}")
            .setProperty("processname").constant("Input")
            .setProperty("auditdetails").constant("Medication Dispense message received")
            // iDAAS DataHub Processing
            .wireTap("direct:auditing")
            // Send To Topic
            .to("kafka:IntgrtnFHIRSvr_MedicationDispense?brokers=localhost:9092")
            // Invoke External FHIR Server
            .to("https://localhost:9443/fhir-server/api/v4/MedicationDispense")
    //Process Response
    ;
    from("servlet://medicationrequest")
        .routeId("FHIRMedicationRequest")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("MedicationRequest")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("Medication Request message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_MedicationRequest?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/MedicationRequest")
        //Process Response
    ;
    from("servlet://medicationadministration")
        .routeId("FHIRMedicationAdministration")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("MedicationAdminstration")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("Medication Statement message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_MedicationAdminstration?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/MedicationAdminstration")
        //Process Response
    ;

    from("servlet://observation")
        .routeId("FHIRObservation")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("Observation")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("Observation message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_Observation?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/Observation")
        //Process Response
    ;

    from("servlet://order")
        .routeId("FHIROrder")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("Order")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("Order message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_Observation?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/Order")
    //Process Response
    ;

    from("servlet://patient")
        .routeId("FHIRPatient")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("Patient")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("Patient message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_Observation?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/Patient")
        //Process Response
    ;

    from("servlet://procedure")
        .routeId("FHIRProcedure")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("Procedure")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("Procedure message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_Observation?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/Procedure")
        //Process Response
    ;
    from("servlet://questionaire")
        .routeId("FHIRQuestionaire")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("Questionaire")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("Questionaire message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_Questionaire?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/Questionaire")
        //Process Response
    ;
    from("servlet://questionaireresponse")
        .routeId("FHIRQuestionaireResponse")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("QuestionaireResponse")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("Questionaire Response message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_QuestionaireResponse?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/QuestionaireResponse")
        //Process Response
    ;
    from("servlet://schedule")
        .routeId("FHIRSchedule")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("Schedule")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("Schedule message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_Observation?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/Schedule")
        //Process Response
    ;
    from("servlet://specimen")
        .routeId("FHIRSpecimen")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("Specimen")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        .setProperty("auditdetails").constant("Specimen message received")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_Specimen?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/Specimen")
        //Process Response
    ;
    /*
     *   Middle Tier
     *   Move Transactions and enable the Clinical Data Enterprise Integration Pattern
     *   HL7 v2
     *   1. from Sending App By Facility
     *   2. to Sending App By Message Type
     *   3. to Facility By Message Type
     *   4. to Enterprise by Message Type
     *   FHIR
     *   1. to Enterprise by Message Type
     */

    /*
     *    HL7v2 ADT
     */
    from("kafka: MCTN_MMS_ADT?brokers=localhost:9092")
       .routeId("ADT-MiddleTier")
       // Auditing
       .setProperty("processingtype").constant("data")
       .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
       .setProperty("industrystd").constant("HL7")
       .setProperty("messagetrigger").constant("ADT")
       .setProperty("component").simple("${routeId}")
       .setProperty("processname").constant("MTier")
       .setProperty("auditdetails").constant("ADT to Enterprise By Sending App By Data Type middle tier")
       .wireTap("direct:auditing")
       // Enterprise Message By Sending App By Type
       .to("kafka:MMS_ADT?brokers=localhost:9092")
       // Auditing
       .setProperty("processingtype").constant("data")
       .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
       .setProperty("industrystd").constant("HL7")
       .setProperty("messagetrigger").constant("ADT")
       .setProperty("component").simple("${routeId}")
       .setProperty("processname").constant("MTier")
       .setProperty("auditdetails").constant("ADT to Facility By Sending App By Data Type middle tier")
       .wireTap("direct:auditing")
       // Facility By Type
       .to("kafka:MCTN_ADT?brokers=localhost:9092")
       // Auditing
       .setProperty("processingtype").constant("data")
       .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
       .setProperty("industrystd").constant("HL7")
       .setProperty("messagetrigger").constant("ADT")
       .setProperty("component").simple("${routeId}")
       .setProperty("processname").constant("MTier")
       .setProperty("auditdetails").constant("ADT to Enterprise By Sending App By Data Type middle tier")
       .wireTap("direct:auditing")
       // Enterprise Message By Type
       .to("kafka:Ent_ADT?brokers=localhost:9092")
    ;

    /*
     *   HL7v2 ORM
     */
    from("kafka: MCTN_MMS_ORM?brokers=localhost:9092")
        .routeId("ORM-MiddleTier")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("HL7")
        .setProperty("messagetrigger").constant("ORM")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("ORM to Enterprise Sending App By Data Type middle tier")
        .wireTap("direct:auditing")
        // Enterprise Message By Sending App By Type
        .to("kafka:MMS_ORM?brokers=localhost:9092")
         // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("HL7")
        .setProperty("messagetrigger").constant("ORM")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("ORM to Facility By Data Type middle tier")
        .wireTap("direct:auditing")
        // Facility By Type
        .to("kafka:MCTN_ORM?brokers=localhost:9092")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("HL7")
        .setProperty("messagetrigger").constant("ORM")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("ADT to Enterprise By Data Type middle tier")
        .wireTap("direct:auditing")
        // Enterprise Message By Type
        .to("kafka:Ent_ORM?brokers=localhost:9092")
    ;

    /*
     *   HL7v2 ORU
     */
    from("kafka: MCTN_MMS_ORU?brokers=localhost:9092")
        .routeId("ORU-MiddleTier")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("HL7")
        .setProperty("messagetrigger").constant("ORU")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("ORU to Enterprise Sending App By Data Type middle tier")
        .wireTap("direct:auditing")
        // Enterprise Message By Sending App By Type
        .to("kafka:MMS_ORU?brokers=localhost:9092")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("HL7")
        .setProperty("messagetrigger").constant("ORU")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("ORU Facility By Data Type middle tier")
        .wireTap("direct:auditing")
        // Facility By Type
        .to("kafka:MCTN_ORU?brokers=localhost:9092")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("HL7")
        .setProperty("messagetrigger").constant("ORU")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("ORU to Enterprise By Data Type middle tier")
        .wireTap("direct:auditing")
        // Entrprise Message By Type
        .to("kafka:Ent_ORU?brokers=localhost:9092")
    ;
    /*
     *   HL7v2 SCH
     */
    from("kafka: MCTN_MMS_SCH?brokers=localhost:9092")
        .routeId("SCH-MiddleTier")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("HL7")
        .setProperty("messagetrigger").constant("SCH")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("SCH to Sending App By Data Type middle tier")
        .wireTap("direct:auditing")
        // Enterprise Message By Sending App By Type
        .to("kafka:MMS_SCH?brokers=localhost:9092")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("HL7")
        .setProperty("messagetrigger").constant("SCH")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("SCH Facility By Data Type middle tier")
        .wireTap("direct:auditing")
        // Facility By Type
        .to("kafka:MCTN_SCH?brokers=localhost:9092")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("HL7")
        .setProperty("messagetrigger").constant("SCH")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("SCH to Enterprise By Data Type middle tier")
        .wireTap("direct:auditing")
        // Entrprise Message By Type
        .to("kafka:Ent_SCH?brokers=localhost:9092")
    ;
    /*
     *   HL7v2 RDE
     */
    from("kafka: MCTN_MMS_RDE?brokers=localhost:9092")
        .routeId("RDE-MiddleTier")
         // Auditing
         .setProperty("processingtype").constant("data")
         .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
         .setProperty("industrystd").constant("HL7")
         .setProperty("messagetrigger").constant("RDE")
         .setProperty("component").simple("${routeId}")
         .setProperty("processname").constant("MTier")
         .setProperty("auditdetails").constant("RDE Sending App By Data Type middle tier")
         .wireTap("direct:auditing")
         // Enterprise Message By Sending App By Type
         .to("kafka:MMS_RDE?brokers=localhost:9092")
         // Auditing
         .setProperty("processingtype").constant("data")
         .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
         .setProperty("industrystd").constant("HL7")
         .setProperty("messagetrigger").constant("RDE")
         .setProperty("component").simple("${routeId}")
         .setProperty("processname").constant("MTier")
         .setProperty("auditdetails").constant("RDE Facility By Data Type middle tier")
         .wireTap("direct:auditing")
         // Facility By Type
         .to("kafka:MCTN_RDE?brokers=localhost:9092")
         // Auditing
         .setProperty("processingtype").constant("data")
         .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
         .setProperty("industrystd").constant("HL7")
         .setProperty("messagetrigger").constant("RDE")
         .setProperty("component").simple("${routeId}")
         .setProperty("processname").constant("MTier")
         .setProperty("auditdetails").constant("RDE Enterprise By Data Type middle tier")
         .wireTap("direct:auditing")
         // Entrprise Message By Type
        .to("kafka:Ent_RDE?brokers=localhost:9092")
    ;
    /*
     *   HL7v2 MDM
     */
    from("kafka: MCTN_MMS_MDM?brokers=localhost:9092")
        .routeId("MDM-MiddleTier")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("HL7")
        .setProperty("messagetrigger").constant("MDM")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("MDM Sending App By Data Type middle tier")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Enterprise Message By Sending App By Type
        .to("kafka:MMS_MDM?brokers=localhost:9092")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("HL7")
        .setProperty("messagetrigger").constant("MDM")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("MDM Facility By Data Type middle tier")
        .wireTap("direct:auditing")
        // Facility By Type
        .to("kafka:MCTN_MDM?brokers=localhost:9092")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("HL7")
        .setProperty("messagetrigger").constant("MDM")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("MDM to Enterprise By Data Type middle tier")
        .wireTap("direct:auditing")
        // Entrprise Message By Type
        .to("kafka:Ent_MDM?brokers=localhost:9092")
    ;
    /*
     *   HL7v2 MFN
     */
    from("kafka: MCTN_MMS_MFN?brokers=localhost:9092")
        .routeId("MFN-MiddleTier")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("HL7")
        .setProperty("messagetrigger").constant("MFN")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("MFN Sending App By Data Type middle tier")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Enterprise Message By Sending App By Type
        .to("kafka:MMS_MFN?brokers=localhost:9092")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("HL7")
        .setProperty("messagetrigger").constant("MFN")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("MFN Facility By Data Type middle tier")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Facility By Type
        .to("kafka:MCTN_ORM?brokers=localhost:9092")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("HL7")
        .setProperty("messagetrigger").constant("MFN")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("ADT to Enterprise By Data Type middle tier")
        .wireTap("direct:auditing")
        // Entrprise Message By Type
        .to("kafka:Ent_MFN?brokers=localhost:9092")
    ;
    /*
     *   HL7v2 VXU
     */
    from("kafka: MCTN_MMS_VXU?brokers=localhost:9092")
        .routeId("VXU-MiddleTier")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("HL7")
        .setProperty("messagetrigger").constant("VXU")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("VXU Sending App By Data Type middle tier")
        .wireTap("direct:auditing")
        // Enterprise Message By Sending App By Type
        .to("kafka:MMS_VXU?brokers=localhost:9092")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("HL7")
        .setProperty("messagetrigger").constant("VXU")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("VXU By Sending App By Data Type middle tier")
        .wireTap("direct:auditing")
        // Facility By Type
        .to("kafka:MCTN_ORM?brokers=localhost:9092")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("HL7")
        .setProperty("messagetrigger").constant("VXU")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("VXU Enterprise By Data Type middle tier")
        .wireTap("direct:auditing")
        // Entrprise Message By Type
        .to("kafka:Ent_VXU?brokers=localhost:9092")
        //.to("kafka:opsMgmt_ProcessedTransactions?brokers={{kafkasettings.hostvalue}}:{{kafkasettings.portnumber}}")
    ;
    /*
     *   FHIR IntgrtnFHIRSvr_CodeSystem
     */
    from("kafka:IntgrtnFHIRSvr_AdverseEvent?brokers=localhost:9092")
        .routeId("AdverseEvent-MiddleTier")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("AdverseEvent")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("Adverse Event to Enterprise By Data Type middle tier")
        .wireTap("direct:auditing")
        // Enterprise Message By Type
        .to("kafka:Ent_IntgrtnFHIRSvr_CarePlan?brokers=localhost:9092")
    ;
    from("kafka:IntgrtnFHIRSvr_CarePlan?brokers=localhost:9092")
        .routeId("CarePlan-MiddleTier")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("CarePlan")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("Care Plan to Enterprise By Data Type middle tier")
        .wireTap("direct:auditing")
        // Enterprise Message By Type
        .to("kafka:Ent_IntgrtnFHIRSvr_CarePlan?brokers=localhost:9092")
    ;
    from("kafka:IntgrtnFHIRSvr_CinicalImpression?brokers=localhost:9092")
        .routeId("ClinicalImpression-MiddleTier")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("ClincalImpression")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("Clinical Impression to Enterprise By Data Type middle tier")
        .wireTap("direct:auditing")
        // Enterprise Message By Type
        .to("kafka:Ent_IntgrtnFHIRSvr_ClinicalImpression?brokers=localhost:9092")
    ;
    from("kafka:IntgrtnFHIRSvr_CodeSystem?brokers=localhost:9092")
        .routeId("CodeSystem-MiddleTier")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("CodeSystem")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("CodeSystem to Enterprise By Data Type middle tier")
        .wireTap("direct:auditing")
        // Enterprise Message By Type
        .to("kafka:Ent_IntgrtnFHIRSvr_CodeSystem?brokers=localhost:9092")
    ;
    from("kafka:IntgrtnFHIRSvr_Communication?brokers=localhost:9092")
        .routeId("Communication-MiddleTier")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("Communication")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("Communication to Enterprise By Data Type middle tier")
        .wireTap("direct:auditing")
        // Enterprise Message By Type
        .to("kafka:Ent_IntgrtnFHIRSvr_Communication?brokers=localhost:9092")
    ;
    from("kafka:IntgrtnFHIRSvr_Condition?brokers=localhost:9092")
        .routeId("Condition-MiddleTier")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("Condition")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("Condition to Enterprise By Data Type middle tier")
        .wireTap("direct:auditing")
        // Enterprise Message By Type
        .to("kafka:Ent_IntgrtnFHIRSvr_Condition?brokers=localhost:9092")
    ;
    from("kafka:IntgrtnFHIRSvr_DeviceRequest?brokers=localhost:9092")
            .routeId("DeviceRequest-MiddleTier")
            // set Auditing Properties
            .setProperty("processingtype").constant("data")
            .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
            .setProperty("industrystd").constant("FHIR")
            .setProperty("messagetrigger").constant("DeviceRequest")
            .setProperty("component").simple("${routeId}")
            .setProperty("processname").constant("MTier")
            .setProperty("auditdetails").constant("DeviceRequest to Enterprise By Data Type middle tier")
            .wireTap("direct:auditing")
            // Enterprise Message By Type
            .to("kafka:Ent_IntgrtnFHIRSvr_DeviceRequest?brokers=localhost:9092")
    ;
    from("kafka:IntgrtnFHIRSvr_DeviceUseStatement?brokers=localhost:9092")
         .routeId("DeviceUseStatement-MiddleTier")
         // set Auditing Properties
         .setProperty("processingtype").constant("data")
         .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
         .setProperty("industrystd").constant("FHIR")
         .setProperty("messagetrigger").constant("DeviceUseStatement")
         .setProperty("component").simple("${routeId}")
         .setProperty("processname").constant("MTier")
         .setProperty("auditdetails").constant("DeviceUseStatement to Enterprise By Data Type middle tier")
         .wireTap("direct:auditing")
         // Enterprise Message By Type
         .to("kafka:Ent_IntgrtnFHIRSvr_DeviceRequest?brokers=localhost:9092")
    ;
    from("kafka:IntgrtnFHIRSvr_DiagnosticResult?brokers=localhost:9092")
        .routeId("DiagnosticResult-MiddleTier")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("DiagnosticResult")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("DiagnosticResult to Enterprise By Data Type middle tier")
        .wireTap("direct:auditing")
        // Enterprise Message By Type
        .to("kafka:Ent_IntgrtnFHIRSvr_DiagnosticResult?brokers=localhost:9092")
    ;
    from("kafka:IntgrtnFHIRSvr_Encounter?brokers=localhost:9092")
        .routeId("Encounter-MiddleTier")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("Encounter")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("Encounter to Enterprise By Data Type middle tier")
        .wireTap("direct:auditing")
        // Enterprise Message By Type
        .to("kafka:Ent_IntgrtnFHIRSvr_Encounter?brokers=localhost:9092")
    ;
    from("kafka:IntgrtnFHIRSvr_EpisodeOfCare?brokers=localhost:9092")
        .routeId("EpisodeOfCare-MiddleTier")
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("EpisodeOfCare")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("EpisodeOfCare to Enterprise By Data Type middle tier")
        .wireTap("direct:auditing")
        // Enterprise Message By Type
        .to("kafka:Ent_IntgrtnFHIRSvr_EpisodeOfCare?brokers=localhost:9092")
    ;
    from("kafka:IntgrtnFHIRSvr_Goal?brokers=localhost:9092")
        .routeId("Goal-MiddleTier")
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("Goal")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("Goal to Enterprise By Data Type middle tier")
        .wireTap("direct:auditing")
        // Enterprise Message By Type
        .to("kafka:Ent_IntgrtnFHIRSvr_Goal?brokers=localhost:9092")
    ;
    from("kafka:IntgrtnFHIRSvr_ImagingStudy?brokers=localhost:9092")
        .routeId("ImagingStudy-MiddleTier")
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("ImagingStudy")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("Imaging Study to Enterprise By Data Type middle tier")
        .wireTap("direct:auditing")
        // Enterprise Message By Type
        .to("kafka:Ent_IntgrtnFHIRSvr_Immunization?brokers=localhost:9092")
    ;
    from("kafka:IntgrtnFHIRSvr_Immunization?brokers=localhost:9092")
       .routeId("Immunization-MiddleTier")
       .setProperty("processingtype").constant("data")
       .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
       .setProperty("industrystd").constant("FHIR")
       .setProperty("messagetrigger").constant("Immunization")
       .setProperty("component").simple("${routeId}")
       .setProperty("processname").constant("MTier")
       .setProperty("auditdetails").constant("Immunization to Enterprise By Data Type middle tier")
       .wireTap("direct:auditing")
       // Enterprise Message By Type
       .to("kafka:Ent_IntgrtnFHIRSvr_Immunization?brokers=localhost:9092")
    ;
    from("kafka:IntgrtnFHIRSvr_MedicationDispense?brokers=localhost:9092")
        .routeId("MedicationDispense-MiddleTier")
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("MedicationDispense")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("MedicationDispense to Enterprise By Data Type middle tier")
        .wireTap("direct:auditing")
        // Enterprise Message By Type
        .to("kafka:Ent_IntgrtnFHIRSvr_MedicationDispense?brokers=localhost:9092")
    ;
    from("kafka:IntgrtnFHIRSvr_MedicationRequest?brokers=localhost:9092")
        .routeId("MedicationRequest-MiddleTier")
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("MedicationRequest")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("MedicationRequest to Enterprise By Data Type middle tier")
        .wireTap("direct:auditing")
        // Enterprise Message By Type
        .to("kafka:Ent_IntgrtnFHIRSvr_MedicationRequest?brokers=localhost:9092")
    ;
    from("kafka:IntgrtnFHIRSvr_MedicationAdminstration?brokers=localhost:9092")
        .routeId("MedicationAdminstration-MiddleTier")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("MedicationAdminstration")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("MedicationAdminstration to Enterprise By Data Type middle tier")
        .wireTap("direct:auditing")
        // Enterprise Message By Type
        .to("kafka:Ent_IntgrtnFHIRSvr_MedicationAdminstration?brokers=localhost:9092")
    ;
    from("kafka:IntgrtnFHIRSvr_Observation?brokers=localhost:9092")
        .routeId("Observation-MiddleTier")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("Observation")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("Observation to Enterprise By Data Type middle tier")
        .wireTap("direct:auditing")
        // Enterprise Message By Type
        .to("kafka:Ent_IntgrtnFHIRSvr_Observation?brokers=localhost:9092")
    ;
    from("kafka:IntgrtnFHIRSvr_Order?brokers=localhost:9092")
        .routeId("Order-MiddleTier")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("Order")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("Order to Enterprise By Data Type middle tier")
        .wireTap("direct:auditing")
        // Enterprise Message By Type
        .to("kafka:Ent_IntgrtnFHIRSvr_Order?brokers=localhost:9092")
    ;
    from("kafka:IntgrtnFHIRSvr_Patient?brokers=localhost:9092")
       .routeId("Patient-MiddleTier")
       // Auditing
       .setProperty("processingtype").constant("data")
       .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
       .setProperty("industrystd").constant("FHIR")
       .setProperty("messagetrigger").constant("Patient")
       .setProperty("component").simple("${routeId}")
       .setProperty("processname").constant("MTier")
       .setProperty("auditdetails").constant("Patient to Enterprise By Data Type middle tier")
       .wireTap("direct:auditing")
       // Enterprise Message By Type
       .to("kafka:Ent_IntgrtnFHIRSvr_Patient?brokers=localhost:9092")
    ;
    from("kafka:IntgrtnFHIRSvr_Procedure?brokers=localhost:9092")
       .routeId("Procedure-MiddleTier")
       // Auditing
       .setProperty("processingtype").constant("data")
       .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
       .setProperty("industrystd").constant("FHIR")
       .setProperty("messagetrigger").constant("Procedure")
       .setProperty("component").simple("${routeId}")
       .setProperty("processname").constant("MTier")
       .setProperty("auditdetails").constant("Procedure to Enterprise By Data Type middle tier")
       .wireTap("direct:auditing")
       // Enterprise Message By Type
       .to("kafka:Ent_IntgrtnFHIRSvr_Procedure?brokers=localhost:9092")
    ;
    from("kafka:IntgrtnFHIRSvr_Questionaire?brokers=localhost:9092")
        .routeId("Questionaire-MiddleTier")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("Questionaire")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("Questionaire to Enterprise By Data Type middle tier")
        .wireTap("direct:auditing")
        // Enterprise Message By Type
        .to("kafka:Ent_IntgrtnFHIRSvr_Questionaire?brokers=localhost:9092")
    ;
    from("kafka:IntgrtnFHIRSvr_QuestionaireResponse?brokers=localhost:9092")
        .routeId("QuestionaireResponse-MiddleTier")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("QuestionaireResponse")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("Questionaire Response to Enterprise By Data Type middle tier")
        .wireTap("direct:auditing")
        // Enterprise Message By Type
        .to("kafka:Ent_IntgrtnFHIRSvr_QuestionaireResponse?brokers=localhost:9092")
    ;
    from("kafka:IntgrtnFHIRSvr_Schedule?brokers=localhost:9092")
        .routeId("Schedule-MiddleTier")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("Schedule")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("Schedule to Enterprise By Data Type middle tier")
        .wireTap("direct:auditing")
        // Enterprise Message By Type
        .to("kafka:Ent_IntgrtnFHIRSvr_Procedure?brokers=localhost:9092")
    ;
    from("kafka:IntgrtnFHIRSvr_Specimen?brokers=localhost:9092")
        .routeId("Specimen-MiddleTier")
        // Auditing
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("Specimen")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("MTier")
        .setProperty("auditdetails").constant("Specimen to Enterprise By Data Type middle tier")
        .wireTap("direct:auditing")
        // Enterprise Message By Type
        .to("kafka:Ent_IntgrtnFHIRSvr_Specimen?brokers=localhost:9092")
    ;
  }
}