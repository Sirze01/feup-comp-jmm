package pt.up.fe.comp.analysis.visitors;

import jas.Var;
import org.eclipse.jgit.util.SystemReader;
import pt.up.fe.comp.analysis.JmmSymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class VariablesVisitor extends AJmmVisitor<List<Report>, String> {
    JmmSymbolTable symbolTable;
    List<String> variables;

    public VariablesVisitor(JmmSymbolTable symbolTable){
        this.symbolTable = symbolTable;
        this.variables = new ArrayList<>();

        addVisit("VarDeclaration", this::visitVarDeclaration);
        addVisit("ID", this::visitID);
        addVisit("_New", this::visitNew);
        addVisit("MethodBody", this::visitMethodBody);
        addVisit("AccessExpression", this::visitObjectMethod);
        addVisit("MethodHeader", this::visitMethodHeader);
        setDefaultVisit(this::defaultVisit);
    }

    private String defaultVisit(JmmNode jmmNode, List<Report> reports){
        for (JmmNode child : jmmNode.getChildren())
            visit(child, reports);
        return "";
    }

    private String visitVarDeclaration(JmmNode node, List<Report> reports){
        List<String> types = new ArrayList<>(Arrays.asList("Int", "IntArray", "Boolean", symbolTable.getClassName()));

        for (String imp: symbolTable.getImports()){
            String[] impList = imp.split("\\.");
            String type = impList[impList.length-1];
            types.add(type);
        }

        if (!types.contains(node.getJmmChild(0).get("type"))){
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC, Integer.parseInt(node.getJmmChild(0).get("line")),
                    Integer.parseInt(node.getJmmChild(0).get("column")),
                    "Type \"" + node.getJmmChild(0).get("type") + "\" is not defined."));
            return "<Invalid>";
        }

        return node.getJmmChild(0).get("type");
    }

    private String visitIDAssignment(JmmNode jmmNode, List<Report> reports){
        String id = visitID(jmmNode.getJmmChild(0), reports);
        if (!id.isEmpty())
            return visitNew(jmmNode.getJmmChild(1), reports);
        return id;
    }

    private String visitID(JmmNode jmmNode, List<Report> reports){
        /*
        Optional<JmmNode> ancestor = jmmNode.getAncestor("MAIN").isPresent() ? jmmNode.getAncestor("MAIN") : jmmNode.getAncestor("METHOD_DECLARATION");
        Symbol var = symbolTable.getVariable(jmmNode.get("name"), ancestor.get().get("name"));

        if (var == null && !symbolTable.checkVariableInImports(jmmNode.get("name"))) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("line")),
                    Integer.parseInt(jmmNode.get("col")),
                    "Variable \"" + jmmNode.get("name") + "\" is undefined."
            ));
            return "<Unknown>";
        }
        if (jmmNode.getParent().getKind().equals("OPERATION")
                && !initializedVariables.contains(jmmNode.get("name"))
                && !symbolTable.getMethod(ancestor.get().get("name")).containsParameter(jmmNode.get("name"))
                && symbolTable.getField(jmmNode.get("name"))==null) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("line")),
                    Integer.parseInt(jmmNode.get("col")),
                    "Variable \"" + jmmNode.get("name") + "\" has not been initialized."
            ));
        }
        String ret = "<Unknown>";
        if (var != null)
            ret = var.getType().isArray() ? var.getType().getName() + " array" : var.getType().getName();

        return ret;*/
        return "";
    }

    private String visitNew(JmmNode jmmNode, List<Report> reports){
        return visit(jmmNode.getJmmChild(0), reports);
    }

    private String visitObjectMethod(JmmNode node, List<Report> reports){
        List<JmmNode> children = node.getChildren();
        System.out.println("lili" + symbolTable.getMethodByName("foo"));
       if(children.get(0).getKind().equals("Literal") && children.get(0).get("value").equals("this") && symbolTable.getSuper() == null){
           System.out.println("lala"+symbolTable.getSuper());
           if (symbolTable.getMethodByName(children.get(1).getJmmChild(0).get("name")) == null){
               System.out.println("mimi");
               reports.add(new Report(ReportType.ERROR,
                       Stage.SEMANTIC,
                       children.get(1).getJmmChild(0).get("line") != null ? Integer.parseInt(children.get(1).getJmmChild(0).get("line")) : 0,
                       Integer.parseInt(children.get(1).getJmmChild(0).get("column")),
                       "Method " + children.get(1).getJmmChild(0).get("name") + "() isn't declared"));
               return "<Invalid>";
           } else if (children.get(1).getJmmChild(0).getChildren().size()
               != symbolTable.getMethodByName(children.get(1).getJmmChild(0).get("name")).getParameters().size()){
                 reports.add(new Report(ReportType.ERROR,
                         Stage.SEMANTIC,
                         children.get(1).getJmmChild(0).get("line") != null ? Integer.parseInt(children.get(1).getJmmChild(0).get("line")) : 0,
                         Integer.parseInt(children.get(1).getJmmChild(0).get("column")),
                         "Method " + children.get(1).getJmmChild(0).get("name") + "() has the wrong number of arguments"));
           }
       }
        visit(node.getChildren().get(1).getJmmChild(0), reports);
        return "Method";
    }

    private String visitMethodHeader(JmmNode node, List<Report> reports) {
        JmmNode identifier = node.getChildren().get(0);
        JmmNode method = node.getChildren().get(1);
        visit(identifier, reports);
        return visit(method, reports);
    }
    private String visitMethodBody(JmmNode node, List<Report> reports){
        variables.clear();
        return defaultVisit(node, reports);
    }
}
