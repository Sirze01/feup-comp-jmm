package pt.up.fe.comp.analysis;

import pt.up.fe.comp.ast.AstUtils;
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
        addVisit("MainMethod", this::mainMethodVisit);
        addVisit("InstanceMethod", this::instanceMethodVisit);
    }

    public List<Report> getReports() {
        return reports;
    }

    private Boolean importDeclarationVisit(JmmNode importNode, JmmSymbolTable symbolTable) {
        String importName = importNode.getChildren().stream().map(id -> id.get("name")).collect(Collectors.joining("."));

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

            System.out.println("--Class Declaration--\n");
            System.out.println(" Node:\n  " + node);
            System.out.println("---------");
            System.out.println("  Tree:\n   " + node.toTree());
            System.out.println("  Kind:\n   " + node.getKind());
            System.out.println("---------\n");


            if (Objects.equals(node.getKind(), "VarDeclaration")) {
                String varName = node.getJmmChild(1).get("name");
                if (symbolTable.getFieldsMap().containsKey(varName)) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("column")), "Variable already defined in this scope. Last definition: " + symbolTable.getFieldsMap().get(varName)));
                    continue;
                }

                Type type = AstUtils.getNodeType(node.getJmmChild(0));

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

    private void addLocalVars(JmmNode methodBody, JmmMethod method) {

        for (JmmNode child : methodBody.getChildren()) {
            if (Objects.equals(child.getKind(), "VarDeclaration")) {
                Symbol s = new Symbol(AstUtils.getNodeType(child.getJmmChild(0)), child.getJmmChild(1).get("name"));
                Symbol se = method.addVar(s);

                if (se != null) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(child.get("line")), Integer.parseInt(child.get("column")), "Variable already defined in this scope. Last definition: " + se));
                }/*
                else{
                    Type type = AstUtils.getNodeType(child.getJmmChild(0));

                    Symbol symbol = new Symbol(type, varName);

                    symbolTable.addField(symbol);
                }*/
            }
        }
    }

    private void addAssignments(JmmNode methodBody, JmmSymbolTable symbolTable, JmmMethod method){

        for (JmmNode child : methodBody.getChildren()) {
            if (Objects.equals(child.getKind(), "IDAssignment")) {
                String varName = child.getJmmChild(0).get("name");

                if (child.getJmmChild(1).getAttributes().contains("type")){
                    if (!Objects.equals(child.getJmmChild(1).get("type"), method.getVars().get(0).getType().getName())){
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(child.get("line")), Integer.parseInt(child.get("column")), "Assigned variable '" + varName  + "' with different type value"));
                    }
                    else{
                        String assignedVarName = child.getJmmChild(1).get("name");

                        Type type = AstUtils.getNodeType(child.getJmmChild(0));

                        Symbol symbol = new Symbol(type, assignedVarName);

                        symbolTable.addField(symbol);
                    }
                }
                else {
                    String assignedVarName = child.getJmmChild(1).get("name");
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(child.get("line")), Integer.parseInt(child.get("column")), "Assigned undefined variable '" + assignedVarName + "'"));

                }
            }
        }
    }

    private Boolean mainMethodVisit(JmmNode methodNode, JmmSymbolTable symbolTable) {
        String parameterName = methodNode.getJmmChild(1).get("name");

        JmmMethod method = new JmmMethod("main", new Type("void", false), List.of(new Symbol(new Type("String", true), parameterName)));
        JmmMethod e = symbolTable.addMethod(method);

        if (e != null) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(methodNode.get("line")), Integer.parseInt(methodNode.get("column")), "Main method already defined. Last definition: " + e));
            return false;
        }

        JmmNode methodBody = methodNode.getJmmChild(2);
        addLocalVars(methodBody, method);
        addAssignments(methodBody, symbolTable, method);

        return true;
    }

    private Boolean instanceMethodVisit(JmmNode methodNode, JmmSymbolTable symbolTable) {
        JmmNode methodHeaderNode = methodNode.getJmmChild(0);
        Type methodType = AstUtils.getNodeType(methodHeaderNode.getJmmChild(0));
        String methodName = methodHeaderNode.getJmmChild(1).get("name");


        JmmNode methodArgsNode = methodNode.getJmmChild(1);
        List<Symbol> parameters = new ArrayList<>();
        for (int param = 0; param < methodArgsNode.getNumChildren(); param += 2) {
            parameters.add(new Symbol(AstUtils.getNodeType(methodArgsNode.getJmmChild(param)), methodArgsNode.getJmmChild(param + 1).get("name")));
        }

        JmmMethod method = new JmmMethod(methodName, methodType, parameters);
        JmmMethod e = symbolTable.addMethod(method);

        if (e != null) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(methodNode.get("line")), Integer.parseInt(methodNode.get("column")), "Method already defined. Last definition: " + e));
        }
        JmmNode methodBody = methodNode.getJmmChild(2);
        addLocalVars(methodBody, method);
        addAssignments(methodBody, symbolTable, method);

        return true;
    }
}
