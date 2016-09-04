# Swagger Codegen for the CowboyServerCodegen library

## Overview
This is a code generator for Erlang Cowboy server, used by [Onedata](https://onedata.org).

## What's Swagger?
The goal of Swagger™ is to define a standard, language-agnostic interface to REST APIs which allows both humans and computers to discover and understand the capabilities of the service without access to source code, documentation, or through network traffic inspection. When properly defined via Swagger, a consumer can understand and interact with the remote service with a minimal amount of implementation logic. Similar to what interfaces have done for lower-level programming, Swagger removes the guesswork in calling the service.


Check out [OpenAPI-Spec](https://github.com/OAI/OpenAPI-Specification) for additional information about the Swagger project, including additional libraries with support for other languages and more. 

## How do I use this?

### Building
```bash
mvn assembly:assembly -DdescriptorId=jar-with-dependencies
```

### Running
```bash
java -jar target/cowboy-swagger-codegen-1.0.0-jar-with-dependencies.jar generate -l cowboy -i ./swagger.json -o ./generated/cowboy
```

