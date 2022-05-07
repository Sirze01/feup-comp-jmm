package pt.up.fe.comp.analysis;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.specs.util.SpecsIo;

public class AnalysisPrivateTest {
    private static JmmSemanticsResult noErrors(String code) {
        JmmSemanticsResult results = TestUtils.analyse(code);
        System.out.println("SymbolTable: ");
        System.out.println(results.getSymbolTable().print());
        TestUtils.noErrors(results);
        return results;
    }

    private static JmmSemanticsResult mustFail(String code){
        JmmSemanticsResult results = TestUtils.analyse(code);
        System.out.println("SymbolTable: ");
        System.out.println(results.getSymbolTable().print());
        TestUtils.mustFail(results);
        return results;
    }


    @Test
    public void testChanges() {
        JmmSemanticsResult results = noErrors(SpecsIo.getResource("fixtures/private/jmm/HelloWorld.jmm"));
        System.out.println(results.getRootNode().toTree());
    }

    @Test
    public void testFindMaximum() {
        noErrors(SpecsIo.getResource("fixtures/public/FindMaximum.jmm"));

    }

    @Test
    public void testHelloWorld() {
        noErrors(SpecsIo.getResource("fixtures/public/HelloWorld.jmm"));
    }

    @Test
    public void testLazysort() {
        noErrors(SpecsIo.getResource("fixtures/public/Lazysort.jmm"));
    }

    @Test
    public void testLife() {
        noErrors(SpecsIo.getResource("fixtures/public/Life.jmm"));

    }

    @Test
    public void testMonteCarloPi() {
        noErrors(SpecsIo.getResource("fixtures/public/MonteCarloPi.jmm"));
    }

    @Test
    public void testQuickSort() {
        noErrors(SpecsIo.getResource("fixtures/public/QuickSort.jmm"));
    }

    @Test
    public void testSimple() {
        noErrors(SpecsIo.getResource("fixtures/public/Simple.jmm"));
    }

    @Test
    public void testTicTacToe() {
        noErrors(SpecsIo.getResource("fixtures/public/TicTacToe.jmm"));

    }

    @Test
    public void testWhileAndIf() {
        noErrors(SpecsIo.getResource("fixtures/public/WhileAndIf.jmm"));
    }
}
