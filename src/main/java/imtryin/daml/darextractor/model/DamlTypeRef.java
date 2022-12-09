package imtryin.daml.darextractor.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DamlTypeRef {

    private String name;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer numericScale;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<DamlTypeRef> typeArguments;
}
