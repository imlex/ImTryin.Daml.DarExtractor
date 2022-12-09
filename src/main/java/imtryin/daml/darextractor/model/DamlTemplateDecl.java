package imtryin.daml.darextractor.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DamlTemplateDecl extends DamlRecordDecl {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DamlTypeRef keyType;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<DamlChoice> choices;
}
