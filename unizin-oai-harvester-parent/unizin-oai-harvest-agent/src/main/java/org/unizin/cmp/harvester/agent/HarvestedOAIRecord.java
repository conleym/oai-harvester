package org.unizin.cmp.harvester.agent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

/**
 * DynamoDB mapped harvested record.
 * <p>
 * Getters and setters are required by the DynamoDB wrapper.
 *
 */
@DynamoDBTable(tableName = HarvestedOAIRecord.TABLE_NAME)
public final class HarvestedOAIRecord {
    public static final String TABLE_NAME = "HarvestedOAIRecords";
    public static final String OAI_ID_ATTRIB = "oai-identifier";
    public static final String DATESTAMP_ATTRIB = "datestamp";
    public static final String SETS_ATTRIB = "sets";
    public static final String BASE_URL_ATTRIB = "base-url";
    public static final String XML_ATTRIB = "xml";
    public static final String CHECKSUM_ATTRIB = "xml-checksum";
    public static final String STATUS_ATTRIB = "status";


    @DynamoDBAttribute(attributeName = BASE_URL_ATTRIB)
    @DynamoDBHashKey
    private String baseURL;

    @DynamoDBAttribute(attributeName = OAI_ID_ATTRIB)
    @DynamoDBRangeKey
    private String identifier;

    @DynamoDBAttribute(attributeName = SETS_ATTRIB)
    private Set<String> sets;

    @DynamoDBIndexRangeKey(localSecondaryIndexName = "DatestampIndex")
    @DynamoDBAttribute(attributeName = DATESTAMP_ATTRIB)
    private String datestamp;

    @DynamoDBAttribute(attributeName = XML_ATTRIB)
    private byte[] xml;

    @DynamoDBAttribute(attributeName = CHECKSUM_ATTRIB)
    private byte[] checksum;

    @DynamoDBAttribute(attributeName = STATUS_ATTRIB)
    private String status;

    public String getBaseURL() {
        return baseURL;
    }

    public void setBaseURL(final String baseURL) {
        this.baseURL = baseURL;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(final String identifier) {
        this.identifier = identifier;
    }

    public void addSet(final String set) {
        if (sets == null) {
            sets = new HashSet<String>();
        }
        sets.add(set);
    }

    public Set<String> getSets() {
        return sets;
    }

    public void setSets(final Set<String> sets) {
        this.sets = sets;
    }

    public String getDatestamp() {
        return datestamp;
    }

    public void setDatestamp(final String datestamp) {
        this.datestamp = datestamp;
    }

    public byte[] getXml() {
        return xml;
    }

    /**
     * Set the XML.
     * <p>
     * Note that this does <em>not</em> change the checksum. There are three
     * reasons for this:
     * <ol>
     * <li>The DynamoDB mapper uses {@link #setChecksum(byte[])} to set the
     * checksum when it reads data from the database, so every read would
     * require recomputing the checksum. Since the mapper will read and set the
     * checksum itself, this is wasteful.</li>
     * <li>It would introduce an asymmetry with {@link #setChecksum(byte[])},
     * which cannot change the XML to match the checksum it receives.</li>
     * <li>It would introduce a potential source of inconsistency caused by #2:
     * Suppose the checksum in the database was somehow different from that
     * which we would compute here, due to programmer error or perhaps data
     * corruption. Depending upon the order in which the mapper sets the XML and
     * the checksum (it will always set both to the values it got from the
     * database), we may have either the correct checksum that we computed or
     * the incorrect one from the database.</li>
     * </ol>
     *
     * @param xml
     *            the record's XML as bytes.
     */
    public void setXml(final byte[] xml) {
        this.xml = xml;
    }

    public byte[] getChecksum() {
        return checksum;
    }

    public void setChecksum(final byte[] checksum) {
        this.checksum = checksum;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((baseURL == null) ? 0 : baseURL.hashCode());
        result = prime * result + Arrays.hashCode(checksum);
        result = prime * result + ((datestamp == null) ? 0 : datestamp.hashCode());
        result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
        result = prime * result + ((sets == null) ? 0 : sets.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        result = prime * result + Arrays.hashCode(xml);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HarvestedOAIRecord other = (HarvestedOAIRecord) obj;
        if (baseURL == null) {
            if (other.baseURL != null)
                return false;
        } else if (!baseURL.equals(other.baseURL))
            return false;
        if (!Arrays.equals(checksum, other.checksum))
            return false;
        if (datestamp == null) {
            if (other.datestamp != null)
                return false;
        } else if (!datestamp.equals(other.datestamp))
            return false;
        if (identifier == null) {
            if (other.identifier != null)
                return false;
        } else if (!identifier.equals(other.identifier))
            return false;
        if (sets == null) {
            if (other.sets != null)
                return false;
        } else if (!sets.equals(other.sets))
            return false;
        if (status == null) {
            if (other.status != null)
                return false;
        } else if (!status.equals(other.status))
            return false;
        if (!Arrays.equals(xml, other.xml))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " [baseURL=" + baseURL +
                ", identifier=" + identifier + ", sets=" + sets +
                ", datestamp=" + datestamp + ", xml=" + Arrays.toString(xml) +
                ", checksum=" + Arrays.toString(checksum) + ", status="+ status
                + "]";
    }
}
