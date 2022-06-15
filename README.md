

# Compilers Project

## **GROUP: 1C**


| NAME              |      NR       |  GRADE | CONTRIBUTION |
|-------------------|:-------------:|:------:|:------------:|
| Ana Luísa Marques | 201907565     | 18     | 25%          |
| José Costa        | 201907216     | 18     | 25%          |
| Margarida Raposo  | 201906784     | 18     | 25%          |
| Maria Carneiro    | 201907726     | 18     | 25%          |




GLOBAL Grade of the project: 18




## **SUMMARY**:
This compiler can generate an AST from a .jmm file after detecting syntatic and semantic errors within it. It cal also generate an .ollir file from a AST and generate a .jasmin file from OLLIR code




## **SEMANTIC ANALYSIS**:
**Type Verification**
- Variables must be initialized;
- Variables must be defined before its usages;
- Assignments must be between elements of the same type;
- Array access must only be indexed by int values;
- An array must only be initialized by int values;
- Operations must be between elements of the same type;
- Operations are not allowed between arrays;
- Arithmetic operations must be between two int (or functions with int return type);
- Conditional operations must be between two booleans (or functions with boolean return type);

**Method Verification**
- Methods must only be inboked by objects that exist and contain the method
- Methods must belong to the current class or its superclass, or being imported (assume that the method is from the superclass when it does not exist in the current class)
- Methods must be invoked with the correct signature;
- The parameter types must match the method signature;
- Methods can be declared before or after other fuction calls it;
- The invocation of methods that are not from the current class assumes that the method exists and assume the expected types;



## **CODE GENERATION**:
Firstly, the source code is read and is transformed into an AST (all entities in the source code are being represented in the AST).The information in the AST is used to create the Symbol Table and perform the Semantic Analysis.  
The OLLIR code is created by visiting the AST and is inspired by three-address.  
The OLLIR output is used as input to the backend stage of the compiler, responsible for the selection of JVM insrtuctions, the assignment of the local variables of methods to the local variables of the JVM and the generation of JVM code in the jasmin format.

## **PROS:**
We have implemented some extra features, such as jasmin optimizations.



## **CONS:**
We were not able to implement any other optimization besides the one mentionated.



## Project setup

For this project, you need to install [Java](https://jdk.java.net/), [Gradle](https://gradle.org/install/), and [Git](https://git-scm.com/downloads/) (and optionally, a [Git GUI client](https://git-scm.com/downloads/guis), such as TortoiseGit or GitHub Desktop). Please check the [compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html) for Java and Gradle versions.


There are three important subfolders inside the main folder. First, inside the subfolder named ``javacc`` you will find the initial grammar definition. Then, inside the subfolder named ``src`` you will find the entry point of the application. Finally, the subfolder named ``tutorial`` contains code solutions for each step of the tutorial. JavaCC21 will generate code inside the subfolder ``generated``.

## Compile and Running

To compile and install the program, run ``gradle installDist``. This will compile your classes and create a launcher script in the folder ``./build/install/comp2022-00/bin``. For convenience, there are two script files, one for Windows (``comp2022-00.bat``) and another for Linux (``comp2022-00``), in the root folder, that call tihs launcher script.

After compilation, a series of tests will be automatically executed. The build will stop if any test fails. Whenever you want to ignore the tests and build the program anyway, you can call Gradle with the flag ``-x test``.

## Test

To test the program, run ``gradle test``. This will execute the build, and run the JUnit tests in the ``test`` folder. If you want to see output printed during the tests, use the flag ``-i`` (i.e., ``gradle test -i``).
You can also see a test report by opening ``./build/reports/tests/test/index.html``.

## Checkpoint 1
For the first checkpoint the following is required:

1. Convert the provided e-BNF grammar into JavaCC grammar format in a .jj file
2. Resolve grammar conflicts, preferably with lookaheads no greater than 2
3. Include missing information in nodes (i.e. tree annotation). E.g. include the operation type in the operation node.
4. Generate a JSON from the AST

### JavaCC to JSON
To help converting the JavaCC nodes into a JSON format, we included in this project the JmmNode interface, which can be seen in ``src-lib/pt/up/fe/comp/jmm/ast/JmmNode.java``. The idea is for you to use this interface along with the Node class that is automatically generated by JavaCC (which can be seen in ``generated``). Then, one can easily convert the JmmNode into a JSON string by invoking the method JmmNode.toJson().

Please check the JavaCC tutorial to see an example of how the interface can be implemented.

### Reports
We also included in this project the class ``src-lib/pt/up/fe/comp/jmm/report/Report.java``. This class is used to generate important reports, including error and warning messages, but also can be used to include debugging and logging information. E.g. When you want to generate an error, create a new Report with the ``Error`` type and provide the stage in which the error occurred.


### Parser Interface

We have included the interface ``src-lib/pt/up/fe/comp/jmm/parser/JmmParser.java``, which you should implement in a class that has a constructor with no parameters (please check ``src/pt/up/fe/comp/CalculatorParser.java`` for an example). This class will be used to test your parser. The interface has a single method, ``parse``, which receives a String with the code to parse, and returns a JmmParserResult instance. This instance contains the root node of your AST, as well as a List of Report instances that you collected during parsing.

To configure the name of the class that implements the JmmParser interface, use the file ``config.properties``.

### Compilation Stages

The project is divided in four compilation stages, that you will be developing during the semester. The stages are Parser, Analysis, Optimization and Backend, and for each of these stages there is a corresponding Java interface that you will have to implement (e.g. for the Parser stage, you have to implement the interface JmmParser).
