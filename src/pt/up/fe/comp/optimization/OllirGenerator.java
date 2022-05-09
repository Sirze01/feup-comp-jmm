package pt.up.fe.comp.optimization;

import pt.up.fe.comp.analysis.JmmMethod;
import pt.up.fe.comp.analysis.JmmSymbolTable;
import pt.up.fe.comp.analysis.JmmSymbolTableBuilder;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OllirGenerator extends AJmmVisitor<Boolean, Boolean> {
    private final SymbolTable symbolTable;
    private final StringBuilder code = new StringBuilder();
    private final List<Report> reports = new ArrayList<>();
    private final Map<String, String> config;

    private int indent = 0;

    private int getNumSpaces() {
        return OllirGeneratorUtils.getNumSpaces(config);
    }

    OllirGenerator(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.config = null;
        addVisits();
    }

    OllirGenerator(SymbolTable symbolTable, Map<String, String> config) {
        this.symbolTable = symbolTable;
        this.config = config;
        addVisits();
    }

    private void addVisits() {
        addVisit("Program", this::programVisit);
        addVisit("ClassDeclaration", this::classDeclarationVisit);
        addVisit("MainMethod", this::mainMethodVisit);
        addVisit("InstanceMethod", this::instanceMethodVisit);
    }

    public String getCode() {
        return code.toString();
    }

    public List<Report> getReports() {
        return reports;
    }

    private Boolean programVisit(JmmNode programNode, Boolean dummy) {
        for (String importedPkg : symbolTable.getImports()) {
            code.append("import ").append(importedPkg).append(";\n");
        }
        code.append('\n');
        for (JmmNode child : programNode.getChildren()) {
            visit(child);
        }
        return true;
    }

    private Boolean classDeclarationVisit(JmmNode classNode, Boolean dummy) {
        code.append(symbolTable.getClassName());

        if (symbolTable.getSuper() != null) {
            code.append(" extends ").append(symbolTable.getSuper());
        }

        code.append(" {\n");
        indent++;

        for (Symbol field : symbolTable.getFields()) {
            code.append(" ".repeat(indent * getNumSpaces()));
            code.append(".field ");
            code.append(OllirGeneratorUtils.getCode(field));
            code.append(";\n");
        }

        code.append("\n\n");

        for (JmmNode child : classNode.getChildren()) {
            visit(child);
            code.append("\n");
        }

        indent--;
        code.append(" ".repeat(indent * getNumSpaces()));
        code.append("}");
        return true;
    }

    private Boolean mainMethodVisit(JmmNode mainNode, Boolean dummy) {
        String mainSignature = new JmmMethod("main", new Type("void", false), List.of(new Symbol(new Type("String", true), null))).toString();

        code.append(" ".repeat(indent * getNumSpaces()));
        code.append(".method public static ");
        methodScopeVisit(mainSignature);

        return true;
    }

    private Boolean instanceMethodVisit(JmmNode methodNode, Boolean dummy) {
        String methodSignature = JmmSymbolTableBuilder.generateMethod(methodNode).toString();

        code.append(" ".repeat(indent * getNumSpaces()));
        code.append(".method public ");
        methodScopeVisit(methodSignature);

        return true;
    }

    private void methodScopeVisit(String methodSignature){
        code.append(OllirGeneratorUtils.getCode(((JmmSymbolTable) symbolTable), methodSignature));
        code.append(" {\n");
        indent++;

        indent--;
        code.append(" ".repeat(indent * getNumSpaces()));
        code.append("}\n");

    }
}
