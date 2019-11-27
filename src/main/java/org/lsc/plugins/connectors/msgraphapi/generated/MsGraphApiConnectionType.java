//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.8-b130911.1802 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2019.11.27 à 10:52:02 AM CET 
//


package org.lsc.plugins.connectors.msgraphapi.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.lsc.configuration.ConnectionType;


/**
 * <p>Classe Java pour msGraphApiConnectionType complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="msGraphApiConnectionType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://lsc-project.org/XSD/lsc-core-2.1.xsd}connectionType">
 *       &lt;sequence>
 *         &lt;element name="clientId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="clientSecret" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="tenant" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "msGraphApiConnectionType", namespace = "http://lsc-project.org/XSD/lsc-microsoft-graph-api-plugin-1.0.xsd", propOrder = {
    "clientId",
    "clientSecret",
    "tenant"
})
public class MsGraphApiConnectionType
    extends ConnectionType
{

    @XmlElement(required = true)
    protected String clientId;
    @XmlElement(required = true)
    protected String clientSecret;
    @XmlElement(required = true)
    protected String tenant;

    /**
     * Obtient la valeur de la propriété clientId.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Définit la valeur de la propriété clientId.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setClientId(String value) {
        this.clientId = value;
    }

    /**
     * Obtient la valeur de la propriété clientSecret.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * Définit la valeur de la propriété clientSecret.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setClientSecret(String value) {
        this.clientSecret = value;
    }

    /**
     * Obtient la valeur de la propriété tenant.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTenant() {
        return tenant;
    }

    /**
     * Définit la valeur de la propriété tenant.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTenant(String value) {
        this.tenant = value;
    }

}
