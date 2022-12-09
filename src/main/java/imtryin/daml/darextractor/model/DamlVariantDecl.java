package imtryin.daml.darextractor.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DamlVariantDecl extends DamlTypeDecl {
    private List<DamlField> variants;
}
