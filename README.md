# Background
For decades across the various markets within healthcare (clinical, financial, pharmacy and life sciences) integration has been a challenge for many differnt reasons. Within Red Hat there is a specific industry team dedicated to ensuring Red Hat's technologies meet the industry needs and ensuring its customers have meaningful platforms that demonstrate these capabilities. The Red Hat healthcare team, has had a specific interest in enabling and empowering healthcare organizations to transform their integration efforts while also introducing and infusing Red Hat's open organization, open source mindset and community based culture as well. As the Red Hat healthcare team started this journey they also wanted to ensure they could help showcase integration as an innovation enabler and helping simplify IT.

As discussed in the introduction integration has been a challenge for decades in each market within healthcare. Within healthcare the challenges have been driven by many different factors, here are just a few common ones: industry standards (HL7) that aren't constrained enough, Clincal software vendor market constraints, COTS integration vendor lock-in, and government mandates that they cannot directly support without third party vendor engagement.

As the Red Hat healthcare team thought about potential ways to help the clinical integration space, they came up 
some differentiators for whatever platform would be built:

* Extensibility
* Reduce Proprietary Knowledge
* Open Source Enablement
* Modern Application Development/Delivery Capabilities
* Reduce Propritary Skillset(s)
* Innovative Disruption
* Deliver Consistentcy
* Polyglot Language Support

Here's how the differentiators are being addressed:

* Extensibility: The focus has been on building an extensible platform based on need and usage. The platform is just a series of components that can be connected, extended or new components developed as developers and business teams work together. Within the platform it is developed to enable small to large and complex healthcare organizations to leverage this platform. The platform looks at ANY data as by the application, components, facilities, and overall organization associated with EVERY transaction. 
* Reduce Proprietray Knowledge: We have removed COTS or black box box software and gone with a very open approach. Our approach leverages the numerous Open Source and upstream Open Source products Red Hat is engaged in. Here are several of the technologies this platform leverages: Spring Boot, Red Hat Fuse, Red Hat Process Automation Manager (Business Rules, Workflow and Complex Event Processing), Red Hat A-MQ Streams (Kafka)  - (we also into an A-MQ release as well).
* Open Source Enablement: Provide the source code of the components to extend as needed.
* Modern Application Development/Delivery Capabilities: Where applicable everything is cloud native/container focused. Several of the platform components are based on SpringBoot for easy of deployment and running.
* Reduce Preprietary Skillsets: Removing COTS centric skillsets. This platform is about enabling industry best technologies to enable healthcare to build capabilities rapidly. 
* Disruption: This platform is absolutely designed to enable disruption and enable innovation at the speed of business and technologists.
* Deliver Consistency: Focus around industry standard design and integration patterns.
We don't require deep knowledge of the industry standards to get started, just common developer skillsets. 
* Polyglot Language Support: Enabling the use of many different programming languages. The focus here is to ensure by major capability area a programming language is supported.

# Platform: Connected Health / iDAAS 
As part of the general vision for healthcare, the Red Hat healthcare team established a extensible platform, Connected Health/iDAAS (Intelligent Data As A Service). As we have above we wanted to focus on several things extensibility, open source enablement, consistency, modern development approach, disruption and community enablement. In this section we want to cover the specific components within iDAAS that are helping us build forward on these differentiators. As you are reading through this it is important to ensure understanding that iDAAS is a platform that is developed as a set of components working with industry leading technologies that enable us a broad range of capabilities, it is not merely a code base written in a programming language. The reason we have followed this pattern is that we can leverage a large set community to ensure we can address potential issues and concerns quickly with a large technical community. Let's do a walk through of the capability areas within iDAAS.

Here are some of the capabilities, the five R’s:

