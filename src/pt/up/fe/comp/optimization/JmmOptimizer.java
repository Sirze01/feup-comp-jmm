package pt.up.fe.comp.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;

public class JmmOptimizer implements JmmOptimization {
    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        List<Report> reports = new ArrayList<>();
        StringBuilder ollirCode = new StringBuilder();

        return new OllirResult(semanticsResult, ollirCode.toString(), reports);
    }
}
