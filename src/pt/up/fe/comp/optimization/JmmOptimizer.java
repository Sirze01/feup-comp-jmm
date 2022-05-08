package pt.up.fe.comp.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;

import java.util.List;

public class JmmOptimizer implements JmmOptimization {
    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        OllirGenerator generator = new OllirGenerator(semanticsResult.getSymbolTable());
        generator.visit(semanticsResult.getRootNode(), null);
        String ollirCode = generator.getCode();
        List <Report> reports = generator.getReports();

        return new OllirResult(semanticsResult, ollirCode, reports);
    }
}
