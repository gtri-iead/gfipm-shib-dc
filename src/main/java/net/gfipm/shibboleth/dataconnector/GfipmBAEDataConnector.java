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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AbstractDataConnector;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.idp.attribute.resolver.PluginDependencySupport;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;


import org.opensaml.security.x509.X509Credential;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.gtri.gfipm.bae.v2_0.SubjectIdentifier;
import org.gtri.gfipm.bae.v2_0.EmailSubjectIdentifier;
import org.gtri.gfipm.bae.v2_0.FASCNSubjectIdentifier;
import org.gtri.gfipm.bae.v2_0.PIVUUIDSubjectIdentifier;
import org.gtri.gfipm.bae.v2_0.InvalidFASCNException;
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
import org.gtri.gfipm.bae.v2_0.WebServiceRequestOptions;
import org.gtri.gfipm.bae.v2_0.WebServiceRequestOptionsFactory;

import com.google.common.collect.Lists;


/**
 * Data connector implementation that returns staticly defined attributes.
 */
public class GfipmBAEDataConnector extends AbstractDataConnector {

    /** Log4j logger. */
    @NonnullAfterInit private final Logger log =  LoggerFactory.getLogger(GfipmBAEDataConnector.class);

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
     */
    public GfipmBAEDataConnector() {
    }

