package org.acme.github.issues.model;

import javax.ws.rs.HeaderParam;

public class SDMAuth {

    private String jwtToken;

    public SDMAuth(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    @HeaderParam("Authorization")
    public String getAuthorization() {
        return "Bearer " + jwtToken;
    }

}
