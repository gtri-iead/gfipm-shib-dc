/*
 * Copyright [2005] [University Corporation for Advanced Internet Development, Inc.] Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.gfipm.shibboleth.dataconnector;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.BaseDataConnector;

import org.opensaml.xml.security.x509.X509Credential;
import org.opensaml.xml.security.SecurityHelper;
import org.opensaml.xml.security.x509.X509Util;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.gtri.gfipm.bae.v2_0.SubjectIdentifier;
import org.gtri.gfipm.bae.v2_0.EmailSubjectIdentifier;
import org.gtri.gfipm.bae.v2_0.BAEServerInfo;
import org.gtri.gfipm.bae.v2_0.BAEClientInfo;
import org.gtri.gfipm.bae.v2_0.BAEServer;
import org.gtri.gfipm.bae.v2_0.BAEServerFactory;
import org.gtri.gfipm.bae.v2_0.BAEServerInfoFactory;
import org.gtri.gfipm.bae.v2_0.BAEClientInfoFactory;
import org.gtri.gfipm.bae.v2_0.BackendAttribute;
import org.gtri.gfipm.bae.v2_0.BackendAttributeValue;
import org.gtri.gfipm.bae.v2_0.BAEServerException;
import org.gtri.gfipm.bae.v2_0.BAEServerCreationException;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;


/**
 * Data connector implementation that returns staticly defined attributes.
 */
public class GfipmBAEDataConnector extends BaseDataConnector {

    /** Log4j logger. */
    private static Logger log = LoggerFactory.getLogger(GfipmBAEDataConnector.class);

    private String baeURL;
    private String subjectId;
    private String baeEntityId;
    private String myEntityId;
    private int    searchTimeLimit;

    /** bae/return attribute names. */
    private List<BAEAttributeNameMap> baeAttributes;
    private Map<String,String>        baeAttrMap;

    /** Trust material used when connecting to the server over https. */
    private X509Credential  x509Trust;
    private X509Credential  x509Key;
    private X509Certificate myCert;
    private PrivateKey      myKey;
    private List<X509Certificate> serverCerts;

    /** BAE Connector */
    private BAEServerInfo serverInfo;
    private BAEClientInfo clientInfo;
    private BAEServer baeServer;

    /**
     * Constructor.
     * 
     * @param fileAttributes attributes that configure this data connector
     */
    public GfipmBAEDataConnector(String url, String id, String baeId, String myId, int time) {
        baeURL = url;
        subjectId = id;
        baeEntityId = baeId;
        myEntityId  = myId;
        searchTimeLimit = time;
    }

    /** {@inheritDoc} */
    public Map<String, BaseAttribute> resolve(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {
        log.debug("Resolving connector " + getId() + " - Principal - " + resolutionContext.getAttributeRequestContext().getPrincipalName());

        Map<String, BaseAttribute> attribute = new HashMap<String, BaseAttribute>();

        Collection<Object> sourceIdValues = getValuesFromAllDependencies(resolutionContext, subjectId);
        if (sourceIdValues == null || sourceIdValues.isEmpty()) {
            log.debug("Source attribute " + subjectId + " for connector " + getId() +" provide no values");
            return Collections.EMPTY_MAP;
        }

        if (sourceIdValues.size() > 1) {
            log.warn("Source attribute " + subjectId + " for connector " + getId() +" has more than one value.");
        }
        String strPrincipal = sourceIdValues.iterator().next().toString();

        log.debug ("Querying for email: " + strPrincipal );


        SubjectIdentifier identifier = new EmailSubjectIdentifier (strPrincipal);

        try {
           Collection<BackendAttribute> attributes = baeServer.attributeQuery(identifier);

           // TBD Integrate BAE Interface
           for (BackendAttribute a : attributes) {
              String attribName  = baeAttrMap.get (a.getName());
              String attribValue = a.getValue().getStringValue();

              BasicAttribute<String> baXmlAttr = new BasicAttribute<String>();
              baXmlAttr.getValues().add(attribValue);
              attribute.put (attribName, baXmlAttr);
          }
        } catch (BAEServerException e) {
           log.error ("BAE Server Error: {}", e);
        }

        return attribute;
    }


    /**
     * This returns the trust managers that will be used for all TLS and SSL connections to the ldap.
     * 
     * @return <code>TrustManager[]</code>
     */
    public X509Credential getX509Trust() {
        return x509Trust;
    }

    /**
     * This sets the trust manager that will be used for BAE queries.
     * 
     * @param tc <code>X509Credential</code> to create TrustManagers with
     */
    public void setX509Trust(X509Credential tc) {
       /* TBD - based on BAE API */
       x509Trust = tc;
       Collection<X509Certificate> coll = tc.getEntityCertificateChain();
       if (coll instanceof List)
          serverCerts = (List)coll;
       else
          serverCerts = new ArrayList(coll);
    }

    /**
     * This returns the key managers that will be used for all TLS and SSL connections to the ldap.
     * 
     * @return <code>KeyManager[]</code>
     */
    public X509Credential getX509Key() {
        /* TBD - based on BAE API */
        return x509Key;
    }

    /**
     * This sets the key managers that will be used for all TLS/WS-Security activity.
     * 
     * @param kc <code>X509Credential</code> to create KeyManagers with
     */
    public void setX509Key (X509Credential kc) {
         /* TBD - based on BAE API */
         x509Key = kc;
         myKey  = kc.getPrivateKey();
         myCert = kc.getEntityCertificate();
//         log.debug ("myKey = {}",  myKey);
//         log.debug ("myCert = {}", myCert);
    }

    public void setBaeAttributes(List<BAEAttributeNameMap> list) {
       baeAttributes = list;
       baeAttrMap = new HashMap<String,String> ();
       for (BAEAttributeNameMap attrib : list) {
          baeAttrMap.put (attrib.QueryName, attrib.ReturnName);
       }
    }

    public void initialize() {
       serverInfo = BAEServerInfoFactory.getInstance().createBAEServerInfo(baeURL, baeEntityId, serverCerts); 
       clientInfo = BAEClientInfoFactory.getInstance().createBAEClientInfo(myEntityId, myCert, myKey);

       try {
          baeServer = BAEServerFactory.getInstance().createBAEServer(serverInfo, clientInfo);
       } catch (BAEServerCreationException e) {
         log.error ("BAE Server Creation Error: {}", e);
       }
    }


    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {
        // Do nothing
    }
}
