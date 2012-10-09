package org.devunited.groovypp.global

import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.ast.*


@GroovyASTTransformation(phase = CompilePhase.CONVERSION)
class SyntheticPropertiesAndSyntheticMethodsASTTransform implements ASTTransformation {


    AstBuilder astBuilder = new AstBuilder()

    private int PUBLIC_STATIC = PropertyNode.ACC_PUBLIC | PropertyNode.ACC_STATIC

    void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {

        if (astNodes.length != 1 || !(astNodes[0] instanceof ModuleNode)) {
            throw new RuntimeException("Internal error: expecting [ModuleNode] but got: ${astNodes as List}")
        }

        if (!(
        (sourceUnit.name =~ /grails-app/) ||
                (sourceUnit.name =~ /src.groovy/) ||
                (sourceUnit.name =~ /src/) ||
                (sourceUnit.name =~ /griffon-app/)
        )) {
            return
        }

        astNodes[0].getClasses().each { ClassNode targetClass ->

            enrichClass(targetClass)

        }

    }

    private void enrichClass(ClassNode targetClass) {
        if (targetClass.isInterface() || targetClass.isAnnotationDefinition() || targetClass.implementsInterface(new ClassNode(ASTTransformation.class))) {
            return
        }

        if (!targetClass.hasProperty("syntheticProperties")) {
            List<String> propsDeclared = []
            targetClass.properties.each {property ->
                if (property.getLineNumber() > 1) {
                    propsDeclared.add(property.name)
                }
            }
            String propsMap = "  "
            propsDeclared.each {
                propsMap += "${it}: ${it},"
            }
            propsMap = propsMap.substring(0, propsMap.length() - 1)
            targetClass.addProperty(
                    new PropertyNode(
                            "syntheticProperties",
                            PropertyNode.ACC_PUBLIC,
                            new ClassNode(Map),
                            targetClass,
                            null,
                            astBuilder.buildFromString("""
                            [${propsMap}]
                            """).first() as BlockStatement,
                            null
                    )
            )
        }

        if (!targetClass.hasProperty("syntheticPropertyNames")) {
            List<String> propsDeclared = []
            targetClass.properties.each {property ->
                if (property.getLineNumber() > 1) {
                    propsDeclared.add(property.name)
                }
            }
            String propsMap = "  "
            propsDeclared.each {
                propsMap += "'${it}',"
            }
            propsMap = propsMap.substring(0, propsMap.length() - 1)
            targetClass.addProperty(
                    new PropertyNode(
                            "syntheticPropertyNames",
                            PUBLIC_STATIC,
                            new ClassNode(List),
                            targetClass,
                            null,
                            astBuilder.buildFromString("""
                            [${propsMap}]
                            """).first() as BlockStatement,
                            null
                    )
            )
        }

        if (!targetClass.hasProperty("syntheticMethods")) {
            List<String> methodsDeclared = []
            targetClass.methods.each {method ->
                if (method.getLineNumber() > 1) {
                    methodsDeclared.add(method.name)
                }
            }
            String methodsList = methodsDeclared ? ("\"\"\"" + methodsDeclared.join("\"\"\", \"\"\"") + "\"\"\"").replaceAll(/\$/, /\\\$/) : ""
            targetClass.addProperty(
                    new PropertyNode(
                            "syntheticMethods",
                            PropertyNode.ACC_PUBLIC,
                            new ClassNode(List),
                            targetClass,
                            null,
                            astBuilder.buildFromString(CompilePhase.CONVERSION, """
                            [${methodsList}]
                            """).first() as BlockStatement,
                            null
                    )
            )
        }

        if (!targetClass.hasProperty("syntheticMethodNames")) {
            List<String> methodsDeclared = []
            targetClass.methods.each {method ->
                if (method.getLineNumber() > 1) {
                    methodsDeclared.add(method.name)
                }
            }
            String methodsList = methodsDeclared ? ("\"\"\"" + methodsDeclared.join("\"\"\", \"\"\"") + "\"\"\"").replaceAll(/\$/, /\\\$/) : ""
            targetClass.addProperty(
                    new PropertyNode(
                            "syntheticMethodNames",
                            PUBLIC_STATIC,
                            new ClassNode(List),
                            targetClass,
                            null,
                            astBuilder.buildFromString(CompilePhase.CONVERSION, """
                            [${methodsList}]
                            """).first() as BlockStatement,
                            null
                    )
            )
        }

        targetClass.innerClasses.each {InnerClassNode innerClassNode ->
            enrichClass(innerClassNode)
        }

    }
}
