package pt.up.fe.comp.analysis;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class AnalysisTest {
    @Test
    public void testChanges(){
        var results = TestUtils.analyse(SpecsIo.getResource("fixtures/private/HelloWorld.jmm"));
        System.out.println(results.getRootNode().toTree());
        System.out.println("SymbolTable: " + results.getSymbolTable().print());
        TestUtils.noErrors(results);
    }
    @Test
    public void testHelloWorld(){
        var results = TestUtils.analyse(SpecsIo.getResource("fixtures/public/HelloWorld.jmm"));
        System.out.println(results.getRootNode().toTree());
        System.out.println("SymbolTable: " + results.getSymbolTable().print());
        TestUtils.noErrors(results);
    }
}
