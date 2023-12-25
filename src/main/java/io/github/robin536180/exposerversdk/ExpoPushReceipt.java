package io.github.robin536180.exposerversdk;

import com.fasterxml.jackson.annotation.*;
import io.github.robin536180.exposerversdk.enums.ReceiptError;
import io.github.robin536180.exposerversdk.enums.Status;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"_debug"})
public class ExpoPushReceipt {

    private String id = null;
    private Status status = null;
    private String message = null;
    private Details details = null;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public String getId()  {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Details getDetails() {
        return details;
    }

    public void setDetails(Details details) {
        this.details = details;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }



    @Override
    public boolean equals(Object _o) {
        if (_o == null)
            return false;

        if (_o == this)
            return true;

        if (_o.getClass() != getClass())
            return false;

        ExpoPushReceipt o = (ExpoPushReceipt) _o;
        return new EqualsBuilder().
                append(status, o.status).
                append(id, o.id).
                append(message, o.message).
                append(details, o.details).
                isEquals();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, getStatus(), getMessage(), getDetails(), getAdditionalProperties());
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties({"apns", "fcm"})
    public static class Details {

        private ReceiptError error;
        private Integer sentAt;
        @JsonIgnore
        private Map<String, Object> additionalProperties = new HashMap<String, Object>();

        public ReceiptError getError() {
            return error;
        }

        public Details setError(ReceiptError error) {
            this.error = error;
            return this;
        }

        public Integer getSentAt() {
            return sentAt;
        }

        public void setSentAt(Integer sentAt) {
            this.sentAt = sentAt;
        }

        @JsonAnyGetter
        public Map<String, Object> getAdditionalProperties() {
            return this.additionalProperties;
        }

        @JsonAnySetter
        public void setAdditionalProperty(String name, Object value) {
            this.additionalProperties.put(name, value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Details)) return false;
            Details details = (Details) o;
            return Objects.equals(getError(), details.getError()) &&
                    Objects.equals(getSentAt(), details.getSentAt()) &&
                    Objects.equals(getAdditionalProperties(), details.getAdditionalProperties());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getError(), getSentAt(), getAdditionalProperties());
        }
    }
}