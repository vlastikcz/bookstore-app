package com.example.bookstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StreamUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.example.bookstore.catalog.interfaces.rest.BookController;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.RequestMethod;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiContractDefinitionTest extends AbstractIntegrationTest {

    private static final String OPENAPI_RESOURCE = "openapi/catalog-auth.yaml";

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void documentedBookEndpointsMatchControllerMappings() throws IOException {
        Set<Endpoint> documented = documentedBookEndpoints();
        Set<Endpoint> implemented = implementedBookEndpoints();

        Set<Endpoint> undocumentedEndpoints = implemented.stream()
                .filter(endpoint -> !documented.contains(endpoint))
                .collect(Collectors.toSet());
        Set<Endpoint> unimplementedSpecEndpoints = documented.stream()
                .filter(endpoint -> !implemented.contains(endpoint))
                .collect(Collectors.toSet());

        assertThat(undocumentedEndpoints)
                .withFailMessage("Controller endpoints missing from OpenAPI specification: %s", undocumentedEndpoints)
                .isEmpty();

        assertThat(unimplementedSpecEndpoints)
                .withFailMessage("OpenAPI specification contains additional book endpoints without controller implementations: %s", unimplementedSpecEndpoints)
                .isEmpty();
    }

    private Set<Endpoint> documentedBookEndpoints() throws IOException {
        String yaml = StreamUtils.copyToString(new ClassPathResource(OPENAPI_RESOURCE).getInputStream(), StandardCharsets.UTF_8);
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, options);
        assertThat(result.getMessages()).as("OpenAPI specification should parse without errors").isEmpty();

        OpenAPI openAPI = result.getOpenAPI();
        assertThat(openAPI).as("OpenAPI document must be present").isNotNull();

        return openAPI.getPaths().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("/api/books"))
                .flatMap(entry -> toEndpoints(entry.getKey(), entry.getValue()))
                .collect(Collectors.toSet());
    }

    private Stream<Endpoint> toEndpoints(String path, PathItem pathItem) {
        Map<PathItem.HttpMethod, io.swagger.v3.oas.models.Operation> operations = pathItem.readOperationsMap();
        return operations.keySet().stream()
                .map(httpMethod -> new Endpoint(httpMethod.name(), normalize(path)));
    }

    private Set<Endpoint> implementedBookEndpoints() {
        return handlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> entry.getValue().getBeanType().equals(BookController.class))
                .flatMap(this::toEndpoints)
                .collect(Collectors.toSet());
    }

    private Stream<Endpoint> toEndpoints(Map.Entry<RequestMappingInfo, HandlerMethod> entry) {
        RequestMappingInfo info = entry.getKey();
        Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
        Set<String> patterns = info.getPathPatternsCondition() == null
                ? Set.of()
                : info.getPathPatternsCondition().getPatternValues();

        if (patterns.isEmpty()) {
            patterns = Set.of("/");
        }

        EnumSet<RequestMethod> httpMethods = methods.isEmpty()
                ? EnumSet.of(RequestMethod.GET)
                : EnumSet.copyOf(methods);

        return patterns.stream()
                .filter(pattern -> pattern.startsWith("/api/books"))
                .flatMap(pattern -> httpMethods.stream()
                        .map(method -> new Endpoint(method.name(), normalize(pattern))));
    }

    private String normalize(String path) {
        return path.replaceAll("\\{[^/]+}", "{}");
    }

    private record Endpoint(String method, String path) { }
}
