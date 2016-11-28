package io.dwak.freight.processor.binding

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName.VOID
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import io.dwak.freight.processor.model.FieldBinding
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PUBLIC

class FreightTrainBindingClass(classPackage: String,
                               className: String,
                               targetClass: String,
                               processingEnvironment: ProcessingEnvironment)
  : AbstractBindingClass(classPackage, className, targetClass, processingEnvironment) {

  companion object {
    val CLASS_SUFFIX = "$\$FreightTrain"
    val METHOD_NAME = "ship"
  }

  override fun createAndAddBinding(element: Element) {
    val binding = FieldBinding(element)
    bindings.put(binding.name, binding)
  }

  override fun generate(): TypeSpec {
    val classBuilder = TypeSpec.classBuilder(className)
        .addModifiers(PUBLIC)
        .addAnnotation(AnnotationSpec.builder(SuppressWarnings::class.java)
                           .addMember("value", "\$S", "unused")
                           .build())
        .addTypeVariable(TypeVariableName.get("T", ClassName.get(classPackage, targetClass)))
        .addSuperinterface(ParameterizedTypeName.get(ClassName.get("io.dwak.freight.internal",
                                                                   "IFreightTrain"),
                                                     TypeVariableName.get("T")))
    val shipBuilder = MethodSpec.methodBuilder(METHOD_NAME)
        .addModifiers(PUBLIC)
        .returns(VOID)
        .addParameter(ParameterSpec.builder(TypeVariableName.get("T"), "target", FINAL).build())
        .addAnnotation(AnnotationSpec.builder(Override::class.java).build())
        .addAnnotation(AnnotationSpec.builder(SuppressWarnings::class.java)
                           .addMember("value", "\$S", "unused")
                           .build())
        .addStatement("final \$T bundle = target.getArgs()", bundle)

    bindings.values
        .map { it as FieldBinding }
        .forEach {
          val (typeHandled, statement) = getBundleStatement(it)
          if(it.kind == ElementKind.FIELD) {
            if(!typeHandled){
              shipBuilder.addStatement("target.\$L = (\$L) bundle.getSerializable(\"${it.key}\")",
                                             it.name, it.type)
            }
            else {
              shipBuilder.addStatement("target.\$L = \$L", it.name, statement)
            }
          }
          else {
            if(!typeHandled) {
              shipBuilder.addStatement("target.\$L((\$L) bundle.getSerializable(\"${it.key}\"))",
                                             it.name, it.type)
            }
            else {
              shipBuilder.addStatement("target.\$L(\$L)", it.name, statement)
            }
          }
        }
    return classBuilder.addMethod(shipBuilder.build())
        .build()

//    return classType(PUBLIC, className) {
//      annotations = setOf(AnnotationSpec.builder(SuppressWarnings::class.java)
//                              .addMember("value", "\$S", "unused")
//                              .build())
//      parameterizedTypes.add(TypeVariableName.get("T", ClassName.get(classPackage, targetClass)))
//      implements.add(ParameterizedTypeName.get(ClassName.get("io.dwak.freight.internal", "IFreightTrain"),
//                                               TypeVariableName.get("T")))
//
//      method(PUBLIC, VOID, METHOD_NAME,
//             setOf(JavaPoetValue(FINAL, TypeVariableName.get("T"), "target"))) {
//        annotations = setOf(AnnotationSpec.builder(Override::class.java).build(),
//                            AnnotationSpec.builder(SuppressWarnings::class.java)
//                                .addMember("value", "\$S", "unused")
//                                .build())
//        statement("final \$T bundle = target.getArgs()", bundle)
//
//
//        bindings.values
//            .map { it as FieldBinding }
//            .forEach {
//              val (typeHandled, statement) = getBundleStatement(it)
//              if (it.kind == ElementKind.FIELD) {
//                if (!typeHandled) {
//                  statement("target.\$L = (\$L) bundle.getSerializable(\"${it.key}\")", it.name, it.type)
//                }
//                else {
//                  statement("target.\$L = \$L", it.name, statement)
//                }
//              }
//              else {
//                if (!typeHandled) {
//                  statement("target.\$L((\$L) bundle.getSerializable(\"${it.key}\"))", it.name, it.type)
//                }
//                else {
//                  statement("target.\$L(\$L)", it.name, statement)
//                }
//              }
//            }
//      }
//    }
  }

  private fun getBundleStatement(fieldBinding: FieldBinding): Pair<Boolean, String> {
    return handleType(fieldBinding, {
      var statement = ""
      if (!fieldBinding.isRequired) {
        statement += "bundle.containsKey(\"${fieldBinding.key}\") ? "
      }
      statement += "bundle.get$it(\"${fieldBinding.key}\")"

      if (!fieldBinding.isRequired) {
        statement += ": null"
      }
      statement
    })
  }
}

