package com.example.jokesapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

public class Joke {
    @NotBlank(groups = {Update.class}) // id is required for updates, but not for creation
    private String id;
   
    private String setup;

    @NotBlank
    @JsonProperty("joke") //added this fix the com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException: Unrecognized field error when reading jokes.json
    private String punchline;

    @JsonProperty("jokesterid")
    @NotBlank
    private String jokesterId;

    public Joke() {}

    public Joke(String id, String setup, String punchline, String jokesterId) {
        this.id = id;
        this.setup = setup;
        this.punchline = punchline;
        this.jokesterId = jokesterId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSetup() { return setup; }
    public void setSetup(String setup) { this.setup = setup; }
    public String getPunchline() { return punchline; }
    public void setPunchline(String punchline) { this.punchline = punchline; }
    public String getJokesterId() { return jokesterId; }
    public void setJokesterId(String jokesterId) { this.jokesterId = jokesterId; }

    public interface Update {}
}
