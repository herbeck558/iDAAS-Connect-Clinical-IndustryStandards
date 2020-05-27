# iDAAS Connect Clinical Industry Standards
iDAAS Connect is all about connecting to data sources or protocols. In this specific case iDAAS Connect Clinical Industry Standards hopes to define iDAAS connecting to clinical data coming from industry standards. As you read, or can find on the <a href="https://github.com/RedHat-Healthcare/iDAAS" target="_blank">iDAAS main repository</a> we discussed the need for what we called the Five R's, here are the two R's this component helps us achieve:

* Receive: receive data from various formats. We have branded this capability iDAAS Connect. From receiving data we focus on industry standards and then third party connectivity. Industry standards support include HL7 v2, FHIR, and EDI Claims. There are potential future plans for  NCPDP and HL7 v3 message support being discussed. From a third party connectivity perspective we focus on building an on-ramp for data to be leveraged within iDAAS for over 75 common protocols like JDBC data sources, File, FTP, SFTP, FTPS, APIs, WSDL, AS400,  Mongo, Kafka, numerous cloud platforms  and many more.
* Route: enable data to be routed to many sources. For this capability we have focused on building out several specific components such as healthcare event builder (both code and integration) to form the intelligent healthcare data router. In order to demonstrate this, our focus was on building a reusable repeatable enterprise application integration message pattern along with the ability for organizations to build and deliver healthcare even streaming.

As you think back to the capabilities provided iDAAS Connect Clinical Industry Standards help us achieve the following:

* Integration: Ties back to Red Hat’s Fuse and its upstream Apache Camel community. This technology is backed by one of the most active communities and continues to grow and expand this technology for well over a decade. As part of its commitment there are thousands of implementations of all sizes, types and scale levels in numerous industries with some of them growing to support 1 billion daily transactions. As part of its growth are the hundred plus connectors that it natively supports, this will be very beneficial for Red Hat’s healthcare team as the platform looks to grow and expand based on feedback and demand. 

The problem of healthcare connectivity and data enablement has been around for decades. Vendors have had long standing practices of limiting paying customers to the data within the systems they operate and manage. As healthcare organizations prepare for their digital experiences, or look to re-evaluate their current digital experience capabilities, this is no longer a practice that can be tolerated or endured. Within iDAAS, this is the component responsible for providing connectivity to the clinical based industry standards of HL7 v2 messages and FHIR. From an integration connectivity and standards perspective it can demonstrates the processesing HL7v2 messages of the following types from any vendor and any specifc message version from 2.1 to 2.8: ADT (Admissions), ORM (Orders), ORU (Results), SCH (Schedules), PHA (Pharmacy), MFN (Master File Notifications), MDM (Medical Document Management) and VXU (Vaccinations). With the final CMS rule around Interoperability we have also added FHIR R4 Support. 

