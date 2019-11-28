//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.8-b130911.1802 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2019.11.28 à 02:57:09 PM CET 
//


package org.lsc.plugins.connectors.msgraphapi.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour anonymous complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;extension base="{http://lsc-project.org/XSD/lsc-microsoft-graph-api-plugin-1.0.xsd}msGraphApiService">
 *       &lt;sequence>
 *         &lt;element name="filter" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="pivot" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="pageSize" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="select" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "filter",
    "pivot",
    "pageSize",
    "select"
})
@XmlRootElement(name = "msGraphApiUsersService", namespace = "http://lsc-project.org/XSD/lsc-microsoft-graph-api-plugin-1.0.xsd")
public class MsGraphApiUsersService
    extends MsGraphApiService
{

    @XmlElement(namespace = "http://lsc-project.org/XSD/lsc-microsoft-graph-api-plugin-1.0.xsd")
    protected String filter;
    @XmlElement(namespace = "http://lsc-project.org/XSD/lsc-microsoft-graph-api-plugin-1.0.xsd")
    protected String pivot;
    @XmlElement(namespace = "http://lsc-project.org/XSD/lsc-microsoft-graph-api-plugin-1.0.xsd")
    protected Integer pageSize;
    @XmlElement(namespace = "http://lsc-project.org/XSD/lsc-microsoft-graph-api-plugin-1.0.xsd")
    protected String select;

    /**
     * Obtient la valeur de la propriété filter.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFilter() {
        return filter;
    }

    /**
     * Définit la valeur de la propriété filter.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFilter(String value) {
        this.filter = value;
    }

    /**
     * Obtient la valeur de la propriété pivot.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPivot() {
        return pivot;
    }

    /**
     * Définit la valeur de la propriété pivot.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPivot(String value) {
        this.pivot = value;
    }

    /**
     * Obtient la valeur de la propriété pageSize.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getPageSize() {
        return pageSize;
    }

    /**
     * Définit la valeur de la propriété pageSize.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setPageSize(Integer value) {
        this.pageSize = value;
    }

    /**
     * Obtient la valeur de la propriété select.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSelect() {
        return select;
    }

    /**
     * Définit la valeur de la propriété select.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSelect(String value) {
        this.select = value;
    }

}
