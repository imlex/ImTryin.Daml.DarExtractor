package imtryin.daml.darextractor.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DamlRecordDecl extends DamlTypeDecl {
    private List<DamlField> fields;
}
