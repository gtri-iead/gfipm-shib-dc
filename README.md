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

