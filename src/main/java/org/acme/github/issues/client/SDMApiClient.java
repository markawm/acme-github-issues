package org.acme.github.issues.client;

import javax.json.JsonObject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import org.acme.github.issues.model.SDMAuth;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@Consumes("application/json")
@Produces("application/json")
public interface SDMApiClient {

    @POST
    @Path("/a/{accountName}/graphql")
    JsonObject executeQuery(@PathParam ("accountName") String accountName, @BeanParam SDMAuth sdmAuth, JsonObject body) throws
            WebApplicationException;
}
