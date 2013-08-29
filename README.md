gfipm-shib-dc
=============

GFIPM Shibboleth Dataconnector.  Includes a BAE dataconnector and an attribute file based test dataconnector.

Build with mvn clean install.

To use this data connector with Shibboleth, you add it to your Attribute-Resolver configuration.  First you must
declare the namespace.  Include the following in the top of the config file:

    <AttributeResolver xmlns:gfipm-bae="urn:global:gfipm:1.1:bae-resolver"
                       xsi:schemaLocation="urn:global:gfipm:1.1:bae-resolver classpath:/schema/gfipm-1.1-resolver.xsd">


Here is a sample test data data connector (files are read from 
/opt/shib-idp/users/${LocalId}.attr and then the contents of the 
file are returned as attribute name/value pairs):

    <resolver:DataConnector id="GfipmTest" xsi:type="gfipm-bae:Test"
                            xmlns="urn:global:gfipm:1.1:bae-resolver"
                            pathToAttributeFiles="/opt/shib-idp/users/"
                            uidAttribute="LocalId">
        <resolver:Dependency ref="LocalId" />

    </resolver:DataConnector>

To use the BAE Query you configure the BAE resolver like this:

    <resolver:DataConnector id="myBAE" xsi:type="gfipm-bae:BAE"
                            xmlns="urn:global:gfipm:1.1:bae-resolver"
                            baeURL="serviceUrl"
                            subjectId="ShibAttributeIdToBeSentToBAEServiceAsNameId"
                            baeEntityId="serviceId"
                            myEntityId="clientId">
        <resolver:Dependency ref="DependencyForAttributeId" />


        <gfipm-bae:TrustCredential xsi:type="X509Filesystem" xmlns="urn:mace:shibboleth:2.0:security" id="IIRCert">
            <Certificate>/opt/shib-idp/credentials/bae-service.crt</Certificate>
        </gfipm-bae:TrustCredential>
        <gfipm-bae:AuthenticationCredential xsi:type="X509Filesystem" xmlns="urn:mace:shibboleth:2.0:security" id="MyBAECred">
           <PrivateKey>/opt/shib-idp/credentials/your.key</PrivateKey>
           <Certificate>/opt/shib-idp/credentials/your.crt</Certificate>
        </gfipm-bae:AuthenticationCredential>


        <gfipm-bae:Attribute QueryName="gfipm:2.0:user:attrName"
                             ReturnName="ShibAttrName" />

    </resolver:DataConnector>


To activate the use of SHA256 for digital signatures (if not enabled via another extension) you may add the following to the IdP's internal.xml:

    <bean id="OpensamlCustomCryptoConfig" class="net.gfipm.shibboleth.cryptoconfig.OpensamlCustomCryptoConfigBean" depends-on="shibboleth.OpensamlConfig" />


All of the included code and configuration files are  Copyright (c) 2013, Georgia Institute of Technology. All Rights Reserved.  This code was developed by Georgia Tech Research Institute (GTRI) under a grant from the U.S. Dept. of Homeland Security, Science & Technologies Division in collaboration with Johns Hopkins University Applied Physics Laboratory. It is licensed under the Apache License, Version 2.0 (the "License"); you may not use these files except in compliance with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

