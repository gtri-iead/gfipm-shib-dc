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

import org.opensaml.xml.util.DatatypeHelper;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.config.attribute.resolver.dataConnector.BaseDataConnectorFactoryBean;
import net.gfipm.shibboleth.dataconnector.GfipmTestDataConnector;

/**
 * Spring bean factory that produces {@link GfipmTestDataConnector}s.
 */
public class GfipmTestDataConnectorFactoryBean extends BaseDataConnectorFactoryBean {

    /** Path to the Directory with GFIPM Metadata. */
    private String pathToAttributeFiles;
    private String uidAttribute;

    /** {@inheritDoc} */
    public Class getObjectType() {
        return GfipmTestDataConnector.class;
    }
    
    /** GFIPM Test Data Connector specific properties. */
    private Map<String, String> testProperties;


    /**
     * Gets the File provider specific properties.
     *
     * @return File specific properties
     */
    public Map<String, String> getTestDataProperties() {
        return testProperties;
    }

    /**
     * Sets the File specific properties.
     *
     * @param properties File specific properties
     */
    public void setTestDataProperties(Map<String, String> properties) {
        testProperties = properties;
    }


    /**
     * Gets the user id atribute name returned by the created data connector.
     * 
     * @return uid attributes returned by the created data connector
     */
    public String getUidAttribute () {
        return uidAttribute;
    }

    /**
     * Sets the file attributes returned by the created data connector.
     * 
     * @param attributes uidAttrName returned by the created data connector
     */
    public void setUidAttribute (String uidAttrName) {
        uidAttribute = DatatypeHelper.safeTrimOrNullString(uidAttrName);
    }

    /**
     * Gets the file attributes returned by the created data connector.
     * 
     * @return file attributes returned by the created data connector
     */
    public String getPathToAttributeFiles () {
        return pathToAttributeFiles;
    }

    /**
     * Sets the file attributes returned by the created data connector.
     * 
     * @param attributes file attributes returned by the created data connector
     */
    public void setPathToAttributeFiles (String path) {
        pathToAttributeFiles = DatatypeHelper.safeTrimOrNullString(path);
    }

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        GfipmTestDataConnector connector = new GfipmTestDataConnector(pathToAttributeFiles, uidAttribute);
        populateDataConnector(connector);

        return connector;
    }
}
