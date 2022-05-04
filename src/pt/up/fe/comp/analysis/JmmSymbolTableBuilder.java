package pt.up.fe.comp.analysis;

import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;

public class JmmSymbolTableBuilder extends PreorderJmmVisitor<JmmSymbolTable, Boolean> {
    List<Report> reports = new ArrayList<>();
    JmmSymbolTableBuilder(){
    }

    public List<Report> getReports() {
        return reports;
    }
}
