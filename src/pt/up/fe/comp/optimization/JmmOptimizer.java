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
        System.out.println(generator.getCode());

        return new OllirResult(semanticsResult, generator.getCode(), generator.getReports());
    }
}