* [HL7 v2 Message Receivers](https://www.hl7.org/implement/standards/product_brief.cfm?product_id=185 "HL7 v2 Message Receivers") - Support for ADT, ORM, ORU, RDE, SCH, MFN, MDM and VXU message types. Connected Clinical does not care about specific HL7 version, its has been tested from version 2.1 through 2.8.
* [FHIR Clinical Receivers](https://www.hl7.org/fhir/ "HL7 FHIR") - Support for FHIR Clinical is currently being implemented. The platform will focus on delivery R4 (4.01) support to align with the CMS guidance on Interoperability and Patient Access. 

As part of the platform we have designed an enterprise integration pattern called Healthcare Data Distribution Enterprise Integration Pattern. This pattern is very important as anyone thinks about how to leverage ANY healthcare data. When and where possible the platform not only audit the transavtions for other activities but it also makes at least one copy of it available for varying needs of healthcare systems. 

Here is a visual on the iDAAS Platform and all its specific components:
<p align="center" >
<img src="https://github.com/RedHat-Healthcare/iDAAS/blob/master/content/images/iDAAS-Platform/iDAASPlatform-HCDD-EIP.png" alt="iDAAS Component Design" 
width="900" height="600" />
</p>

## Other Repositories Needed/Required
Because of the design of iDAAS some components could require additional components to enable additional iDAAS features or capabilities. iDAAS Connect Clinical Industry Standards is independant; however, adding the middle tier component below enables a very reusable data distribution design pattern. 

* <a href="https://github.com/RedHat-Healthcare/iDAAS-Connect-Clinical-MiddleTier" target="_blank">iDAAS Connect Clinical Middle Tier</a>: This component is specifically implemented to help keep the iDAAS Connect Clinical Industry Standards component small by moving the data streaming and HCDD-EIP (Healthcare Data Distribution and Enterprise Integration Pattern).

## Other Contributions within Source
As discussed in the iDAAS base repository this component has additional contributions in order to assist. In order to try and not just put the software out there we also wanted to help development and implementation as well. 
To help support these areas we have included additional artifacts within specific directories. 

* content: This directory is intended to maintain any content published about the specific component. Within this directory is the Development documentation and implementation guides. The core images and general base content that we use across all iDAAS components are leveraged within the <a href="https://github.com/RedHat-Healthcare/iDAAS" target="_blank">iDAAS core repository</a> within the content directory.
* platform-scripts: designed to assist implementation with scripts that can be downloaded and leveraged. 
It should be understood that these scripts will need to be tweaked, mostly to address base implemented directories of solutions. These scripts currently cover A-MQ and Kafka. The intent for them is to be able to start the products and enable implementors to quickly get the products running. 
* testing data: <a href="https://github.com/RedHat-Healthcare/iDAAS/tree/master/testdata" target="_blank">iDAAS base testing data</a>. After building, running and/or deploying implementors might want to test. We have included industry samples for everyone to leverage. Within the solution there are several directories to leverage, this will be the consistent area where we continue to update testing capabilities. <br>
    - You can leverage the files within samples-hl7 messages to send data via ANY MLLP client. <br>
    - You can leverage the files within samples-fhir messages to post these files using postman or other
common tooling for testing endpoints.<br>

# Practical Scenarios This Component Addrresses
As mentioned in the <a href="https://github.com/RedHat-Healthcare/iDAAS" target="_blank">iDAAS Repository</a> in much greater detail the Red Hat Healthcare team has created a fictious company named Care Delivery Corporation US (CADuCeUS).

Here are some specific details for all the demonstrations developed:

| Item | Technologies |
| ---- | ------------ |
| Healthcare Facilities |  MCTN |   
| Sending Application(s)|  MMS (Main Medical Software)/Care Kiosk UI |
| Custom Integrated Application | myEHR |

Here are some of the scenarios this component has developed into it for ease of demonstrating.

## Use Cases / Scenarios
The following scenarios as what iDAAS Connect Clinical Industry Standards platform can help demonstrate.

* HL7 v2 message processing of the most commonly used transactions. There is NO version, vendor or other limitation (like Z segment processing)
* FHIR R4 processing, currently there are almost 40 clinical and reporting FHIR resources supported specific to this platform component.
* Enterprise and/or Organization and/or Application level by message type data distribution of data leveraging the HCDD-EIP.

# Building, Running and Testing

## Building
This code can be built (and will be run) with the following command:

mvn clean install

## Packaging
To package the solution to a single jar:

mvn package

## Running

To Run The Platform:
1. Make sure Kafka is started (start script is in the platform-scripts directory)
2. Ensure Topics are in place (you can run the kafkacmd-topics-list in the platform-scripts directory).
if there a topics then you should be ok to run. If you are concerned or no topics are listed, then run the 
kafkacmd_topics_createiDAAS script in the platform-scripts directory. All that will happen is ANY queues that are not in place when the create script runs will be created.
3. java -jar <jarfile.jar> 

## Testing
We are currently creating a tesing component to simplify testing. In the meantime as we work on this please follow the following general testing implementation steps:

### HL7 v2
1. Go to <a href="https://github.com/RedHat-Healthcare/iDAAS/tree/master/testdata" target="_blank">iDAAS base testing data</a> and get the needed messages from samples-hl7 directory. 
2. Use any standard HL7 client and after connecting to the correct MLLP socket send a transaction. With the base platform you will be able to see a transaction process and also the acknowledgement sent back.

### FHIR
1. Go to <a href="https://github.com/RedHat-Healthcare/iDAAS/tree/master/testdata" target="_blank">iDAAS base testing data</a> and get the needed messages from samples-fhir directory. 
2. Leverage a tool like Postman and configure the endpoint to leverage. For general reference it is hrrp://hostname:8080/camel/<fhirResourceName>. If you connect a FHIR server there will be some additional configuration in the platform to do.

## Containers Based - Where Possible 
As we have discussed the iDAAS platform we have taken a very modern cloud native approach to everything. As you will see when you package the solution they are very small < 80 megs and have a ton of features. However, it is important to know that some components CANNOT be run as containers accurately. Specifically, the HL7 connections cannot be accurateot scaled as containers as they are long running server socket based protocols. Since this plaform has HL7v2 and FHIR bundled into the same solution you will just need to be aware of this.

### Docker
Please feel free to visit our Docker organization at <a href="https://hub.docker.com/orgs/redhathealthcare" target="_blank">
Docker - Red Hat Healthcare Org</a>

### OpenShift
OpenShift platform is already running, if not you can find details how to Install OpenShift at your site.
Your system is configured for Fabric8 Maven Workflow, if not you can find a Get Started Guide
The example can be built and run on OpenShift using a single goal:

mvn fabric8:deploy
When the example runs in OpenShift, you can use the OpenShift client tool to inspect the status

To list all the running pods:

oc get pods
Then find the name of the pod that runs this quickstart, and output the logs from the running pods with:

oc logs <name of pod>
You can also use the OpenShift web console to manage the running pods, and view logs and much more.

Running via an S2I Application Template

Application templates allow you deploy applications to OpenShift by filling out a form in the OpenShift console that allows you to adjust deployment parameters. This template uses an S2I source build so that it handle building and deploying the application for you.

First, import the Fuse image streams:

oc create -f https://raw.githubusercontent.com/jboss-fuse/application-templates/GA/fis-image-streams.json
Then create the quickstart template:

oc create -f https://raw.githubusercontent.com/jboss-fuse/application-templates/GA/quickstarts/spring-boot-camel-template.json
Now when you use "Add to Project" button in the OpenShift console, you should see a template for this quickstart.
