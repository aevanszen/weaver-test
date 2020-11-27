package weaver

import scala.util.control.NoStackTrace

import org.portablescala.reflect.Reflect
import scala.reflect.ClassTag

package object framework {

  protected[framework] def loadConstructor[A, C](
      qualifiedName: String,
      loader: ClassLoader)(
      implicit A: ClassTag[A],
      C: ClassTag[C]): A => C = {
    Reflect.lookupInstantiatableClass(qualifiedName) match {
      case None =>
        throw new Exception(s"Could not find class $qualifiedName")
          with NoStackTrace
      case Some(cls) => cls.getConstructor(A.runtimeClass) match {
          case None =>
            val message =
              s"${cls.runtimeClass} is a class. It should either be an object, or have a constructor that takes a single parameter of type ${A.runtimeClass.getName()}"
            throw new Exception(message) with scala.util.control.NoStackTrace
          case Some(cst) => (a: A) => cast[C](cst.newInstance(a))
        }
    }
  }

  protected[framework] def cast[T](any: Any)(
      implicit T: ClassTag[T]): T = any match {
    case suite if T.runtimeClass.isInstance(suite) =>
      suite.asInstanceOf[T]
    case other =>
      throw new Exception(
        s"$other is not an instance of ${T.runtimeClass.getName()}")
        with NoStackTrace
  }

  protected[framework] def loadModule(
      qualifiedName: String,
      loader: ClassLoader): Any = {
    val moduleName = qualifiedName + "$"
    Reflect.lookupLoadableModuleClass(moduleName) match {
      case None =>
        throw new Exception(s"Could not load object $moduleName")
          with NoStackTrace
      case Some(cls) => cls.loadModule()
    }
  }

}
