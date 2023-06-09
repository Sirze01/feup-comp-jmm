package pt.up.fe.comp.optimization;
/**
 * Copyright 2021 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;

public class OptimizationPrivateTest {
    private static OllirResult analyseAndPrint(String code) {
        OllirResult results = TestUtils.optimize(code);
        System.out.println("Ollir Code: ");
        System.out.println(results.getOllirCode());

        return results;
    }

    private static OllirResult analyseAndPrint(JmmSemanticsResult semanticsResult) {
        OllirResult results = TestUtils.optimize(semanticsResult);
        System.out.println("Ollir Code: ");
        System.out.println(results.getOllirCode());

        return results;
    }

    @Test
    public void testChanges() {
        String code = SpecsIo.getResource("fixtures/private/jmm/HelloWorld.jmm");
        JmmSemanticsResult semantic = TestUtils.analyse(code);
        System.out.println(semantic.getRootNode().toTree());
        TestUtils.noErrors(analyseAndPrint(semantic));
    }

    @Test
    public void testHelloWorld() {
        TestUtils.noErrors(analyseAndPrint(SpecsIo.getResource("fixtures/public/HelloWorld.jmm")));
    }
}
