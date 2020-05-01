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
   * HL7 implementation based upon https://camel.apache.org/components/latest/dataformats/hl7-dataformat.html
   */
  @Override
  public void configure() throws Exception {
	  /*
	   *  HL7 v2x Server Implementations
	   *  ------------------------------
	   *  There is NO restriction or limitation on data by version or with their dreaded Z-Segments
	   *  Please go to https://www.hl7.org/implement/standards/product_brief.cfm?product_id=185 for more details.
	   *  If you need to download ANY specifications an HL7 account will be required.
	   */

	  // ADT
	  from("netty4:tcp://0.0.0.0:10001?sync=true&decoder=#hl7Decoder&encoder=#hl7Encoder")
	    //from("file:src/data-in/hl7v2/adt?delete=true?noop=true")
        .routeId("hl7TcpRouteAdmissions")
        .to("kafka:MCTN_MMS_ADT?brokers=localhost:9092")
        // iDAAS DataHub Processing
        .to("kafka:opsMgmt_HL7_RcvdTrans?brokers=localhost:9092")
        //.log(LoggingLevel.INFO, log, "HL7 Admissions Message: [${body}]")
        //Response to HL7 Message Sent Built by platform
        .transform(HL7.ack())
      // This would enable persistence of the ACK
    ;

    // ORM
    from("netty4:tcp://0.0.0.0:10002?sync=true&decoder=#hl7Decoder&encoder=#hl7Encoder")
      //from("file:src/data-in/hl7v2/orm?delete=true?noop=true")
      .routeId("hl7TcpRouteOrders")
      .to("kafka:MCTN_MMS_ORM?brokers=localhost:9092")
      // iDAAS DataHub Processing
      .to("kafka:opsMgmt_HL7_RcvdTrans?brokers=localhost:9092")
      .log(LoggingLevel.INFO, log, "HL7 Order Message: [${body}]")
      //Response to HL7 Message Sent Built by platform
      .transform(HL7.ack())
      // This would enable persistence of the ACK
    ;

    // ORU
    from("netty4:tcp://0.0.0.0:10003?sync=true&decoder=#hl7Decoder&encoder=#hl7Encoder")
      //from("file:src/data-in/hl7v2/orm?delete=true?noop=true")
      .routeId("hl7TcpRouteResults")
      .to("kafka:MCTN_MMS_ORU?brokers=localhost:9092")
      // iDAAS DataHub Processing
      .to("kafka:opsMgmt_HL7_RcvdTrans?brokers=localhost:9092")
      .log(LoggingLevel.INFO, log, "HL7 Result Message: [${body}]")
      //Response to HL7 Message Sent Built by platform
      .transform(HL7.ack())
      // This would enable persistence of the ACK
    ;

    // RDE
    from("netty4:tcp://0.0.0.0:10004?sync=true&decoder=#hl7Decoder&encoder=#hl7Encoder")
      //from("file:src/data-in/hl7v2/orm?delete=true?noop=true")
      .routeId("hl7TcpRoutePharmacy")
      .to("kafka:MCTN_MMS_RDE?brokers=localhost:9092")
      // iDAAS DataHub Processing
      .to("kafka:opsMgmt_HL7_RcvdTrans?brokers=localhost:9092")
      .log(LoggingLevel.INFO, log, "HL7 Pharmacy Message: [${body}]")
      //Response to HL7 Message Sent Built by platform
      .transform(HL7.ack())
      // This would enable persistence of the ACK
    ;

    // MFN
    from("netty4:tcp://0.0.0.0:10005?sync=true&decoder=#hl7Decoder&encoder=#hl7Encoder")
      //from("file:src/data-in/hl7v2/orm?delete=true?noop=true")
      .routeId("hl7TcpRouteMasterFiles")
      .to("kafka:MCTN_MMS_MFN?brokers=localhost:9092")
      // iDAAS DataHub Processing
      .to("kafka:opsMgmt_HL7_RcvdTrans?brokers=localhost:9092")
      .log(LoggingLevel.INFO, log, "HL7 Master File Message: [${body}]")
      //Response to HL7 Message Sent Built by platform
      .transform(HL7.ack())
    // This would enable persistence of the ACK
    ;

    // MDM
    from("netty4:tcp://0.0.0.0:10006?sync=true&decoder=#hl7Decoder&encoder=#hl7Encoder")
       //from("file:src/data-in/hl7v2/orm?delete=true?noop=true")
       .routeId("hl7TcpRouteMasterDocs")
       .to("kafka:MCTN_MMS_MDM?brokers=localhost:9092")
       // iDAAS DataHub Processing
       .to("kafka:opsMgmt_HL7_RcvdTrans?brokers=localhost:9092")
       .log(LoggingLevel.INFO, log, "HL7 Master Doc Message: [${body}]")
       //Response to HL7 Message Sent Built by platform
       .transform(HL7.ack())
      // This would enable persistence of the ACK
    ;

    // SCH
    from("netty4:tcp://0.0.0.0:10007?sync=true&decoder=#hl7Decoder&encoder=#hl7Encoder")
       //from("file:src/data-in/hl7v2/orm?delete=true?noop=true")
       .routeId("hl7TcpRouteSchedule")
       .to("kafka:MCTN_MMS_SCH?brokers=localhost:9092")
       // iDAAS DataHub Processing
       .to("kafka:opsMgmt_HL7_RcvdTrans?brokers=localhost:9092")
       .log(LoggingLevel.INFO, log, "HL7 Schedule Message: [${body}]")
       //Response to HL7 Message Sent Built by platform
       .transform(HL7.ack())
       // This would enable persistence of the ACK
    ;

    // VXU
    from("netty4:tcp://0.0.0.0:10008?sync=true&decoder=#hl7Decoder&encoder=#hl7Encoder")
       //from("file:src/data-in/hl7v2/orm?delete=true?noop=true")
       .routeId("hl7TcpRouteVaccination")
       .to("kafka:MCTN_MMS_VXU?brokers=localhost:9092")
       // iDAAS DataHub Processing
       .to("kafka:opsMgmt_HL7_RcvdTrans?brokers=localhost:9092")
       .log(LoggingLevel.INFO, log, "HL7 Vaccination Message: [${body}]")
       //Response to HL7 Message Sent Built by platform
       .transform(HL7.ack())
       // This would enable persistence of the ACK
    ;

    /*
     *  FHIR
     *  ----
     *  FHIR Resources:
     *  CodeSystem
     *  DiagnosticResult
     *  Encounter
     *  EpisodeOfCare
     *  Immunization
     *  MedicationRequest
     *  MedicationStatement
     *  Observation
     *  Order
     *  Patient
     *  Procedure
     *  Schedule
     */

    // Audit
    from("direct:audit")
            // look at simple for expressions of exchange properties
            // .setHeader("source").simple("Value")
            .setHeader("source").constant("Value")
            .setHeader("sourcename").exchangeProperty("component")

        .to("kafka:opsMgmt_FHIR_RcvdTrans?brokers=localhost:9092")
    ;

    // wireTap to direct and the direct will be leveraged to add all the headers in one place
    from("servlet://fhirrcodesystem")
      .routeId("FHIRCodeSystem")
      .setProperty("component").constant("FHIRCodeSystem")
      // iDAAS DataHub Processing
      //.wireTap("kafka:opsMgmt_FHIR_RcvdTrans?brokers=localhost:9092")
      .wireTap("direct:audit")
      // Persist for Routing
      //.wireTap("kafka:FHIRCodes?brokers=localhost:9092")
      .to("https://localhost:9443/fhir-server/api/v4/CodeSystem")
      .wireTap("kafka:opsMgmt_FHIR_RcvdTrans?brokers=localhost:9092")
     ;

    from("jetty://http://localhost:8888/fhirdiagnosticresult")
      //to("https://weather.yahoo.com/united-states?bridgeEndpoint=true&throwExceptionOnFailure=false&traceEnabled").
      .routeId("FHIRDiagnosticResult")
      .log(LoggingLevel.INFO, log, "FHIR Diagnostic Result Message: [${body}]")
      // iDAAS DataHub Processing
      .wireTap("kafka:opsMgmt_FHIR_RcvdTrans?brokers=localhost:9092")
    ;

    from("jetty://http://localhost:8888/fhirencounter")
      //to("https://weather.yahoo.com/united-states?bridgeEndpoint=true&throwExceptionOnFailure=false&traceEnabled").
      .routeId("FHIREncounter")
      .log(LoggingLevel.INFO, log, "FHIR Encounter Message: [${body}]")
      // iDAAS DataHub Processing
      .to("kafka:opsMgmt_FHIR_RcvdTrans?brokers=localhost:9092")
    ;

    from("jetty://http://localhost:8888/fhirepisodeofcare")
      //to("https://weather.yahoo.com/united-states?bridgeEndpoint=true&throwExceptionOnFailure=false&traceEnabled").
      .routeId("FHIREpisodeOfCare")
      .log(LoggingLevel.INFO, log, "FHIR Episode of Care Message: [${body}]")
      // iDAAS DataHub Processing
      .to("kafka:opsMgmt_FHIR_RcvdTrans?brokers=localhost:9092")
    ;

    from("jetty://http://localhost:8888/fhirmedicationstatement")
      //to("https://weather.yahoo.com/united-states?bridgeEndpoint=true&throwExceptionOnFailure=false&traceEnabled").
      .routeId("FHIRMedicationStatement")
      .log(LoggingLevel.INFO, log, "FHIR Medication Statement Message: [${body}]")
      // iDAAS DataHub Processing
      .to("kafka:opsMgmt_FHIR_RcvdTrans?brokers=localhost:9092")
    ;

    from("jetty://http://localhost:8888/fhirobservation")
      //to("https://weather.yahoo.com/united-states?bridgeEndpoint=true&throwExceptionOnFailure=false&traceEnabled").
      .routeId("FHIRObservation")
      .log(LoggingLevel.INFO, log, "FHIR Observation Message: [${body}]")
      // iDAAS DataHub Processing
      .to("kafka:opsMgmt_FHIR_RcvdTrans?brokers=localhost:9092")
    ;

    from("jetty://http://localhost:8888/fhirorder")
      //to("https://weather.yahoo.com/united-states?bridgeEndpoint=true&throwExceptionOnFailure=false&traceEnabled").
      .routeId("FHIROrder")
      .log(LoggingLevel.INFO, log, "FHIR Order Message: [${body}]")
      // iDAAS DataHub Processing
      .to("kafka:opsMgmt_FHIR_RcvdTrans?brokers=localhost:9092")
    ;

    from("jetty://http://localhost:8888/fhirpatient")
      //to("https://weather.yahoo.com/united-states?bridgeEndpoint=true&throwExceptionOnFailure=false&traceEnabled").
      .routeId("FHIRPatient")
      .log(LoggingLevel.INFO, log, "FHIR Patient Message: [${body}]")
      // iDAAS DataHub Processing
      .to("kafka:opsMgmt_FHIR_RcvdTrans?brokers=localhost:9092")
    ;

    from("jetty://http://localhost:8888/fhirprocedure")
      //to("https://weather.yahoo.com/united-states?bridgeEndpoint=true&throwExceptionOnFailure=false&traceEnabled").
      .routeId("FHIRProcedure")
      .log(LoggingLevel.INFO, log, "FHIR Procedure Message: [${body}]")
      // iDAAS DataHub Processing
      .to("kafka:opsMgmt_FHIR_RcvdTrans?brokers=localhost:9092")
    ;

    from("jetty://http://localhost:8888/fhirschedule")
      //to("https://weather.yahoo.com/united-states?bridgeEndpoint=true&throwExceptionOnFailure=false&traceEnabled").
      .routeId("FHIRSchedule")
      .log(LoggingLevel.INFO, log, "FHIR Schedule Message: [${body}]")
      // iDAAS DataHub Processing
      .to("kafka:opsMgmt_FHIR_RcvdTrans?brokers=localhost:9092")
    ;


    /*
    *   Middle Tier
    *   Move Transactions from Facility By Sending App by Event Type
    *   To Sending App by Event Type
    *   To Enterprise By Event Type
    *   from("kafka:test?brokers=localhost:9092")
    */

    /*
     *   ADT Transactions from Sending App By Facility
     *   to Sending App By Message Type
     *   to Enterprise by Message Type
     */
    from("kafka: MCTN_MMS_ADT?brokers=localhost:9092")
       .routeId("hl7SendingAppADT-MiddleTier")
       .setBody(body())
       // Enterprise Message By Sending App By Type
       .to("kafka:MMS_ADT?brokers=localhost:9092")
       // Ensure iDAAS Data can track processing
       .to("kafka:opsMgmt_ProcessedTransactions?brokers=localhost:9092")
       // Enterprise Message By Type
       .to("kafka:ENT_ADT?brokers=localhost:9092")
       // Ensure iDAAS Data can track processing
       .to("kafka:opsMgmt_ProcessedTransactions?brokers=localhost:9092")
    ;

    /*
     *   ORM Transactions from Sending App By Facility
     *   to Sending App By Message Type
     *   to Enterprise by Message Type
     */
    from("kafka: MCTN_MMS_ORM?brokers=localhost:9092")
      .routeId("hl7SendingAppORM-MiddleTier")
      .setBody(body())
      // Enterprise Message By Sending App By Type
      .to("kafka:MMS_ORM?brokers=localhost:9092")
      // Ensure iDAAS Data can track processing
      .to("kafka:opsMgmt_ProcessedTransactions?brokers=localhost:9092")
      // Entrprise Message By Type
      .to("kafka:ENT_ORM?brokers=localhost:9092")
      // Ensure iDAAS Data can track processing
      .to("kafka:opsMgmt_ProcessedTransactions?brokers=localhost:9092")
    ;

    /*
     *   ORU Transactions from Sending App By Facility
     *   to Sending App By Message Type
     *   to Enterprise by Message Type
     */
    from("kafka: MCTN_MMS_ORU?brokers=localhost:9092")
      .routeId("hl7SendingAppORU-MiddleTier")
      .setBody(body())
      // Enterprise Message By Sending App By Type
      .to("kafka:MMS_ORU?brokers=localhost:9092")
      // Ensure iDAAS Data can track processing
      .to("kafka:opsMgmt_ProcessedTransactions?brokers=localhost:9092")
      // Entrprise Message By Type
      .to("kafka:ENT_ORU?brokers=localhost:9092")
      // Ensure iDAAS Data can track processing
      .to("kafka:opsMgmt_ProcessedTransactions?brokers=localhost:9092")
    ;
    /*
     *   SCH Transactions from Sending App By Facility
     *   to Sending App By Message Type
     *   to Enterprise by Message Type
     */
    from("kafka: MCTN_MMS_SCH?brokers=localhost:9092")
      .routeId("hl7SendingAppSCH-MiddleTier")
      .setBody(body())
      // Enterprise Message By Sending App By Type
      .to("kafka:MMS_SCH?brokers=localhost:9092")
      // Ensure iDAAS Data can track processing
      .to("kafka:opsMgmt_ProcessedTransactions?brokers=localhost:9092")
      // Entrprise Message By Type
      .to("kafka:ENT_SCH?brokers=localhost:9092")
      // Ensure iDAAS Data can track processing
      .to("kafka:opsMgmt_ProcessedTransactions?brokers=localhost:9092")
    ;
    /*
     *   RDE Transactions from Sending App By Facility
     *   to Sending App By Message Type
     *   to Enterprise by Message Type
     */
    from("kafka: MCTN_MMS_RDE?brokers=localhost:9092")
       .routeId("hl7SendingAppRDE-MiddleTier")
       .setBody(body())
       // Enterprise Message By Sending App By Type
       .to("kafka:MMS_RDE?brokers=localhost:9092")
       // Ensure iDAAS Data can track processing
       .to("kafka:opsMgmt_ProcessedTransactions?brokers=localhost:9092")
       // Entrprise Message By Type
       .to("kafka:ENT_RDE?brokers=localhost:9092")
       // Ensure iDAAS Data can track processing
       .to("kafka:opsMgmt_ProcessedTransactions?brokers=localhost:9092")
    ;
    /*
     *   MDM Transactions from Sending App By Facility
     *   to Sending App By Message Type
     *   to Enterprise by Message Type
     */
    from("kafka: MCTN_MMS_MDM?brokers=localhost:9092")
      .routeId("hl7SendingAppMDM-MiddleTier")
      .setBody(body())
      // Enterprise Message By Sending App By Type
      .to("kafka:MMS_MDM?brokers=localhost:9092")
      // Ensure iDAAS Data can track processing
      .to("kafka:opsMgmt_ProcessedTransactions?brokers=localhost:9092")
      // Entrprise Message By Type
      .to("kafka:ENT_MDM?brokers=localhost:9092")
      // Ensure iDAAS Data can track processing
      .to("kafka:opsMgmt_ProcessedTransactions?brokers=localhost:9092")
    ;
    /*
     *   MFN Transactions from Sending App By Facility
     *   to Sending App By Message Type
     *   to Enterprise by Message Type
     */
    from("kafka: MCTN_MMS_MFN?brokers=localhost:9092")
      .routeId("hl7SendingAppMFN-MiddleTier")
      .setBody(body())
      // Enterprise Message By Sending App By Type
      .to("kafka:MMS_MFN?brokers=localhost:9092")
      // Ensure iDAAS Data can track processing
      .to("kafka:opsMgmt_ProcessedTransactions?brokers=localhost:9092")
      // Entrprise Message By Type
      .to("kafka:ENT_MFN?brokers=localhost:9092")
      // Ensure iDAAS Data can track processing
      .to("kafka:opsMgmt_ProcessedTransactions?brokers=localhost:9092")
    ;
    /*
     *   VXU Transactions from Sending App By Facility
     *   to Sending App By Message Type
     *   to Enterprise by Message Type
     */
    from("kafka: MCTN_MMS_VXU?brokers=localhost:9092")
      .routeId("hl7SendingAppVXU-MiddleTier")
      .setBody(body())
      // Enterprise Message By Sending App By Type
      .to("kafka:MMS_VXU?brokers=localhost:9092")
      // Ensure iDAAS Data can track processing
      .to("kafka:opsMgmt_ProcessedTransactions?brokers=localhost:9092")
      // Entrprise Message By Type
      .to("kafka:ENT_VXU?brokers=localhost:9092")
      // Ensure iDAAS Data can track processing
      // .to("kafka:opsMgmt_ProcessedTransactions?brokers=localhost:9092")
      .to("kafka:opsMgmt_ProcessedTransactions?brokers={{kafkasettings.hostvalue}}:{{kafkasettings.portnumber}}")
    ;

  }
}
