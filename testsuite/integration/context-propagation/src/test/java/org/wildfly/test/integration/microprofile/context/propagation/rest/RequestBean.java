package org.wildfly.test.integration.microprofile.context.propagation.rest;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;

@RequestScoped
public class RequestBean {

    public long id() {
        return System.identityHashCode(this);
    }

    @PostConstruct
    public void postConstruct() {
        System.out.println("====> postConstruct " + this);
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println("====> preDestroy " + this);
    }
}
