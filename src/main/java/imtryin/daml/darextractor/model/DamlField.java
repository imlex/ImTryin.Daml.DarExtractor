package imtryin.daml.darextractor.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class DamlField {

    private String name;

    private DamlTypeRef type;
}
