/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.gfipm.shibboleth.config;

import java.util.Map;
import java.util.List;

import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.security.x509.X509Credential;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.config.attribute.resolver.dataConnector.BaseDataConnectorFactoryBean;
import net.gfipm.shibboleth.dataconnector.GfipmBAEDataConnector;
import net.gfipm.shibboleth.dataconnector.BAEAttributeNameMap;

/**
 * Spring bean factory that produces {@link GfipmTestDataConnector}s.
 */
public class GfipmBAEDataConnectorFactoryBean extends BaseDataConnectorFactoryBean {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(GfipmBAEDataConnectorFactoryBean.class);

    private String baeURL;
    private String subjectId;
    private String baeEntityId;
    private String myEntityId;
    private int    searchTimeLimit;

    /** bae/return attribute names. */
    private List<BAEAttributeNameMap> baeAttributes;

    /** Trust material used when connecting to the server over https. */
    private X509Credential trustCredential;

    /** Client authentication material used when using client certificate auth. */
    private X509Credential myCredential;


    /** {@inheritDoc} */
    public Class getObjectType() {
        return GfipmBAEDataConnector.class;
    }
    
    /** GFIPM Test Data Connector specific properties. */
    private Map<String, String> baeProperties;


    /**
     * Gets the BAE provider specific properties.
     *
     * @return File specific properties
     */
    public Map<String, String> getBaeDataProperties() {
        return baeProperties;
    }

    /**
     * Sets the BAE specific properties.
     *
     * @param properties File specific properties
     */
    public void setBaeDataProperties(Map<String, String> properties) {
        baeProperties = properties;
    }


    public int getSearchTimeLimit () {
        return searchTimeLimit;
    }
    public void setSearchTimeLimit (int time) {
        searchTimeLimit = time; 
    }
    public String getSubjectId () {
        return subjectId;
    }
    public void setSubjectId (String id) {
        subjectId = id;
    }
    public String getBaeURL () {
        return baeURL;
    }
    public void setBaeURL (String url) {
        baeURL = url; 
    }
    public String getBaeEntityId () {
        return baeEntityId;
    }
    public void setBaeEntityId (String id) {
        baeEntityId = id;
    }
    public String getMyEntityId () {
        return myEntityId;
    }
    public void setMyEntityId (String id) {
        myEntityId = id;
    }
    public X509Credential getMyCredential () {
        return myCredential;
    }
    public void setMyCredential (X509Credential cred) {
        myCredential = cred;
    }
    public X509Credential getTrustCredential () {
        return trustCredential;
    }
    public void setTrustCredential (X509Credential cred) {
        trustCredential = cred;
    }
    public void setBaeAttributes(List<BAEAttributeNameMap> list) {
        baeAttributes = list;
    }
    public List<BAEAttributeNameMap> getRwsAttributes() {
        return baeAttributes;
    }
    

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        GfipmBAEDataConnector connector = new GfipmBAEDataConnector(baeURL, subjectId, baeEntityId, myEntityId, searchTimeLimit);
        populateDataConnector(connector);

        connector.setX509Trust     (trustCredential);
        connector.setX509Key       (myCredential);
        connector.setBaeAttributes (baeAttributes);

        log.debug("BaeDataConnectorFactoryBean initializing.");

        connector.initialize ();
         
        return connector;
    }
}
