package imtryin.daml.darextractor;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.daml.daml_lf_dev.DamlLf;
import com.daml.lf.archive.Dar;
import com.daml.lf.archive.package$;
import com.daml.lf.typesig.PackageSignature;
import com.daml.lf.typesig.reader.SignatureReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import imtryin.daml.darextractor.model.DamlTypeDecl;
import scala.jdk.CollectionConverters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    @Parameter(order = 0, names = {"-j", "--jsonSchema"}, description = "Write JSON schema.")
    private Boolean jsonSchema;

    @Parameter(order = 1, names = {"-a", "--allTypes"},
            description = "Process all types from DAR file - not only templates and all referenced types, but also not referenced.")
    private Boolean allTypes;

    @Parameter(order = 2, names = {"-i", "--indentOutput"}, description = "Indent output JSON.")
    private Boolean indentOutput;

    @Parameter(order = 3, names = {"-?", "-h", "--help"}, description = "Show usage.", help = true)
    private boolean help;

    public static void main(String[] args) throws IOException {
        Main main = new Main();

        JCommander jCommander = JCommander.newBuilder().programName("dar-extractor").addObject(main).build();
        jCommander.parse(args);
        if (main.help) {
            System.out.println("dar-extractor");
            System.out.println("\treads DAR file from standard input stream,");
            System.out.println("\textracts templates/types metadata,");
            System.out.println("\twrites metadata to standard output stream as JSON.");
            System.out.println();

            jCommander.usage();
        } else if (main.jsonSchema != null && main.jsonSchema) {
            main.writeJsonSchema();
        } else {
            main.convert();
        }
    }

    private void writeJsonSchema() throws IOException {
        var schemaGeneratorConfig = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(new JacksonModule())
                .build();
        var jsonSchema = new SchemaGenerator(schemaGeneratorConfig)
                .generateSchema(List.class, DamlTypeDecl.class);

        System.out.println(indentOutput != null && indentOutput ? jsonSchema.toPrettyString() : jsonSchema.toString());
    }

    private void convert() throws IOException {
        byte[] inBytes = System.in.readAllBytes();
        var tempFile = File.createTempFile("dar", "dar");
        try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
            fileOutputStream.write(inBytes, 0, inBytes.length);
        }

        Dar<DamlLf.Archive> archiveDar = package$.MODULE$.DarParser().assertReadArchiveFromFile(tempFile);

        var typeMap = new HashMap<String, PackageSignature.TypeDecl>();
        for (var archive : CollectionConverters.IterableHasAsJava(archiveDar.all()).asJava()) {
            var packageSignature = SignatureReader.readPackageSignature(archive)._2;

            for (var typeDeclEntry : packageSignature.getTypeDecls().entrySet()) {
                typeMap.put(packageSignature.packageId() + ":" + typeDeclEntry.getKey().qualifiedName(), typeDeclEntry.getValue());
            }
        }

        var packageSignature = SignatureReader.readPackageSignature(archiveDar.main())._2;
        var typeDeclsStream = packageSignature.getTypeDecls().entrySet().stream();
        if (allTypes != null && allTypes) {
            typeDeclsStream = typeDeclsStream
                    .filter(e -> e.getValue().getTemplate().isPresent());
        }

        Set<String> queuedTypeName = typeDeclsStream
                .map(e -> packageSignature.packageId() + ":" + e.getKey().qualifiedName())
                .collect(Collectors.toSet());

        Set<String> processedTypeNames = new HashSet<>();

        DamlTypeConverter damlTypeConverter = new DamlTypeConverter();

        var result = new ArrayList<DamlTypeDecl>();

        while (!queuedTypeName.isEmpty()) {
            String typeName = queuedTypeName.iterator().next();
            queuedTypeName.remove(typeName);
            processedTypeNames.add(typeName);

            PackageSignature.TypeDecl typeDecl = typeMap.get(typeName);

            result.add(damlTypeConverter.convert(typeName, typeDecl, refTypeName -> {
                if (!processedTypeNames.contains(refTypeName)) {
                    queuedTypeName.add(refTypeName);
                }
            }));
        }

        ObjectMapper objectMapper = new ObjectMapper();
        if (indentOutput != null && indentOutput) {
            objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        }
        objectMapper.writer()
                .forType(objectMapper.getTypeFactory().constructCollectionType(List.class, DamlTypeDecl.class))
                .writeValue(System.out, result);
    }
}