* Receive: receive data from various formats. We have branded this capability iDAAS Connect. From receiving data we focus on industry standards and then third party connectivity. Industry standards support include HL7 v2, FHIR, and EDI Claims. There are potential future plans for  NCPDP and HL7 v3 message support being discussed. From a third party connectivity perspective we focus on building an on-ramp for data to be leveraged within iDAAS for over 75 common protocols like JDBC data sources, File, FTP, SFTP, FTPS, APIs, WSDL, AS400,  Mongo, Kafka, numerous cloud platforms  and many more.
* Route: enable data to be routed to many sources. For this capability we have focused on building out several specific components such as healthcare event builder (both code and integration) to form the intelligent healthcare data router. In order to demonstrate this, our focus was on building a reusable repeatable enterprise application integration message pattern along with the ability for organizations to build and deliver healthcare even streaming.
* Run:  This is one of the most exciting capabilities we are enabling. This is done through our DREAM component. DREAM, Data Realtime Exchange Automated Management, is all about driving change in the healthcare community and building out a value exchange that can be used by anyone implementing iDAAS to add new capabilities in a low latency and very low risk. You would build a component that has the capability of acting on streaming information and being invoked in a real-time manner. These activities could include areas like event population and building, business rules enablement, third party application integration, AI, ML,  dynamic mapping of data sources, cross mapping codes and many other potential examples.
* Resolve: Enable error handling and insight to resolve potential processing issues. Also, the need to replay messages for new needs.
* Research: Enable access to information in a reusable auditable manner. This includes our ability to enable real time clinical information to be tagged as its passing through the platform.

Here are the technologies that drive the capabilities:

* Integration: Ties back to Red Hat’s Fuse and its upstream Apache Camel community. This technology is backed by one of the most active communities and continues to grow and expand this technology for well over a decade. As part of its commitment there are thousands of implementations of all sizes, types and scale levels in numerous industries with some of them growing to support 1 billion daily transactions. As part of its growth are the hundred plus connectors that it natively supports, this will be very beneficial for Red Hat’s healthcare team as the platform looks to grow and expand based on feedback and demand. 
* Business Rules: This capability is supported by Decision Manager and the upstream Drools community. This technology is very largely adopted and implemented throughout the community. Drools has a very large footprint in organizations all over the world delivering expert based solutions and capabilities.
* Data Streaming: A key effort for us is enabling and building a high end data streaming and distribution platform. This capability is being supported by AMQ-Streams and the upstream Kafka community. We are also looking at future implementations with other distributed queuing technologies. As a side note, within the codebase there is a codebase built atop Apache-MQ; however, we maintain the code ONLY for resources that would rather leverage that technology.
* APIs: we have developed all the APIs leveraging .Net Core v3. This will be in two seperate efforts, one is for the platform and one will be for the data tier.
* UX /User Interfaces: The UX experience for the platform is currently driven by .Net Core v3 based assets and a very modern, responsive application design. These applications are best run on the industry leading OpenShift platform for scale. The security model for these capabilities is currently being worked through to ensure the platform is demonstrable, the focus is on showcasing security and enabling the adopters to tie it to LDAP/AD or SAML based systems for the highest level of security.
* Data: We currently are moving to support DDLs for SQL Server, Maria DB (MySQL) and Postgres. This is because there is wide adoption of these within healthcare. The first few implementations with be SQL Server DDL centric as well finalize data model validation. We also intend to expand towards NoSQL standards quickly as we continue to implement new features.
Connected Health/iDAAS overall objective is to simplify data integration and interoperability needs. By establishing a platform initally this enabled the team to focus on the overall industry needs and work through them and establish capabilities for each. As the capabilities were worked through components have been developed to address these needs. 

Here is a general visual of how iDAAS fits in and can enable innovation and solving the problem of integration innovation in the clinical space:
<p align="center" >
<img src="https://github.com/redhat-healthcare-chiefarchitect/iDAAS-Connect-Clinical-IndustryStds/blob/development/content/images/iDAAS%20-%20High%20Level%20Solution%20Overview.png" alt="Healthcare Integration" width="500" height="350" />
</p>

Here is a visual on the iDAAS Platform and all its specific components:
<p align="center" >
<img src="https://github.com/redhat-healthcare-chiefarchitect/iDAAS-Connect-Clinical-IndustryStds/blob/development/content/images/iDAAS%20Platform%20-%20iDAAS%20Connect%20and%20Vendor%20FHIR%20Modules.png/" alt="iDAAS Component Design" 
width="600" height="600" />
</p>

