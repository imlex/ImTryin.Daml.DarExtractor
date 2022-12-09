package imtryin.daml.darextractor.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class DamlChoice {

    private String name;

    private boolean consuming;

    private DamlTypeRef paramType;

    private DamlTypeRef returnType;
}
