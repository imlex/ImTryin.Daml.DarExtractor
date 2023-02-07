package imtryin.daml.darextractor;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.daml.daml_lf_dev.DamlLf;
import com.daml.lf.archive.Dar;
import com.daml.lf.archive.package$;
import com.daml.lf.typesig.DefInterface;
import com.daml.lf.typesig.PackageSignature;
import com.daml.lf.typesig.Type;
import com.daml.lf.typesig.reader.SignatureReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import imtryin.daml.darextractor.model.DamlTypeDecl;
import scala.jdk.CollectionConverters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    private enum Format {
        JSON,
        XML
    }

    private enum Types {
        TEMPLATES,
        ALL,
        EVERYTHING
    }

    @Parameter(order = 0, names = {"-f", "--format"}, required = true, description = "Write metadata in selected format.")
    private Format format;

    @Parameter(order = 0, names = {"-s", "--schema"}, description = "Write metadata schema.")
    private boolean schema = false;

    @Parameter(order = 1, names = {"-t", "--types"},
            description = "Process only TEMPLATES or ALL types from DAR file or EVERYTHING including referenced types.")
    private Types types = Types.TEMPLATES;

    @Parameter(order = 2, names = {"-i", "--indentOutput"}, description = "Indent output JSON.")
    private boolean indentOutput = false;

    @Parameter(order = 3, names = {"-?", "-h", "--help"}, description = "Show usage.", help = true)
    private boolean help = false;

    public static void main(String[] args) throws Exception {
        Main main = new Main();

        JCommander jCommander = JCommander.newBuilder().programName("dar-extractor").addObject(main).build();
        jCommander.parse(args);
        if (main.help) {
            System.out.println("dar-extractor");
            System.out.println("\treads DAR file from standard input stream,");
            System.out.println("\textracts templates/types metadata,");
            System.out.println("\twrites metadata to standard output stream.");
            System.out.println();

            jCommander.usage();
        } else if (main.schema) {
            main.writeSchema();
        } else {
            main.convert();
        }
    }

    private void writeSchema() throws IOException {
        var schemaGeneratorConfig = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(new JacksonModule())
                .build();
        var jsonSchema = new SchemaGenerator(schemaGeneratorConfig)
                .generateSchema(List.class, DamlTypeDecl.class);

        System.out.println(indentOutput ? jsonSchema.toPrettyString() : jsonSchema.toString());
    }

    private void convert() throws Exception {
        byte[] inBytes = System.in.readAllBytes();
        var tempFile = File.createTempFile("dar", "dar");
        try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
            fileOutputStream.write(inBytes, 0, inBytes.length);
        }

        Dar<DamlLf.Archive> archiveDar = package$.MODULE$.DarParser().assertReadArchiveFromFile(tempFile);

        var typeMap = new HashMap<String, PackageSignature.TypeDecl>();
        var interfaceMap = new HashMap<String, DefInterface<Type>>();
        for (var archive : CollectionConverters.IterableHasAsJava(archiveDar.all()).asJava()) {
            var packageSignature = SignatureReader.readPackageSignature(archive)._2;

            for (var typeDeclEntry : packageSignature.getTypeDecls().entrySet()) {
                typeMap.put(packageSignature.packageId() + ":" + typeDeclEntry.getKey().qualifiedName(), typeDeclEntry.getValue());
            }
            for (var interfaceEntry : packageSignature.getInterfaces().entrySet()) {
                interfaceMap.put(packageSignature.packageId() + ":" + interfaceEntry.getKey().qualifiedName(), interfaceEntry.getValue());
            }
        }

        Set<String> queuedTypeName;
        if (types == Types.TEMPLATES || types == Types.ALL) {
            var packageSignature = SignatureReader.readPackageSignature(archiveDar.main())._2;
            var typeDeclsStream = packageSignature.getTypeDecls().entrySet().stream();
            if (types == Types.TEMPLATES) {
                typeDeclsStream = typeDeclsStream
                        .filter(e -> e.getValue().getTemplate().isPresent());
            }

            queuedTypeName = typeDeclsStream
                    .map(e -> packageSignature.packageId() + ":" + e.getKey().qualifiedName())
                    .collect(Collectors.toSet());
        } else if (types == Types.EVERYTHING) {
            queuedTypeName = Stream.concat(typeMap.keySet().stream(), interfaceMap.keySet().stream())
                    .collect(Collectors.toSet());
        } else {
            throw new RuntimeException("Not implemented!");
        }


        Set<String> processedTypeNames = new HashSet<>();

        DamlTypeConverter damlTypeConverter = new DamlTypeConverter();

        var result = new ArrayList<DamlTypeDecl>();

        while (!queuedTypeName.isEmpty()) {
            String typeName = queuedTypeName.iterator().next();
            queuedTypeName.remove(typeName);
            processedTypeNames.add(typeName);

            PackageSignature.TypeDecl typeDecl = typeMap.get(typeName);
            if (typeDecl != null) {
                result.add(damlTypeConverter.convert(typeName, typeDecl, refTypeName -> {
                    if (!processedTypeNames.contains(refTypeName)) {
                        queuedTypeName.add(refTypeName);
                    }
                }));
                continue;
            }

            DefInterface<Type> interfaceDecl = interfaceMap.get(typeName);
            if (interfaceDecl != null) {
                result.add(damlTypeConverter.convert(typeName, interfaceDecl, refTypeName -> {
                    if (!processedTypeNames.contains(refTypeName)) {
                        queuedTypeName.add(refTypeName);
                    }
                }));
                continue;
            }

            throw new Exception("Unable to find declaration for '" + typeName + "' type.");
        }

        switch (format) {
            case JSON:
                ObjectMapper objectMapper = new ObjectMapper();
                if (indentOutput) {
                    objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
                }
                objectMapper.writer()
                        .forType(objectMapper.getTypeFactory().constructCollectionType(List.class, DamlTypeDecl.class))
                        .writeValue(System.out, result);
                break;
            case XML:
                XmlMapper xmlMapper = new XmlMapper();
                if (indentOutput) {
                    xmlMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
                }
                xmlMapper.writer()
                        .forType(xmlMapper.getTypeFactory().constructCollectionType(List.class, DamlTypeDecl.class))
                        .writeValue(System.out, result);
                //xmlMapper.writeValue(System.out, result);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + format);
        }
    }
}