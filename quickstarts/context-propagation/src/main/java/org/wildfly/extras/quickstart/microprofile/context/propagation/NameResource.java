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

package org.wildfly.extras.quickstart.microprofile.context.propagation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@RequestScoped
@Path("/")
public class NameResource {

    @PersistenceContext(unitName = "quickstart")
    EntityManager em;

    @Inject
    ThreadContext threadContext;

    @Inject
    ManagedExecutor executor;

    private String[] forenames = new String[]{
            "Albert", "Ben", "Charles", "Daisy", "Emma", "Frank", "George", "Harriet",
            "Inger", "James", "Kabir", "Lisa", "Mona", "Oscar", "Pedro", "Quincy",
            "Rose", "Stuart", "Tom", "Umberto", "Victor"};
    private String[] surnames = new String[]{
            "Ascot", "Bell", "Crop", "Dane", "Energy", "Food", "Goose", "Hope",
            "India", "Jones", "Khan", "Lisp", "Machine", "Ozone", "Pewter", "Quo",
            "Roof", "Samson", "Tidy", "Umbrella", "Vince", "X-Ray", "Yoyo", "Zinc"};

    @Transactional
    @GET
    @Path("/names-tc")
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<List<Name>> readAndStoreNamesTc() {
        return threadContext.withContextCapture(generateNamesCompletionStage(3))
                .thenApplyAsync(
                        list -> {
                            for (Name name : list) {
                                // Context propagation makes this block inherit the transaction of the caller
                                System.out.println("Storing name: " + name);
                                // here we are all in the same transaction
                                // thanks to context propagation
                                em.persist(name);
                            }
                            return list;
                        },
                        Executors.newCachedThreadPool());
    }

    @Transactional
    @GET
    @Path("/names-me")
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<List<Name>> readAndStoreNamesMe() {
        CompletionStage<List<Name>> future = executor.completedStage(new ArrayList<>());
        future.thenApply(list -> {
            for (Name name : generateNames(3)) {
                // Context propagation makes this block inherit the transaction of the caller
                System.out.println("Storing name: " + name);
                // here we are all in the same transaction
                // thanks to context propagation
                em.persist(name);
                list.add(name);
            }
            return list;
        });
        return future;
    }

    @GET
    @Path("/names/all")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Name> getAllNames() {
        TypedQuery<Name> query = em.createQuery("SELECT n from Name n", Name.class);
        return query.getResultList();
    }

    CompletionStage<List<Name>> generateNamesCompletionStage(int number) {
        CompletableFuture<List<Name>> future = new CompletableFuture<>();
        Executors.newCachedThreadPool().submit(() -> {
            try {
                future.complete(generateNames(number));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    private List<Name> generateNames(int number) {
        List<Name> list = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Name name = new Name(getRandomElement(forenames), getRandomElement(surnames));
            list.add(name);
        }
        return list;
    }

    private String getRandomElement(String[] arr) {
        if (arr.length == 0) {
            return null;
        }
        int index = (int)(Math.random() * arr.length);
        return arr[index];
    }

}