# Data Enablement: iDAAS Connect Clinical Industry Standards
The problem of healthcare connectivity and data enablement has been around for decades. Vendors have had long standing practices of limiting paying customers to the data within the systems they operate and manage. As healthcare organizations prepare for their digital experiences, or look to re-evaluate their current digital experience capabilities, this is no longer a practice that can be tolerated or endured. Within iDAAS, this is the component responsible for providing connectivity to the clinical based industry standards of HL7 v2 messages and FHIR. From an integration connectivity and standards perspective it can demonstrates the processesing HL7v2 messages of the following types from any vendor and any specifc message version from 2.1 to 2.8: ADT (Admissions), ORM (Orders), ORU (Results), SCH (Schedules), PHA (Pharmacy), MFN (Master File Notifications), MDM (Medical Document Management) and VXU (Vaccinations). With the final CMS rule around Interoperability we have also added FHIR R4 Support. 

# Industry Standard Support
As mentioned above we discussed the industry Standard support. This section covers the clinical integration standards and detailed links that are support by the implementation of the platform:

* [HL7 v2 Message Receivers](https://www.hl7.org/implement/standards/product_brief.cfm?product_id=185 "HL7 v2 Message Receivers") - Support for ADT, ORM, ORU, RDE, SCH, MFN, MDM and VXU message types. Connected Clinical does not care about specific HL7 version, its has been tested from version 2.1 through 2.8.
* [FHIR Clinical Receivers](https://www.hl7.org/fhir/ "HL7 FHIR") - Support for FHIR Clinical is currently being implemented. The platform will focus on delivery R4 (4.01) support to align with the CMS guidance on Interoperability and Patient Access. 

# Development IDE
The iDAAS (Intelligent Data as a Service) Clinical platform was initially developed using Eclipse. While this can still be used to update code, the team wanted to share its has transitioned to the following Development IDE and plugins: 

* IntelliJ Community IDE with the following plugins: Apache Camel Plugin, Big Data Tools Plugin and Kubernetes Plugin

# iDAAS Technologies
The iDAAS (Intelligent Data as a Service) Clinical platform is intended for usage for the healthcare market. It contains the following Red Hat technologies, (this platform will also will work with their upstream equivalents):

| Capabilty Area | Upstream Release | Red Hat Release |
| -------------- | ---------------- | --------------- |
| Integration    | Apache Camel     | Red Hat Fuse   |
| Business Rules | Drools           | Red Hat Decision Mgr |
| Data Streaming | Apache Kafka     | Red Hat A-MQ Streams |
| Containers     | K8               | OpenShift            |

It contains the following non Red Hat technologies:

| Capabilty Area | Technologies |
| -------------- | ------------ |
| RDBMS    | SQL Server, Postgres or MariaDB     |
| Programming Language(s)     | Java 1.8 or higher (determined by software implementations. .NetCore 3 for APIs and UI Platforms|


# Practical Implementation: Partnering Organization
To support ANY developed artifcats the Red Hat Healthcare team has created a fictious company named Care Delivery Corporation US (CADuCeUS). Care Delivery Corp. US is intended to help Red Hat Healthcare demonstrate its solutions across all areas of the healthcare marketspace. While Everything associated with Care Delivery Corp US is fictiotious including the software they leverage and are building the industry scenarios and platform usage is driven by real world healthcare ongoing needs.

Here are some specific details for all the demonstrations developed:

| Item | Technologies |
| ---- | ------------ |
| Healthcare Facilities |  MCTN |   
| Sending Application(s)|  MMS (Main Medical Software)/Care Kiosk UI |
| Custom Integrated Application | myEHR |

# Other Contributions within Source

While this repository has all the source code needed to run the solution, it also has several additional artifacts within specific  directories that are needed to run the code:

* content: This directory is intended to maintain any content published about the platform. Within this directory is the Development documentation and implementation guides along with images that are leveraged within the 
content or site to help ensure everyone has all the available materials.
* platform-scripts: designed to assist implementation with scripts that can be downloaded and leveraged. It should be understood that these scripts will need to be tweaked, mostly to address base implemented directories of solutions. These scripts cover A-MQ and Kafka. The intent for them is to be able to start the products and enable implementors to quickly get the products running. 

# Building and Running

This code can be built with the following command:

mvn clean install

After building and then running the solution, implementors might want to test. We have included industry samples for everyone to leverage.

Within the src/test/data there are two directories test-hl7 and test-fhir.

# Containers Based - Openshift (where possible) 
It is assumed that:

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

# Other Related GitHub Repositories for Connected Health / iDAAS Platform Components
As mentioned above iDAAS is a platform and is built in a very modular manner. We will be releasing other public Git Hub repositories that will enable additional extensibility.
