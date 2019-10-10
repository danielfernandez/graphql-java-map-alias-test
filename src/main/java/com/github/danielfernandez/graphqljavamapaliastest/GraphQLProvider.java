/*
 * =============================================================================
 *
 *   This software is part of the DenodoConnect component collection.
 *
 *   Copyright (c) 2017-2019, Denodo Technologies (http://www.denodo.com)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 * =============================================================================
 */
package com.github.danielfernandez.graphqljavamapaliastest;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import graphql.GraphQL;
import graphql.language.Argument;
import graphql.language.SelectionSet;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.SelectedField;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeRuntimeWiring;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


@Component
public class GraphQLProvider {

    private static final LocalDate SPECIAL_DATE = LocalDate.of(1905,9,26);
    private static final LocalDate GENERAL_DATE = LocalDate.of(1915,11,25);

    private GraphQL graphQL;

    @Bean
    public GraphQL graphQL() {
        return this.graphQL;
    }

    @PostConstruct
    public void init() throws IOException {
        final URL url = Resources.getResource("schema.graphqls");
        final String sdl = Resources.toString(url, Charsets.UTF_8);
        final GraphQLSchema graphQLSchema = buildSchema(sdl);
        this.graphQL = GraphQL.newGraphQL(graphQLSchema).build();
    }


    private GraphQLSchema buildSchema(final String sdl) {
        final TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
        final RuntimeWiring runtimeWiring = buildWiring();
        final SchemaGenerator schemaGenerator = new SchemaGenerator();
        return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
    }

    private RuntimeWiring buildWiring() {
        return RuntimeWiring.newRuntimeWiring()
                .type(TypeRuntimeWiring.newTypeWiring("Query")
                        .dataFetcher("relativity", GraphQLProvider::computeRelativity))
                .type(TypeRuntimeWiring.newTypeWiring("Relativity")
                        .dataFetcher("specialDate", GraphQLProvider::fetchSpecialDate))
                .type(TypeRuntimeWiring.newTypeWiring("Relativity")
                        .dataFetcher("generalDate", GraphQLProvider::fetchGeneralDate))
                .build();
    }



    /*
     * ====================
     * DATA FETCHER METHODS
     * ====================
     */

    private static Map<String,Object> computeRelativity(final DataFetchingEnvironment env) {

//        final List<Argument> generalDateArgs =
//                env.getMergedField().getFields().stream().filter(f -> f.getName().equals("generalDate")).findFirst().get().getArguments();

        final Map<String,String> aliasedSelectedFields = computeAliasedSelectedFields(env);

        final Map<String,Object> user = new HashMap<>();
        user.put("author", "Albert Einstein");
        return user;
    }

    private static String fetchSpecialDate(final DataFetchingEnvironment env) {
        return formatDate(SPECIAL_DATE, env.getArgument("pattern"));
    }

    private static String fetchGeneralDate(final DataFetchingEnvironment env) {
        return formatDate(GENERAL_DATE, env.getArgument("pattern"));
    }

    private static String formatDate(final LocalDate localDate, final String pattern) {
        final DateTimeFormatter formatter =
                (pattern != null)?
                        DateTimeFormatter.ofPattern(pattern, Locale.US) :
                        DateTimeFormatter.ISO_DATE;
        return localDate.format(formatter);
    }


    private static Map<String,String> computeAliasedSelectedFields(final DataFetchingEnvironment env) {

        final GraphQLOutputType outputType = env.getFieldType();
        if (!(outputType instanceof GraphQLObjectType)) {
            return null;
        }
        final GraphQLObjectType objectType = (GraphQLObjectType) outputType;

        final Map<String,String> aliasedSelectedFields = new LinkedHashMap<>();
        computeAliasedSelectedFieldsLevel(env.getGraphQLSchema(), objectType, env.getSelectionSet());
        return aliasedSelectedFields;

    }


    private static void computeAliasedSelectedFieldsLevel(
            final GraphQLSchema schema, final GraphQLObjectType objectType,
            final DataFetchingFieldSelectionSet objectSelectionSet) {

        final Map<String,String> aliasedSelectedFields = new LinkedHashMap<>();
        for (final SelectedField field : objectSelectionSet.getFields()) {
            aliasedSelectedFields.put(field.getQualifiedName(), field.getName());
        }
        return aliasedSelectedFields;
    }


}
