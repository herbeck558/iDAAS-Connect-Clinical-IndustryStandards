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
            .setHeader("source").simple("{$body}")
            .setHeader("messageprocesseddate").simple("${date:now:yyyy-MM-dd}")
            .setHeader("messageprocessedtime").simple("${date:now:HH:mm:ss:SSS}")
            .setHeader("processingtype").exchangeProperty("processingtype")
            .setHeader("industrystd").exchangeProperty("industrystd")
            .setHeader("component").exchangeProperty("component")
            .setHeader("messagetrigger").exchangeProperty("messagetrigger")
            .setHeader("processname").exchangeProperty("processname")
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
	 *  There is NO restriction or limitation on data by version or with their dreaded Z-Segments
	 *  Please go to https://www.hl7.org/implement/standards/product_brief.cfm?product_id=185 for more details.
	 *  If you need to download ANY specifications an HL7 account will be required.
	 *  Below is an example of how to leverage the test data without needing external HL7 message data
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
     * <hostname>:8080/fhir/<resource>
     * FHIR Resources:
     *  CodeSystem,DiagnosticResult,Encounter,EpisodeOfCare,Immunization,MedicationRequest
     *  MedicationStatement,Observation,Order,Patient,Procedure,Schedule
     */

    // wireTap to direct and the direct will be leveraged to add all the headers in one place
    from("servlet://fhirrcodesystem")
        .routeId("FHIRCodeSystem")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("CodeSystem")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
      // iDAAS DataHub Processing
      .wireTap("direct:auditing")
      // Send To Topic
      .to("kafka:IntgrtnFHIRSvr_CodeSystem?brokers=localhost:9092")
      // Invoke External FHIR Server
      .to("https://localhost:9443/fhir-server/api/v4/CodeSystem")
      // Process Response
     ;

    from("servlet://http://localhost:8888/fhirdiagnosticresult")
        .routeId("FHIRDiagnosticResult")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("DiagnosticResult")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_DiagnosticResult?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/DiagnosticResult")
        //Process Response
    ;

    from("servlet://http://localhost:8888/fhirencounter")
        .routeId("FHIREncounter")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("Encounter")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_Encounter?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/Encounter")
        //Process Response
    ;

    from("servlet://http://localhost:8888/fhirepisodeofcare")
        .routeId("FHIREpisodeOfCare")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("EpisodeOfCare")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_EpisodeOfCare?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/EpisodeOfCare")
        //Process Response
    ;

    // left off here

    from("servlet://http://localhost:8888/fhirmedicationrequest")
        .routeId("FHIRMedicationRequest")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("MedicationRequest")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_MedicationRequest?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/MedicationRequest")
        //Process Response
    ;

    from("servlet://http://localhost:8888/fhirmedicationstatement")
        .routeId("FHIRMedicationStatement")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("MedicationStatement")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_MedicationStatement?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/MedicationStatement")
        //Process Response
    ;

    from("servlet://http://localhost:8888/fhirobservation")
        .routeId("FHIRObservation")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("Observation")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_Observation?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/Observation")
        //Process Response
    ;

    from("servlet://http://localhost:8888/fhirorder")
        .routeId("FHIROrder")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("Order")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_Observation?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/Order")
    //Process Response
    ;

    from("servlet://http://localhost:8888/fhirpatient")
        .routeId("FHIRPatient")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("Patient")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_Observation?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/Patient")
        //Process Response
    ;

    from("servlet://http://localhost:8888/fhirprocedure")
        .routeId("FHIRProcedure")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("Procedure")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_Observation?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/Procedure")
        //Process Response
    ;

    from("servlet://http://localhost:8888/fhirschedule")
        .routeId("FHIRSchedule")
        // set Auditing Properties
        .setProperty("processingtype").constant("data")
        .setProperty("appname").constant("iDAAS-ConnectClinical-IndustryStd")
        .setProperty("industrystd").constant("FHIR")
        .setProperty("messagetrigger").constant("Schedule")
        .setProperty("component").simple("${routeId}")
        .setProperty("processname").constant("Input")
        // iDAAS DataHub Processing
        .wireTap("direct:auditing")
        // Send To Topic
        .to("kafka:IntgrtnFHIRSvr_Observation?brokers=localhost:9092")
        // Invoke External FHIR Server
        .to("https://localhost:9443/fhir-server/api/v4/Schedule")
        //Process Response
    ;

    /*
    *   Middle Tier
    *   Move Transactions from Facility By Sending App by Event Type
    *   To Sending App by Event Type
    *   To Facility by Event Type
    *   To Enterprise By Event Type
    *   from("kafka:test?brokers=localhost:9092")
    */

    /*
     *   ADT Transactions from Sending App By Facility
     *   to Sending App By Message Type
     *   to Facility By Message Type
     *   to Enterprise by Message Type
     */
    from("kafka: MCTN_MMS_ADT?brokers=localhost:9092")
       .routeId("hl7SendingAppADT-MiddleTier")
       .setBody(body())
       // Enterprise Message By Sending App By Type
       .to("kafka:MMS_ADT?brokers=localhost:9092")
       // Facility By Type
       .to("kafka:MCTN_ADT?brokers=localhost:9092")
       // Enterprise Message By Type
       .to("kafka:ENT_ADT?brokers=localhost:9092")
    ;

    // Ensure to add the Facility By Message Type to ALL HL7 v2 middle tier


    /*
     *   ORM Transactions from Sending App By Facility
     *   to Sending App By Message Type
     *   to Enterprise by Message Type
     */
    from("kafka: MCTN_MMS_ORM?brokers=localhost:9092")
      .routeId("hl7SendingAppORM-MiddleTier")
      // Enterprise Message By Sending App By Type
      .to("kafka:MMS_ORM?brokers=localhost:9092")
      // Entrprise Message By Type
      .to("kafka:ENT_ORM?brokers=localhost:9092")
    ;

    /*
     *   ORU Transactions from Sending App By Facility
     *   to Sending App By Message Type
     *   to Enterprise by Message Type
     */
    from("kafka: MCTN_MMS_ORU?brokers=localhost:9092")
      .routeId("hl7SendingAppORU-MiddleTier")
      // Enterprise Message By Sending App By Type
      .to("kafka:MMS_ORU?brokers=localhost:9092")
      // Entrprise Message By Type
      .to("kafka:ENT_ORU?brokers=localhost:9092")
    ;
    /*
     *   SCH Transactions from Sending App By Facility
     *   to Sending App By Message Type
     *   to Enterprise by Message Type
     */
    from("kafka: MCTN_MMS_SCH?brokers=localhost:9092")
      .routeId("hl7SendingAppSCH-MiddleTier")
      // Enterprise Message By Sending App By Type
      .to("kafka:MMS_SCH?brokers=localhost:9092")
      // Entrprise Message By Type
      .to("kafka:ENT_SCH?brokers=localhost:9092")
    ;
    /*
     *   RDE Transactions from Sending App By Facility
     *   to Sending App By Message Type
     *   to Enterprise by Message Type
     */
    from("kafka: MCTN_MMS_RDE?brokers=localhost:9092")
       .routeId("hl7SendingAppRDE-MiddleTier")
       // Enterprise Message By Sending App By Type
       .to("kafka:MMS_RDE?brokers=localhost:9092")
       // Entrprise Message By Type
       .to("kafka:ENT_RDE?brokers=localhost:9092")
    ;
    /*
     *   MDM Transactions from Sending App By Facility
     *   to Sending App By Message Type
     *   to Enterprise by Message Type
     */
    from("kafka: MCTN_MMS_MDM?brokers=localhost:9092")
      .routeId("hl7SendingAppMDM-MiddleTier")
      // Enterprise Message By Sending App By Type
      .to("kafka:MMS_MDM?brokers=localhost:9092")
      // Entrprise Message By Type
      .to("kafka:ENT_MDM?brokers=localhost:9092")
    ;
    /*
     *   MFN Transactions from Sending App By Facility
     *   to Sending App By Message Type
     *   to Enterprise by Message Type
     */
    from("kafka: MCTN_MMS_MFN?brokers=localhost:9092")
      .routeId("hl7SendingAppMFN-MiddleTier")
      // Enterprise Message By Sending App By Type
      .to("kafka:MMS_MFN?brokers=localhost:9092")
      // Entrprise Message By Type
      .to("kafka:ENT_MFN?brokers=localhost:9092")
    ;
    /*
     *   VXU Transactions from Sending App By Facility
     *   to Sending App By Message Type
     *   to Enterprise by Message Type
     */
    from("kafka: MCTN_MMS_VXU?brokers=localhost:9092")
      .routeId("hl7SendingAppVXU-MiddleTier")
      // Enterprise Message By Sending App By Type
      .to("kafka:MMS_VXU?brokers=localhost:9092")
      // Entrprise Message By Type
      .to("kafka:ENT_VXU?brokers=localhost:9092")
      //.to("kafka:opsMgmt_ProcessedTransactions?brokers={{kafkasettings.hostvalue}}:{{kafkasettings.portnumber}}")
    ;

  }
}
