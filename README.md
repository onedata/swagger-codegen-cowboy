# Swagger codegen for the Erlang Cowboy server library

## Overview
This is a code generator for Erlang [Cowboy](https://github.com/ninenines/cowboy) server, used by [Onedata](https://onedata.org).

## What's Swagger?
The goal of Swagger™ is to define a standard, language-agnostic interface to REST APIs which allows both humans and computers to discover and understand the capabilities of the service without access to source code, documentation, or through network traffic inspection. When properly defined via Swagger, a consumer can understand and interact with the remote service with a minimal amount of implementation logic. Similar to what interfaces have done for lower-level programming, Swagger removes the guesswork in calling the service.


Check out [OpenAPI-Spec](https://github.com/OAI/OpenAPI-Specification) for additional information about the Swagger project, including additional libraries with support for other languages and more. 

## How do I use this?

### Building

```bash
mvn assembly:assembly -DdescriptorId=jar-with-dependencies
```

### Running
The library assumes that the current directory contains 2 files:
* `rest_api.mustache` - a Mustache template for generating Cowboy routers and handlers
* `rest_model.mustache` - a Mustache template for generating Erlang data types for input and output data models

```bash
java -jar target/cowboy-swagger-codegen-1.0.0-jar-with-dependencies.jar generate -l cowboy -i ./swagger.json -o ./generated/cowboy
```

### Mustache templates
This generator, as well as it's templates supports several Swagger vendor extensions, some specific to Erlang and some specific to Onedata:
* `x-discriminator` - determines the name of the discriminator field in inherited data types
* `x-erlang-datatype` - allows to specify that a property has some specific Erlang datatype, unsupported by JSON (e.g. `atom`)
* `x-onedata-version` - version of the API
* `x-onedata-module` - the module implementing handler for specific operation
* `x-onedata-resource` - the resource related to specific operation
* `x-onedata-datatype` - Onedata specific datatype
* `x-onedata-dictionary-value-type` - determines whether the property should be mapped to a map

## License

See [LICENSE](./LICENSE)
