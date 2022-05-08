package pt.up.fe.comp.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;

public class OllirGenerator extends AJmmVisitor<Boolean, Boolean> {
    private final SymbolTable symbolTable;

    private StringBuilder code = new StringBuilder();
    private List<Report> reports = new ArrayList<>();

    OllirGenerator(SymbolTable symbolTable){
        this.symbolTable = symbolTable;
    }

    public String getCode() {
        return code.toString();
    }

    public List<Report> getReports() {
        return reports;
    }
}
