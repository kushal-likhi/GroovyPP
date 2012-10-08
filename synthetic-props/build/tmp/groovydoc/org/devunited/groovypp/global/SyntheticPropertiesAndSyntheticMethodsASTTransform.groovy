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


    void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {

        if (astNodes.length != 1 || !(astNodes[0] instanceof ModuleNode)) {
            throw new RuntimeException("Internal error: expecting [ModuleNode] but got: ${astNodes as List}")
        }

        if (!((sourceUnit.name =~ /grails-app/) || (sourceUnit.name =~ /src.groovy/))) {
            return
        }

        astNodes[0].getClasses().each { ClassNode targetClass ->

            List<String> propsDeclared = []
            targetClass.properties.each {property ->
                if (property.getLineNumber() > 1) {
                    propsDeclared.add(property.name)
                }
            }

            List<String> methodsDeclared = []
            targetClass.methods.each {method ->
                if (method.getLineNumber() > 1) {
                    methodsDeclared.add(method.name)
                }
            }

            String propsMap = "  "
            propsDeclared.each {
                propsMap += "${it}: ${it},"
            }
            propsMap = propsMap.substring(0, propsMap.length() - 1)
            targetClass.addProperty(
                    new PropertyNode(
                            "declaredProperties",
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

            String methodsList = "  "
            methodsDeclared.each {
                methodsList += "'${it}',"
            }
            methodsList = methodsList.substring(0, methodsList.length() - 1)
            targetClass.addProperty(
                    new PropertyNode(
                            "declaredMethods",
                            PropertyNode.ACC_PUBLIC,
                            new ClassNode(List),
                            targetClass,
                            null,
                            astBuilder.buildFromString("""
                            [${methodsList}]
                            """).first() as BlockStatement,
                            null
                    )
            )

        }

    }
}
