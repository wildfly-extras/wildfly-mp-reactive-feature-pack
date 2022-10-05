package org.wildfly.test.integration.microprofile.context.propagation.rest;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class RequestBean {

    public long id() {
        return System.identityHashCode(this);
    }
}
