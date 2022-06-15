package pt.up.fe.comp;

import pt.up.fe.comp.analysis.JmmAnalyser;
import pt.up.fe.comp.backend.JmmBackend;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.optimization.JmmOptimizer;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Launcher {

    public static void main(String[] args) {
        SpecsSystem.programStandardInit();

        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        // read the input code
        if (args.length != 1) {
            throw new RuntimeException("Expected a single argument, a path to an existing input file.");
        }
        File inputFile = new File(args[0]);
        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + args[0] + "'.");
        }
        String input = SpecsIo.read(inputFile);

        // Create config
        Map<String, String> config = new HashMap<>();
        config.put("inputFile", args[0]);
        config.put("optimize", "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "false");

        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();
        // Parse stage
        JmmParserResult parserResult = parser.parse(input, config);
        // Check if there are parsing errors
        TestUtils.noErrors(parserResult.getReports());


        // Instantiate JmmAnalysis
        JmmAnalyser analyser = new JmmAnalyser();
        // Analysis stage
        JmmSemanticsResult analysisResult = analyser.semanticAnalysis(parserResult);
        // Check if there are analysis errors
        TestUtils.noErrors(analysisResult.getReports());


        // Instantiate JmmOptimizer
        JmmOptimizer optimizer = new JmmOptimizer();
        // Optimization stage
        OllirResult optimizerResult = optimizer.toOllir(analysisResult);
        // Check if there are optimization errors
        TestUtils.noErrors(optimizerResult.getReports());

        JmmBackend backend = new JmmBackend();
        JasminResult backendResult = backend.toJasmin(optimizerResult);
        TestUtils.noErrors(backendResult.getReports());

        Path mainDir = Paths.get("Results/");
        try {
            if (!Files.exists(mainDir)) {
                Files.createDirectory(mainDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Path path = Paths.get("Results/" + optimizerResult.getSymbolTable().getClassName() + "/");
        try {
            if (!Files.exists(path)) {
                Files.createDirectory(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileWriter myWriter = new FileWriter(path + "/ast.json");
            myWriter.write(parserResult.toJson());
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileWriter myWriter = new FileWriter(path + "/symbolTable.txt");
            myWriter.write(analysisResult.getSymbolTable().print());
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileWriter myWriter = new FileWriter(path + "/ollir.ollir");
            myWriter.write(optimizerResult.getOllirCode());
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileWriter myWriter = new FileWriter(path + "/jasmin.j");
            myWriter.write(backendResult.getJasminCode());
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        backendResult.compile(path.toFile());
    }
}
