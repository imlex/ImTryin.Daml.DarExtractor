package imtryin.daml.darextractor;

import com.daml.lf.data.Ref;
import imtryin.daml.darextractor.model.*;

import com.daml.lf.typesig.Enum;
import com.daml.lf.typesig.Record;
import com.daml.lf.typesig.*;
import scala.Option;
import scala.jdk.CollectionConverters;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DamlTypeConverter {

    public DamlTypeDecl convert(String typeName, PackageSignature.TypeDecl typeDecl, Consumer<String> onTypeRef) {
        DamlTypeDecl damlTypeDecl;

        DataType dataType = typeDecl.getType().dataType();
        var template = typeDecl.getTemplate();
        if (template.isPresent()) {
            DamlTemplateDecl damlTemplateType = new DamlTemplateDecl();
            damlTemplateType.setFields(getFields((Record<Type>) dataType, onTypeRef));

            DefTemplate<Type> defTemplate = template.get();

            defTemplate.getKey().ifPresent(type -> damlTemplateType.setKeyType(getTypeRef(type, onTypeRef)));

            damlTemplateType.setChoices(
                    CollectionConverters.MapHasAsJava(defTemplate.choices()).asJava().entrySet().stream()
                            .map(c -> {
                                TemplateChoice<Type> templateChoice = c.getValue();
                                return new DamlChoice(
                                        c.getKey(),
                                        templateChoice.consuming(),
                                        getTypeRef(templateChoice.param(), onTypeRef),
                                        getTypeRef(templateChoice.returnType(), onTypeRef));
                            })
                            .collect(Collectors.toList()));

            damlTypeDecl = damlTemplateType;
        } else if (dataType instanceof Record) {
            var damlRecordDecl = new DamlRecordDecl();

            damlRecordDecl.setFields(getFields((Record<Type>) dataType, onTypeRef));

            damlTypeDecl = damlRecordDecl;
        } else if (dataType instanceof Enum anEnum) {
            var damlEnumDecl = new DamlEnumDecl();

            damlEnumDecl.setEnumValues(CollectionConverters.SeqHasAsJava(anEnum.constructors()).asJava());

            damlTypeDecl = damlEnumDecl;
        } else if (dataType instanceof Variant) {
            var damlVariantDecl = new DamlVariantDecl();

            damlVariantDecl.setVariants(getFields((Variant<Type>) dataType, onTypeRef));

            damlTypeDecl = damlVariantDecl;
        } else {
            throw new IllegalArgumentException();
        }

        damlTypeDecl.setName(typeName);
        var typeVars = (List<String>) typeDecl.type().getTypeVars();
        if (typeVars.size() > 0) {
            damlTypeDecl.setTypeVars(typeVars);
        }

        return damlTypeDecl;
    }

    public DamlTypeDecl convert(String typeName, DefInterface<Type> interfaceDecl, Consumer<String> onTypeRef) {
        if (typeName.equals("95644d5c6ff8c9a433820d694916d86d5e94e1418880b66bf0b3e5103dbc0e09:Daml.Finance.Interface.Holding.Transferable:Transferable")) {
            int i = 0;
        }

        DamlInterfaceDecl damlInterfaceDecl = new DamlInterfaceDecl();

        Option<Ref.Identifier> viewType = interfaceDecl.viewType();
        if (viewType.isDefined()) {
            damlInterfaceDecl.setViewType(getTypeRef(viewType.get(), onTypeRef));
        }

        // ToDo: retroImplements
        if (!interfaceDecl.retroImplements().isEmpty()) {
            throw new RuntimeException();
        }

        damlInterfaceDecl.setChoices(
                CollectionConverters.MapHasAsJava(interfaceDecl.choices()).asJava().entrySet().stream()
                        .map(c -> {
                            TemplateChoice<Type> templateChoice = c.getValue();
                            return new DamlChoice(
                                    c.getKey(),
                                    templateChoice.consuming(),
                                    getTypeRef(templateChoice.param(), onTypeRef),
                                    getTypeRef(templateChoice.returnType(), onTypeRef));
                        })
                        .collect(Collectors.toList()));

        damlInterfaceDecl.setName(typeName);

        return damlInterfaceDecl;
    }

    private List<DamlField> getFields(DataType.GetFields<Type> dataType, Consumer<String> onTypeRef) {
        return dataType.getFields().stream()
                .map(f -> new DamlField(f._1, getTypeRef(f._2, onTypeRef)))
                .collect(Collectors.toList());
    }

    private DamlTypeRef getTypeRef(Type type, Consumer<String> onTypeRef) {
        DamlTypeRef damlTypeRef = new DamlTypeRef();

        if (type instanceof TypeCon typeCon) {
            String damlTypeName = typeCon.name().identifier().toString();
            //log.debug("Processing " + damlTypeName);

            damlTypeRef.setName(damlTypeName);
            if (typeCon.getTypArgs().size() > 0) {
                damlTypeRef.setTypeArguments(typeCon.getTypArgs().stream().map(t -> getTypeRef(t, onTypeRef)).collect(Collectors.toList()));
            }

            onTypeRef.accept(damlTypeName);
        } else if (type instanceof TypeNumeric typeNumeric) {
            String damlTypeName = "Numeric";
            //log.debug("Processing " + damlTypeName);

            damlTypeRef.setName(damlTypeName);
            damlTypeRef.setNumericScale(typeNumeric.scale());
        } else if (type instanceof TypePrim typePrim) {
            PrimType primType = typePrim.typ();

            if (primType.equals(PrimType.Bool())) {
                String damlTypeName = "Bool";
                //log.debug("Processing " + damlTypeName);

                damlTypeRef.setName(damlTypeName);
            } else if (primType.equals(PrimType.ContractId())) {
                String damlTypeName = "ContractId";
                //log.debug("Processing " + damlTypeName);

                damlTypeRef.setName(damlTypeName);
                damlTypeRef.setTypeArguments(typePrim.getTypArgs().stream().map(t -> getTypeRef(t, onTypeRef)).collect(Collectors.toList()));
            } else if (primType.equals(PrimType.Date())) {
                String damlTypeName = "Date";
                //log.debug("Processing " + damlTypeName);

                damlTypeRef.setName(damlTypeName);
            } else if (primType.equals(PrimType.GenMap())) {
                String damlTypeName = "GenMap";
                //log.debug("Processing " + damlTypeName);

                damlTypeRef.setName(damlTypeName);
                damlTypeRef.setTypeArguments(typePrim.getTypArgs().stream().map(t -> getTypeRef(t, onTypeRef)).collect(Collectors.toList()));
            } else if (primType.equals(PrimType.Int64())) {
                String damlTypeName = "Int64";
                //log.debug("Processing " + damlTypeName);

                damlTypeRef.setName(damlTypeName);
            } else if (primType.equals(PrimType.List())) {
                String damlTypeName = "List";
                //log.debug("Processing " + damlTypeName);

                damlTypeRef.setName(damlTypeName);
                damlTypeRef.setTypeArguments(typePrim.getTypArgs().stream().map(t -> getTypeRef(t, onTypeRef)).collect(Collectors.toList()));
            } else if (primType.equals(PrimType.Optional())) {
                String damlTypeName = "Optional";
                //log.debug("Processing " + damlTypeName);

                damlTypeRef.setName(damlTypeName);
                damlTypeRef.setTypeArguments(typePrim.getTypArgs().stream().map(t -> getTypeRef(t, onTypeRef)).collect(Collectors.toList()));
            } else if (primType.equals(PrimType.Party())) {
                String damlTypeName = "Party";
                //log.debug("Processing " + damlTypeName);

                damlTypeRef.setName(damlTypeName);
            } else if (primType.equals(PrimType.Text())) {
                String damlTypeName = "Text";
                //log.debug("Processing " + damlTypeName);

                damlTypeRef.setName(damlTypeName);
            } else if (primType.equals(PrimType.TextMap())) {
                String damlTypeName = "TextMap";
                //log.debug("Processing " + damlTypeName);

                damlTypeRef.setName(damlTypeName);
                damlTypeRef.setTypeArguments(typePrim.getTypArgs().stream().map(t -> getTypeRef(t, onTypeRef)).collect(Collectors.toList()));
            } else if (primType.equals(PrimType.Timestamp())) {
                String damlTypeName = "Timestamp";
                //log.debug("Processing " + damlTypeName);

                damlTypeRef.setName(damlTypeName);
            } else if (primType.equals(PrimType.Unit())) {
                String damlTypeName = "Unit";
                //log.debug("Processing " + damlTypeName);

                damlTypeRef.setName(damlTypeName);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (type instanceof TypeVar typeVar) {
            String damlTypeName = typeVar.name();
            //log.debug("Processing " + damlTypeName);

            damlTypeRef.setName(damlTypeName);
        }

        return damlTypeRef;
    }

    private DamlTypeRef getTypeRef(Ref.Identifier identifier, Consumer<String> onTypeRef) {
        DamlTypeRef damlTypeRef = new DamlTypeRef();
        String damlTypeName = identifier.toString();
        //log.debug("Processing " + damlTypeName);

        damlTypeRef.setName(damlTypeName);

        onTypeRef.accept(damlTypeName);

        return damlTypeRef;
    }
}
