package com.pacesonline.identityservice.contract;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiContractTest {

    private static final Path CONTRACT_PATH = Path.of(
            "..",
            "..",
            "contracts",
            "identity-api",
            "identity-api.yaml"
    ).toAbsolutePath().normalize();

    @Test
    void contractIsValidAndAllReferencesResolve() {
        assertTrue(
                Files.exists(CONTRACT_PATH),
                () -> "OpenAPI contract does not exist: "
                        + CONTRACT_PATH
        );

        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);

        SwaggerParseResult parseResult = new OpenAPIV3Parser()
                .readLocation(
                        CONTRACT_PATH.toUri().toString(),
                        null,
                        parseOptions
                );

        List<String> messages = parseResult.getMessages();

        assertTrue(
                messages == null || messages.isEmpty(),
                () -> "OpenAPI validation failed:\n"
                        + String.join("\n", messages)
        );

        assertNotNull(
                parseResult.getOpenAPI(),
                "The OpenAPI contract could not be parsed"
        );

        assertNotNull(
                parseResult.getOpenAPI()
                        .getPaths()
                        .get("/api/v1/auth/refresh"),
                "The refresh-token path is missing"
        );
    }
}