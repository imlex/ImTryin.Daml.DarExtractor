package imtryin.daml.darextractor.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DamlInterfaceDecl extends DamlTypeDecl {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DamlTypeRef viewType;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<DamlChoice> choices;
}
