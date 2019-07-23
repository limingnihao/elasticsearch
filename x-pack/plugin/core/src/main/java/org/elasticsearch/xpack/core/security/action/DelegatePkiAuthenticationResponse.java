/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.core.security.action;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.unit.TimeValue;

import java.io.IOException;
import java.util.Objects;

public final class DelegatePkiAuthenticationResponse extends ActionResponse {

    private String tokenString;
    private TimeValue expiresIn;

    DelegatePkiAuthenticationResponse() { }

    public DelegatePkiAuthenticationResponse(String tokenString, TimeValue expiresIn) {
        this.tokenString = Objects.requireNonNull(tokenString);
        this.expiresIn = Objects.requireNonNull(expiresIn);
    }

    public DelegatePkiAuthenticationResponse(StreamInput input) throws IOException {
        super(input);
        tokenString = input.readString();
        expiresIn = input.readTimeValue();
    }

    public String getTokenString() {
        return tokenString;
    }

    public TimeValue getExpiresIn() {
        return expiresIn;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(tokenString);
        out.writeTimeValue(expiresIn);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DelegatePkiAuthenticationResponse that = (DelegatePkiAuthenticationResponse) o;
        return Objects.equals(tokenString, that.tokenString) &&
            Objects.equals(expiresIn, that.expiresIn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenString, expiresIn);
    }
}
