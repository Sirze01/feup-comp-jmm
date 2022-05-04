package pt.up.fe.comp.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class JmmSymbolTableBuilder extends PreorderJmmVisitor<JmmSymbolTable, Boolean> {
    List<Report> reports = new ArrayList<>();

    JmmSymbolTableBuilder() {
        addVisit("ImportDeclaration", this::importDeclarationVisit);
        addVisit("ClassDeclaration", this::classDeclarationVisit);
        addVisit("InheritanceDeclaration", this::inheritanceDeclarationVisit);
    }

    public List<Report> getReports() {
        return reports;
    }

    private Boolean importDeclarationVisit(JmmNode importNode, JmmSymbolTable symbolTable) {
        String importName = importNode.getChildren()
                .stream()
                .map(id -> id.get("name"))
                .collect(Collectors.joining("."));

        if (symbolTable.getImports().contains("importName")) {
            reports.add(new Report(ReportType.WARNING, Stage.SEMANTIC, Integer.parseInt(importNode.get("line")), Integer.parseInt(importNode.get("column")), "Repeated import statement: " + importName));
            return false;
        }
        symbolTable.addImport(importName);
        return true;
    }

    private Boolean classDeclarationVisit(JmmNode classNode, JmmSymbolTable symbolTable) {
        String className = classNode.getJmmChild(0).get("name");
        symbolTable.setClassName(className);

        for (JmmNode node : classNode.getChildren()) {
            if (Objects.equals(node.getKind(), "VarDeclaration")) {
                String varName = node.getJmmChild(1).get("name");

                if (symbolTable.getFieldsMap().containsKey(varName)) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("column")), "Variable already defined in this scope. Last definition: " + symbolTable.getFieldsMap().get(varName)));
                    continue;
                }

                Type type;
                var typeNode = node.getJmmChild(0);
                if (Objects.equals(typeNode.get("type"), "IntArray")) {
                    type = new Type("Int", true);
                } else {
                    type = new Type(typeNode.get("type"), false);
                }

                Symbol symbol = new Symbol(type, varName);

                symbolTable.addField(symbol);
            }

        }
        return true;
    }

    private Boolean inheritanceDeclarationVisit(JmmNode inheritanceNode, JmmSymbolTable symbolTable) {
        symbolTable.setSuperName(inheritanceNode.getJmmChild(0).get("name"));
        return true;
    }


}