    /**
     * Set and Get Methods used by initialization
     */
    public void setSearchTimeLimit(@Nullable int timeout) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        searchTimeLimit = timeout;
    }
    @NonnullAfterInit public int getSearchTimeLimit() {
        return searchTimeLimit;
    }
    public void setBaeUrl(@Nullable String url) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        baeURL = StringSupport.trimOrNull(url);
    }
    @NonnullAfterInit public String getBaeUrl() {
        return baeURL;
    }
    public void setMyEntityId(@Nullable String id) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        myEntityId = StringSupport.trimOrNull(id);
    }
    @NonnullAfterInit public String getMyEntityId() {
        return myEntityId;
    }
    public void setBaeEntityId(@Nullable String id) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        baeEntityId = StringSupport.trimOrNull(id);
    }
    @NonnullAfterInit public String getBaeEntityId() {
        return baeEntityId;
    }
    public void setSubjectId(@Nullable String id) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        subjectId = StringSupport.trimOrNull(id);
    }
    @NonnullAfterInit public String getSubjectId() {
        return subjectId;
    }


    /** 
     * Method for generating a subject identifier.  "Intelligently" determines type
     */
    private SubjectIdentifier GetSubjectIdentifier (String PrincipalName) {

       // Test if it is a PIV-I Style UUID       
       if (PrincipalName.indexOf ("uuid") != -1) {
          log.debug("Principal: " + PrincipalName + " resolved as PIV-I UUID type.");
          String trimmedUUID = PrincipalName.substring(9); // Remove "urn:uuid:" from the beginning.
          log.debug("Trimmed UUID: " + trimmedUUID);
          return new PIVUUIDSubjectIdentifier(trimmedUUID);
       } 

       // Test if it is a FASCN
       try {
          FASCNSubjectIdentifier fascnId = new FASCNSubjectIdentifier(PrincipalName);
          log.debug("Principal: " + PrincipalName + " resolved as FASCN type.");
          return fascnId;
       } catch (InvalidFASCNException e) {
         // Not a valid FASCN so we default to e-mail address.
         log.debug("Principal: " + PrincipalName + " defaulting to E-mail type.");
         return new EmailSubjectIdentifier (PrincipalName);
       }
          
    }

    /** {@inheritDoc} */
    /** {@inheritDoc} */
    @Override
    @Nonnull protected Map<String, IdPAttribute> doDataConnectorResolve(
            @Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final AttributeResolverWorkContext workContext) throws ResolutionException {

        log.debug("Resolving BAE Data Connector.");

        Map<String, IdPAttribute> attribute = new HashMap<String, IdPAttribute>();

        
        final Map<String,List<IdPAttributeValue<?>>> dependencyAttributes = PluginDependencySupport.getAllAttributeValues (workContext, getDependencies());
        if (dependencyAttributes == null || dependencyAttributes.isEmpty()) {
            log.debug("Source attribute " + subjectId + " for connector " + getId() +" provided no values, cannot resolve.");
            return Collections.EMPTY_MAP;
        }

        if (dependencyAttributes.size() > 1) {
            log.warn("Source attribute " + subjectId + " for connector " + getId() +" has more than one value, just using the first.");
        }

        for (final Map.Entry<String, List<IdPAttributeValue<?>>> entry : dependencyAttributes.entrySet()) {
             log.debug("Adding dependency {} to context with {} value(s)", entry.getKey(), entry.getValue());
        }

        String strPrincipal = "blah - gotta look at trace output and figure out what goes here";

        log.debug ("Querying for Id : " + strPrincipal );



        try {
//           SubjectIdentifier identifier = new FASCNSubjectIdentifier (strPrincipal);
           SubjectIdentifier identifier = GetSubjectIdentifier (strPrincipal);

           Collection<BackendAttribute> attributes = baeServer.attributeQuery(identifier);

           // TBD Integrate BAE Interface
           for (BackendAttribute a : attributes) {
              
              // If we find this attribute in our map, then it is an attribute we process 
              if ( baeAttrMap.get (a.getName()) != null ) 
              {
                String attribName  = baeAttrMap.get (a.getName());
                String attribValue = a.getValue().getStringValue();

                List<IdPAttributeValue<String>> baXmlAttr = Lists.newArrayListWithExpectedSize(1);
                baXmlAttr.add(new StringAttributeValue(attribValue));

                final IdPAttribute tempAttribute = new IdPAttribute(attribName);
                tempAttribute.setValues(baXmlAttr);

                attribute.put (tempAttribute.getId(), tempAttribute);
              }
          }
        } catch (BAEServerException e) {
           log.error ("BAE Server Error: {}", e);
        } catch (Exception e) {
           log.error ("Query Failed: {}", e);
        }

        return attribute;
    }


    /**
     * This returns the trust managers that will be used for all TLS and SSL connections to the BAE Responder.
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
     * This returns the key managers that will be used for all TLS and SSL connections to the BAE Responder.
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

    

    @Override
    protected void doInitialize() throws ComponentInitializationException {

        if (null == baeURL) {
            throw new ComponentInitializationException(getLogPrefix() + " No BAE Responder URL found.");
        }
        if (null == baeEntityId) {
            throw new ComponentInitializationException(getLogPrefix() + " No BAE Responder Entity Id found.");
        }
        if (null == serverCerts) {
            throw new ComponentInitializationException(getLogPrefix() + " No BAE Responder Trust Certificate(s) found.");
        }
        if (null == myEntityId) {
            throw new ComponentInitializationException(getLogPrefix() + " No BAE Requester Entity Id found.");
        }
        if (null == myCert) {
            throw new ComponentInitializationException(getLogPrefix() + " No BAE Requester Certificate found.");
        }
        if (null == myKey) {
            throw new ComponentInitializationException(getLogPrefix() + " No BAE Requester Private Key found.");
        }
        if (null == baeAttrMap) {
            throw new ComponentInitializationException(getLogPrefix() + " No Attribute Map found.");
        }
        if (null == subjectId) {
            throw new ComponentInitializationException(getLogPrefix() + " No Subject Identifier attribute found.");
        }

       serverInfo = BAEServerInfoFactory.getInstance().createBAEServerInfo(baeURL, baeEntityId, serverCerts); 
       clientInfo = BAEClientInfoFactory.getInstance().createBAEClientInfo(myEntityId, myCert, myKey);
       Map<String,String> mapOptions = new HashMap<String,String> ();
       mapOptions.put (WebServiceRequestOptions.CLIENT_CERT_AUTH, "false");
       mapOptions.put (WebServiceRequestOptions.SERVER_CERT_AUTH, "false");
       WebServiceRequestOptions wsRequestOptions = WebServiceRequestOptionsFactory.getInstance().createWebServiceRequestOptions(mapOptions);

       try {
          baeServer = BAEServerFactory.getInstance().createBAEServer(serverInfo, clientInfo, wsRequestOptions);
       } catch (BAEServerCreationException e) {
         log.error ("BAE Server Creation Error: {}", e);
         throw new ComponentInitializationException(getLogPrefix() + " BAE Server could not be initialized.");
       }
    }
}
