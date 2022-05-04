package pt.up.fe.comp.parser;

import org.junit.Test;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class ParserPortionsTest {
    @Test
    public void sum(){
        TestUtils.noErrors(TestUtils.parse("1+2", "AddSubExpression"));
    }

}
