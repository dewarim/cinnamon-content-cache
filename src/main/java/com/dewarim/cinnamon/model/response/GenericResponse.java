package com.dewarim.cinnamon.model.response;

import java.util.Objects;

/**
 * A simple class to report the success or failure of an operation.
 * (Just using http status code 204 is a little ambiguous)
 */
public class GenericResponse {
    
    private String message;
    private boolean successful;

    public GenericResponse() {
    }

    public GenericResponse(boolean successful) {
        this.successful = successful;
    }

    public GenericResponse(String message, boolean successful) {
        this.message = message;
        this.successful = successful;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GenericResponse that = (GenericResponse) o;
        return successful == that.successful &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {

        return Objects.hash(message, successful);
    }
}
