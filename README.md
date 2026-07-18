# JokesAPI

JokesAPI - RESTful API endpoints to create, update &amp; get jokes and its jokesters. AI coded using Github Copilot. Built using Java 25, Spring Boot and Redis -backed in-memory DB loaded from `data/` JSON files.
Built using GitHub Copilot and IDE Visual Studio Code.

## What's implemented:
- a RESTful API with full CRUD capabilities on endpoints /v1/jokes as well as /v1/jokesters
- The API’s service and repository layers create and manage in memory DB using the JSON files in /data folder
- Service layer uses Redis memory DB found on 127.0.0.1:6379
- On application startup, data from `data/jokes.json` and `data/jokesters.json` is loaded into Redis on startup, ready for use by endpoints.
- HATEOAS links are included in responses.
- Use `Idempotency-Key` header to assist safe retries (basic support).
- Finally, API uses structured error format for errors which includes problem details
- The CRUD operations follows standard HTTP methods - all HTTP methods except PATCH implemented. API uses **Upserts** when implementing PUT calls

## Prompts:
* Build using Java 25 and Spring Boot, a RESTful API with full CRUD capabilities to perform CRUD operations on jokes as well as its related jokesters. The two endpoint nouns are jokes and jokesters. Sample jokes and jokesters data found in ./data/*.json files. The API’s service and repository layers will create and manage in memory DB using the JSON files data and inherent relational structure. It will use Redis memory DB found on 127.0.0.1:6379. On application startup, the JSON data will be copied to local Redis DB, ready for use by endpoints. Use latest REST API standards including no deep nesting in URLs, use of idempotency headers to safely handle retries, API versioning (i.e., /v1 prefix) and naming conventions such as plural nouns. Also implement HATEOAS to help navigate between resources using links provided in responses. Finally, use structured error format for errors which includes problem details. The CRUD operations follow standard HTTP methods. Implement for all HTTP methods except PATCH. Allow upserts when implementing PUT calls.
* implement cursor based pagination for both get all jokes and get all jokesters endpoints
* lets write unit tests for the project. Closely look at code logic and decide which needs tests. Ensure testing for all test cases including edge cases. Code coverage is also important but whats more important is quality of tests

## Coding LLMs:
GPT-5 mini <br/>
MAI-Code-1-Flash <br/>

## Build steps:

```bash
mvn -U -DskipTests package
```

## Run

```bash
java -jar target/jokesapi-0.1.0.jar
```

## Requirements
- Java 25
- Redis running on `127.0.0.1:6379`

## API
- Base path: `/v1`
- Resources: `/v1/jokes`, `/v1/jokesters`
