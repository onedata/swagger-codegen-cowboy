package org.onedata.swagger;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.google.common.base.Joiner;
import io.swagger.codegen.*;
import io.swagger.codegen.examples.ExampleGenerator;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Response;
import io.swagger.models.parameters.*;
import io.swagger.models.properties.*;
import io.swagger.models.Swagger;
import io.swagger.util.Yaml;

import java.io.File;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CowboyServerCodegen extends DefaultCodegen implements CodegenConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(CowboyServerCodegen.class);
    protected String gemName;
    protected String moduleName;
    protected String libFolder = "lib";

    public CowboyServerCodegen() {
        super();
        outputFolder = "generated-code" + File.separator + "cowboy";

        // no model
        modelTemplateFiles.clear(); //put("rest_model.mustache", ".erl");
        
        apiTemplateFiles.put("rest_api.mustache", ".erl");
        
        embeddedTemplateDir = templateDir = "./";

        typeMapping.clear();
        languageSpecificPrimitives.clear();

        setReservedWordsLowerCase(
                Arrays.asList(
                    "after", "and", "andalso", "band", "begin", "bnot", "bor", "bsl", 
                    "bsr", "bxor", "case", "catch", "cond", "div", "end", "fun", "if", 
                    "let", "not", "of", "or", "orelse", "receive", "rem", "try", "when", "xor")
        );

        languageSpecificPrimitives.add("int");
        languageSpecificPrimitives.add("array");
        languageSpecificPrimitives.add("map");
        languageSpecificPrimitives.add("string");
        languageSpecificPrimitives.add("atom");
        languageSpecificPrimitives.add("float");
        languageSpecificPrimitives.add("boolean");



        typeMapping.put("string", "string");
        typeMapping.put("ByteArray", "string");
        typeMapping.put("boolean", "boolean");
        typeMapping.put("double", "float");
        typeMapping.put("float", "float");
        typeMapping.put("string", "string");
        typeMapping.put("integer", "integer");
        typeMapping.put("long", "long");
        typeMapping.put("map", "map");
        typeMapping.put("number", "number");

        // remove modelPackage and apiPackage added by default
        cliOptions.clear();
    }

    @Override
    public void processOpts() {
        super.processOpts();

        // use constant model/api package (folder path)
        //setModelPackage("models");
        //setApiPackage("api");

        supportingFiles.add(new SupportingFile("rest_model.mustache", "", "rest_model.erl"));

    }

    @Override
    public String removeNonNameElementToCamelCase(String name) {
        return removeNonNameElementToCamelCase(name, "[-:;#]");
    }

    @Override
    public String generateExamplePath(String path, Operation operation) {
        StringBuilder sb = new StringBuilder();
        sb.append(path);

        if (operation.getParameters() != null) {
            int count = 0;

            for (Parameter param : operation.getParameters()) {
                if (param instanceof QueryParameter) {
                    StringBuilder paramPart = new StringBuilder();
                    QueryParameter qp = (QueryParameter) param;

                    if (count == 0) {
                        paramPart.append("?");
                    } else {
                        paramPart.append(",");
                    }
                    count += 1;
                    if (!param.getRequired()) {
                        paramPart.append("[");
                    }
                    paramPart.append(param.getName()).append("=");
                    paramPart.append(":"); // start of parameter
                    if (qp.getCollectionFormat() != null) {
                        paramPart.append(param.getName() + "1");
                        if ("csv".equals(qp.getCollectionFormat())) {
                            paramPart.append(",");
                        } else if ("pipes".equals(qp.getCollectionFormat())) {
                            paramPart.append("|");
                        } else if ("tsv".equals(qp.getCollectionFormat())) {
                            paramPart.append("\t");
                        } else if ("multi".equals(qp.getCollectionFormat())) {
                            paramPart.append("&").append(param.getName()).append("=");
                            paramPart.append(param.getName() + "2");
                        }
                    } else {
                        paramPart.append(param.getName());
                    }
                    paramPart.append(""); // end of parameter
                    if (!param.getRequired()) {
                        paramPart.append("]");
                    }
                    sb.append(paramPart.toString());
                }
            }
        }

        return sb.toString();

    }

    //
    // Convert an HTTP path to a Cowboy route, i.e.:
    // /a/b/:c/e/:d instead of /a/b/{c}/e/{d}
    //
    private String pathToCowboyRoute(String path, List<CodegenParameter> pathParams) {
        // Map the capture params by their names.
        HashMap<String, String> captureTypes = new HashMap<String, String>();
        for (CodegenParameter param : pathParams) {
            captureTypes.put(param.baseName, param.dataType);
        }

        // Cut off the leading slash, if it is present.
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        // Convert the path into a list of servant route components.
        List<String> pathComponents = new ArrayList<String>();
        for (String piece : path.split("/")) {
            if (piece.startsWith("{") && piece.endsWith("}")) {
                String name = piece.substring(1, piece.length() - 1);
                //pathComponents.add("Capture \"" + name + "\" " + captureTypes.get(name));
                pathComponents.add(":"+name);
            } else {
                pathComponents.add(piece);
            }
        }

        return "/"+ Joiner.on("/").join(pathComponents);
    }

    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation, Map<String, Model> definitions, Swagger swagger) {
        CodegenOperation op = super.fromOperation(path, httpMethod, operation, definitions, swagger);

        op.path = pathToCowboyRoute(path, op.pathParams);

        return op;
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    @Override
    public String getName() {
        return "cowboy";
    }

    @Override
    public String getHelp() {
        return "Generates an Erlang Cowboy server library.";
    }

    @Override
    public String escapeReservedWord(String name) {
        return "_" + name;
    }

    @Override
    public String apiFileFolder() {
        return outputFolder + File.separator + apiPackage.replace("/", File.separator);
    }

    public String getTypeDeclarationNoOptionalCheck(Property p) {
        if(p instanceof ObjectProperty && p.getName()!=null) {
            //return toModelName(((ObjectProperty)p).getType())+"_model()";
            //ObjectProperty op = (ObjectProperty)p;
            //return toModelName(op.getName()) + "_model()"
            String result = "#{ ";
            for(String subproperty_key : ((ObjectProperty) p).getProperties().keySet()) {
                Property subproperty = ((ObjectProperty) p).getProperties().get(subproperty_key);
                result = toVarName(subproperty.getName()) + " => " + getTypeDeclaration(subproperty) +",";
            }
            return result;
        }
        else if (p instanceof RefProperty) {
            RefProperty rp = (RefProperty)p;
            return toModelName(rp.get$ref().substring(rp.get$ref().lastIndexOf('/') + 1))+"_model()";
        }
        else if (p instanceof ArrayProperty) {
            ArrayProperty ap = (ArrayProperty) p;
            Property inner = ap.getItems();
            return "[" + getTypeDeclaration(inner.getType()) + "]";
        }
        else if (p instanceof MapProperty) {
            MapProperty mp = (MapProperty) p;
            Property inner = mp.getAdditionalProperties();
            //return getSwaggerType(p) + "[string," + getTypeDeclaration(inner) + "]";
            return "#{ '_' => " + getTypeDeclaration(inner) + "}";
        }
        else if(p instanceof StringProperty) {
            StringProperty sp = (StringProperty)p;
            return "string";
        }
        /*else if(p instanceof StringProperty) {
            if( (((StringProperty)p).getEnum() != null) && (!((StringProperty)p).getEnum().isEmpty()) ) {
                return "atom";
            }
            else {
                return "string";
            }
        }*/
        else if(p.getVendorExtensions().containsKey("x-erlang-datatype")) {
            return (String)p.getVendorExtensions().get("x-erlang-datatype");
        }
        else {
            return super.getTypeDeclaration(p); //+"__"+p.getClass().toString();
        }
    }

    @Override
    public String getTypeDeclaration(Property p) {
        //if(!p.getRequired()) {
        //    return " {" + getTypeDeclarationNoOptionalCheck(p) + ", optional}";
        //}
        //else {
        return getTypeDeclarationNoOptionalCheck(p);
        //}

    }

    @Override
    public String getSwaggerType(Property p) {
        String swaggerType = super.getSwaggerType(p);
        String type = null;
        if (typeMapping.containsKey(swaggerType)) {
            type = typeMapping.get(swaggerType);
            if (languageSpecificPrimitives.contains(type)) {
                return type;
            }
        }
        else {
            type = swaggerType;
        }
        if (type == null) {
            return null;
        }
        return type;
    }

    @Override
    public String toDefaultValue(Property p) {
        return "null";
    }

    @Override
    public String toVarName(String name) {
        // replace - with _ e.g. created-at => created_at
        name = name.replaceAll("-", "_"); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

        // if it's all uppper case, convert to lower case
        if (name.matches("^[A-Z_]*$")) {
            name = name.toLowerCase();
        }

        /*
        // petId => pet_id
        name = underscore(name);

        // for reserved word or word starting with number, append _
        if (isReservedWord(name) || name.matches("^\\d.*")) {
            name = escapeReservedWord(name);
        }
        */

        return name;
    }

    @Override
    public String toParamName(String name) {
        // should be the same as variable name
        return toVarName(name);
    }

    @Override
    public String toModelName(String name) {
        // model name cannot use reserved keyword, e.g. return
        if (isReservedWord(name)) {
            throw new RuntimeException(name + " (reserved word) cannot be used as a model name");
        }

        // camelize the model name
        // phone_number => PhoneNumber
        return underscore(name);
    }

    @Override
    public String toModelFilename(String name) {
        // model name cannot use reserved keyword, e.g. return
        if (isReservedWord(name)) {
            throw new RuntimeException(name + " (reserved word) cannot be used as a model name");
        }

        // underscore the model file name
        // PhoneNumber.rb => phone_number.rb
        return underscore(name);
    }

    @Override
    public String toApiFilename(String name) {
        // replace - with _ e.g. created-at => created_at
        name = name.replaceAll("-", "_"); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

        // e.g. PhoneNumberApi.rb => phone_number_api.rb
        return underscore(name) + "_api";
    }

    @Override
    public String toApiName(String name) {
        if (name.length() == 0) {
            return "DefaultApi";
        }
        // e.g. phone_number_api => PhoneNumberApi
        return camelize(name) + "Api";
    }

    @Override
    public String toOperationId(String operationId) {
        // method name cannot use reserved keyword, e.g. return
        if (isReservedWord(operationId)) {
            throw new RuntimeException(operationId + " (reserved word) cannot be used as method name");
        }

        return underscore(operationId);
    }

    @Override
    public Map<String, Object> postProcessSupportingFileData(Map<String, Object> objs) {
        Swagger swagger = (Swagger)objs.get("swagger");
        if(swagger != null) {
            try {
                objs.put("swagger-yaml", Yaml.mapper().writeValueAsString(swagger));
            } catch (JsonProcessingException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        return super.postProcessSupportingFileData(objs);
    }

}
