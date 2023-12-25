package io.github.robin536180.exposerversdk.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Status {
        @JsonProperty("ok")
        OK("ok"),
        @JsonProperty("error")
        ERROR("error");

        private String status;
        private Status(String status) {
                this.status = status;
        }

        @Override
        public String toString(){
                return status;
        }
}
