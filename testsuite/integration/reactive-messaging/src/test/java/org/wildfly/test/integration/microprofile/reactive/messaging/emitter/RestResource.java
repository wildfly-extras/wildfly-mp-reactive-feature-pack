/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.emitter;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@Path("/")
public class RestResource {
    @Inject
    RsoMessagingBean bean;

    @POST
    @Path("/push/{value}")
    public Response addValue(@PathParam("value") String value) {
        bean.addValue(value);
        // Return an empty string so that the HttpClient used for convenience in the tests does not choke
        return Response.ok("").build();
    }

    @GET
    @Path("/values")
    public Response getValues() {
        String values = bean.getFormattedValues();
        return Response.ok(values).build();
    }
}
