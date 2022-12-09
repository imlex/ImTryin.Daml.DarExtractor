package imtryin.daml.darextractor.model;

import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DamlTemplateDecl.class, name = "template"),
        @JsonSubTypes.Type(value = DamlRecordDecl.class, name = "record"),
        @JsonSubTypes.Type(value = DamlEnumDecl.class, name = "enum"),
        @JsonSubTypes.Type(value = DamlVariantDecl.class, name = "variant")
})
public abstract class DamlTypeDecl {

    private String name;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> typeVars;
}
