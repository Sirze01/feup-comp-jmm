package pt.up.fe.comp.analysis;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class AnalysisPublicTest {
    @Test
    public void testFindMaximum() {
        var results = TestUtils.analyse(SpecsIo.getResource("fixtures/public/FindMaximum.jmm"));
        TestUtils.noErrors(results);
    }
    @Test
    public void testHelloWorld() {
        var results = TestUtils.analyse(SpecsIo.getResource("fixtures/public/HelloWorld.jmm"));
        TestUtils.noErrors(results);
    }

    @Test
    public void testLazysort() {
        var results = TestUtils.analyse(SpecsIo.getResource("fixtures/public/Lazysort.jmm"));
        TestUtils.noErrors(results);
    }

    @Test
    public void testLife() {
        var results = TestUtils.analyse(SpecsIo.getResource("fixtures/public/Life.jmm"));
        TestUtils.noErrors(results);
    }

    @Test
    public void testMonteCarloPi() {
        var results = TestUtils.analyse(SpecsIo.getResource("fixtures/public/MonteCarloPi.jmm"));
        TestUtils.noErrors(results);
    }

    @Test
    public void testQuickSort() {
        var results = TestUtils.analyse(SpecsIo.getResource("fixtures/public/QuickSort.jmm"));
        TestUtils.noErrors(results);
    }

    @Test
    public void testSimple() {
        var results = TestUtils.analyse(SpecsIo.getResource("fixtures/public/Simple.jmm"));
        TestUtils.noErrors(results);
    }

    @Test
    public void testTicTacToe() {
        var results = TestUtils.analyse(SpecsIo.getResource("fixtures/public/TicTacToe.jmm"));
        TestUtils.noErrors(results);
    }

    @Test
    public void testWhileAndIf() {
        var results = TestUtils.analyse(SpecsIo.getResource("fixtures/public/WhileAndIf.jmm"));
        TestUtils.noErrors(results);
    }
}
