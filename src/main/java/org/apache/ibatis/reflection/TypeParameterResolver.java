/**
 *    Copyright 2009-2018 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.reflection;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;

/**
 * @author Iwao AVE!
 *TypeParameterResolver是一个工具类，提供一系列的静态方法，去解析类中的字段、方法返回值、方法参数的类型
 * <p><a herf="https://www.jianshu.com/p/7649f86614d3">《我眼中的 Java-Type 体系(1)》</a><p/>
 * https://www.cnblogs.com/wly1-6/p/10303041.html
 *
 *
 * <p>在正式介绍TypeParameterResolver之前，先介绍一个JDK提供的接口Type，因为TypeParameterResolver的实现会涉及到它的实现；它是所有类型的父接口，在JDK实现中，Type接口拥有四个接口和一个实现类分别代表不同的数据类型；
 *
 * 分别是：
 *<p>
 * 类Class：表示原始类型。Class对象表示JVM的一个类和接口，每个java类在JVM里都是一个Class对象，在程序中可以通过"类名.class","对象.getClass","Class.forName()"获取到，数组也被映射为Class对象，所有元素类型相同且维数相同的数组共享同一个Class对象；
 *
 * <p>
 * 接口ParameterizedType：表示的是参数化类型，例如：List<String>、Map<Integer,String>这种带范型的类型；
 *
 * <p>
 * 接口TypeVariable：表示的类型变量，用来反映在JVM编译泛型前的信息，例如：List<T>中的T就是类型变量，在编译时需被转换成一个具体的类型后才能被使用。
 *
 * <p>
 * 接口GenericArrayType：表示的是数组类型且组成元素是ParameterizedType或TypeVariable。例如：List<String>[]或T[]
 * <p>
 * 接口WildcardType：表示的是通配符类型，；例如 ? extends Number 和 ? super Integer 。
 *
 * <p>Type的实现和子接口源码就不贴出来了，下面会介绍一下上述四个接口的主要方法：
 *
 * 接口ParameterizedType：
 * Type  getRawType( )——返回参数化类型的最外层类型，例如List<String> => List;
 * Type[ ]  getActualTypeArguments( )——获取参数化类型的类型变量或者实际类型列表，例如Map<Integer,String>的实际类型列表Integer和String。需要注意的是该列表的元素也是Type接口，可能存在多层嵌套的情况；
 * Type  getOwnerType( )——返回的是类型所属的类型，例如存在A<T>类，其中定义了内部类InnerA<I>，则InnerA<I>的所属类型为A<T>，如果是顶层类型，则返回null；
 *
 * <p>
 * 接口TypeVariable：
 * Type[ ]  getBounds( )——获取类型变量的上边界，如未明确声明上边界则默认为Object，例如：class Test<K extends Person>中K的上边界就是Person；
 * D  getGenericDecralation( )——获取声明该类型变量的原始类型，例如class Test<K extends Person>中的原始类型就是Test；
 * String  getName( )——获取在源码中定义是的名字，上例为K;
 * <p>
 * 接口GenericArrayType：Type  getGenericComponentType( )——返回数组的元素类型
 * <p>
 * 接口WildcardType：
 * Type[ ]  getUpperBounds( )——返回泛型变量的上界；
 * Type[ ]  getLowerBounds( )——返回泛型变量的下界；
 *
 *
 *
 */
public class TypeParameterResolver {


    /**
     * srcType：被反射时调用的类型，即被解析的方法或字段是通过那个类型反射出来的
     *
     * declaringClass：定义被解析的方法或字段的class类型，即这个方法/字段是定义在那个class中的
     *
     * example:
     *
     * <p>
     *     public interface A<N>{
     *          public N getVal();
     *     }
     *
     *     public interface B extends A<String>{}
     * </p>
     * 如上面代码所示，如果通过B接口获取到的getVal方法并对其返回值进行解析，则srcType表示B的类型，declaringClass表示A的类型
     */

  /**
   * 解析属性类型
   * @return The field type as {@link Type}. If it has type parameters in the declaration,<br>
   *         they will be resolved to the actual runtime {@link Type}s.
   */
  public static Type resolveFieldType(Field field, Type srcType) {
      // 属性类型
    Type fieldType = field.getGenericType();
      // 定义的类
    Class<?> declaringClass = field.getDeclaringClass();
      // 解析类型
    return resolveType(fieldType, srcType, declaringClass);
  }

  /**
   * 解析方法返回类型
   * @return The return type of the method as {@link Type}. If it has type parameters in the declaration,<br>
   *         they will be resolved to the actual runtime {@link Type}s.
   */
  public static Type resolveReturnType(Method method, Type srcType) {
      //获取方法的返回类型
    Type returnType = method.getGenericReturnType();
      // 获取字段定义所在的类的Class对象
    Class<?> declaringClass = method.getDeclaringClass();
      // 调用 resolveType方法进行后续处理
    return resolveType(returnType, srcType, declaringClass);
  }

  /**
   * 解析方法参数的类型数组
   * @return The parameter types of the method as an array of {@link Type}s. If they have type parameters in the declaration,<br>
   *         they will be resolved to the actual runtime {@link Type}s.
   */
  public static Type[] resolveParamTypes(Method method, Type srcType) {
      // 获得方法参数类型数组 获取方法所有参数类型
    Type[] paramTypes = method.getGenericParameterTypes();
      //获取方法定义的类类型
    Class<?> declaringClass = method.getDeclaringClass();
      // 解析类型们
    Type[] result = new Type[paramTypes.length];
    for (int i = 0; i < paramTypes.length; i++) {
        // 调用 resolveType方法进行后续处理
      result[i] = resolveType(paramTypes[i], srcType, declaringClass);
    }
    return result;
  }

    /**
     * 解析类型
     *
     * @param type 类型
     * @param srcType 来源类型
     * @param declaringClass 定义的类
     * @return 解析后的类型
     */
  private static Type resolveType(Type type, Type srcType, Class<?> declaringClass) {
    if (type instanceof TypeVariable) {
      return resolveTypeVar((TypeVariable<?>) type, srcType, declaringClass);
    } else if (type instanceof ParameterizedType) {
      return resolveParameterizedType((ParameterizedType) type, srcType, declaringClass);
    } else if (type instanceof GenericArrayType) {
      return resolveGenericArrayType((GenericArrayType) type, srcType, declaringClass);
    } else {
      return type;
    }
  }

  private static Type resolveGenericArrayType(GenericArrayType genericArrayType, Type srcType, Class<?> declaringClass) {
      //去掉一层[]后的泛型类型变量,数组类型
    Type componentType = genericArrayType.getGenericComponentType();
    Type resolvedComponentType = null;
      //根据去掉一维数组后的类型变量，再根据其类型递归解析
    if (componentType instanceof TypeVariable) {
      resolvedComponentType = resolveTypeVar((TypeVariable<?>) componentType, srcType, declaringClass);
    } else if (componentType instanceof GenericArrayType) {
      resolvedComponentType = resolveGenericArrayType((GenericArrayType) componentType, srcType, declaringClass);
    } else if (componentType instanceof ParameterizedType) {
      resolvedComponentType = resolveParameterizedType((ParameterizedType) componentType, srcType, declaringClass);
    }
      // 【2】创建 GenericArrayTypeImpl 对象
    if (resolvedComponentType instanceof Class) {
      return Array.newInstance((Class<?>) resolvedComponentType, 0).getClass();
    } else {
      return new GenericArrayTypeImpl(resolvedComponentType);
    }
  }

    /**
     * 解析 ParameterizedType 类型
     *
     * @param parameterizedType ParameterizedType 类型
     * @param srcType 来源类型
     * @param declaringClass 定义的类
     * @return 解析后的类型
     */
  private static ParameterizedType resolveParameterizedType(ParameterizedType parameterizedType, Type srcType, Class<?> declaringClass) {
    Class<?> rawType = (Class<?>) parameterizedType.getRawType();
      // 【1】解析 <> 中实际类型
    Type[] typeArgs = parameterizedType.getActualTypeArguments();
    Type[] args = new Type[typeArgs.length];
    for (int i = 0; i < typeArgs.length; i++) {
      if (typeArgs[i] instanceof TypeVariable) {
        args[i] = resolveTypeVar((TypeVariable<?>) typeArgs[i], srcType, declaringClass);
      } else if (typeArgs[i] instanceof ParameterizedType) {
        args[i] = resolveParameterizedType((ParameterizedType) typeArgs[i], srcType, declaringClass);
      } else if (typeArgs[i] instanceof WildcardType) {
        args[i] = resolveWildcardType((WildcardType) typeArgs[i], srcType, declaringClass);
      } else {
        args[i] = typeArgs[i];
      }
    }
      // 【2】创建 ParameterizedTypeImpl 对象
    return new ParameterizedTypeImpl(rawType, null, args);
  }

    /**
     * 解析 WildcardType 类型
     * @param wildcardType
     * @param srcType
     * @param declaringClass
     * @return
     */
  private static Type resolveWildcardType(WildcardType wildcardType, Type srcType, Class<?> declaringClass) {
      // <1.1> 解析泛型表达式下界（下限 super）
    Type[] lowerBounds = resolveWildcardTypeBounds(wildcardType.getLowerBounds(), srcType, declaringClass);
      // <1.2> 解析泛型表达式上界（上限 extends）
    Type[] upperBounds = resolveWildcardTypeBounds(wildcardType.getUpperBounds(), srcType, declaringClass);
      // <2> 创建 WildcardTypeImpl 对象
    return new WildcardTypeImpl(lowerBounds, upperBounds);
  }

  private static Type[] resolveWildcardTypeBounds(Type[] bounds, Type srcType, Class<?> declaringClass) {
    Type[] result = new Type[bounds.length];
    for (int i = 0; i < bounds.length; i++) {
      if (bounds[i] instanceof TypeVariable) {
        result[i] = resolveTypeVar((TypeVariable<?>) bounds[i], srcType, declaringClass);
      } else if (bounds[i] instanceof ParameterizedType) {
        result[i] = resolveParameterizedType((ParameterizedType) bounds[i], srcType, declaringClass);
      } else if (bounds[i] instanceof WildcardType) {
        result[i] = resolveWildcardType((WildcardType) bounds[i], srcType, declaringClass);
      } else {
        result[i] = bounds[i];
      }
    }
    return result;
  }

    /**
     * 解析具体的类型变量指代的类型。
     * 1.如果srcType的Class类型和declaringClass为同一个类，表示获取该类型变量时被反射的类型就是其定义的类型，
     *      则取该类型变量定义是有没有上限，如果有则使用其上限代表其类型，否则就用Object。
     *
     * 2.如果不是，则代表declaringClass是srcType的父类或者实现的接口，则解析继承中有没有定义其代表的类型
     */
  private static Type resolveTypeVar(TypeVariable<?> typeVar, Type srcType, Class<?> declaringClass) {
    Type result = null;
    Class<?> clazz = null;
    if (srcType instanceof Class) {
      clazz = (Class<?>) srcType;
    } else if (srcType instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) srcType;
      clazz = (Class<?>) parameterizedType.getRawType();
    } else {
      throw new IllegalArgumentException("The 2nd arg must be Class or ParameterizedType, but was: " + srcType.getClass());
    }

    if (clazz == declaringClass) {
      Type[] bounds = typeVar.getBounds();
      if(bounds.length > 0) {
        return bounds[0];
      }
      return Object.class;
    }

    Type superclass = clazz.getGenericSuperclass();
    result = scanSuperTypes(typeVar, srcType, declaringClass, clazz, superclass);
    if (result != null) {
      return result;
    }

    Type[] superInterfaces = clazz.getGenericInterfaces();
    for (Type superInterface : superInterfaces) {
      result = scanSuperTypes(typeVar, srcType, declaringClass, clazz, superInterface);
      if (result != null) {
        return result;
      }
    }
    return Object.class;
  }

  private static Type scanSuperTypes(TypeVariable<?> typeVar, Type srcType, Class<?> declaringClass, Class<?> clazz, Type superclass) {
    if (superclass instanceof ParameterizedType) {
      ParameterizedType parentAsType = (ParameterizedType) superclass;
      Class<?> parentAsClass = (Class<?>) parentAsType.getRawType();
      TypeVariable<?>[] parentTypeVars = parentAsClass.getTypeParameters();
      if (srcType instanceof ParameterizedType) {
        parentAsType = translateParentTypeVars((ParameterizedType) srcType, clazz, parentAsType);
      }
      if (declaringClass == parentAsClass) {
        for (int i = 0; i < parentTypeVars.length; i++) {
          if (typeVar == parentTypeVars[i]) {
            return parentAsType.getActualTypeArguments()[i];
          }
        }
      }
      if (declaringClass.isAssignableFrom(parentAsClass)) {
        return resolveTypeVar(typeVar, parentAsType, declaringClass);
      }
    } else if (superclass instanceof Class && declaringClass.isAssignableFrom((Class<?>) superclass)) {
      return resolveTypeVar(typeVar, superclass, declaringClass);
    }
    return null;
  }

  private static ParameterizedType translateParentTypeVars(ParameterizedType srcType, Class<?> srcClass, ParameterizedType parentType) {
    Type[] parentTypeArgs = parentType.getActualTypeArguments();
    Type[] srcTypeArgs = srcType.getActualTypeArguments();
    TypeVariable<?>[] srcTypeVars = srcClass.getTypeParameters();
    Type[] newParentArgs = new Type[parentTypeArgs.length];
    boolean noChange = true;
    for (int i = 0; i < parentTypeArgs.length; i++) {
      if (parentTypeArgs[i] instanceof TypeVariable) {
        for (int j = 0; j < srcTypeVars.length; j++) {
          if (srcTypeVars[j] == parentTypeArgs[i]) {
            noChange = false;
            newParentArgs[i] = srcTypeArgs[j];
          }
        }
      } else {
        newParentArgs[i] = parentTypeArgs[i];
      }
    }
    return noChange ? parentType : new ParameterizedTypeImpl((Class<?>)parentType.getRawType(), null, newParentArgs);
  }

  private TypeParameterResolver() {
    super();
  }

    /**
     * ParameterizedType 实现类
     *
     * 参数化类型，即泛型。例如：List<T>、Map<K, V>等带有参数化的配置
     */
  static class ParameterizedTypeImpl implements ParameterizedType {
      // 以 List<T> 举例子

      /**
       * <> 前面实际类型
       *
       * 例如：List
       */
    private Class<?> rawType;

      /**
       * 如果这个类型是某个属性所有，则获取这个所有者类型；否则，返回 null
       */
    private Type ownerType;
      /**
       * <> 中实际类型
       *
       * 例如：T
       */
    private Type[] actualTypeArguments;


    public ParameterizedTypeImpl(Class<?> rawType, Type ownerType, Type[] actualTypeArguments) {
      super();
      this.rawType = rawType;
      this.ownerType = ownerType;
      this.actualTypeArguments = actualTypeArguments;
    }

    @Override
    public Type[] getActualTypeArguments() {
      return actualTypeArguments;
    }

    @Override
    public Type getOwnerType() {
      return ownerType;
    }

    @Override
    public Type getRawType() {
      return rawType;
    }

    @Override
    public String toString() {
      return "ParameterizedTypeImpl [rawType=" + rawType + ", ownerType=" + ownerType + ", actualTypeArguments=" + Arrays.toString(actualTypeArguments) + "]";
    }
  }

    /**
     * WildcardType 实现类
     *
     * 泛型表达式（或者通配符表达式），即 ? extend Number、? super Integer 这样的表达式。
     * WildcardType 虽然是 Type 的子接口，但却不是 Java 类型中的一种。
     */
  static class WildcardTypeImpl implements WildcardType {
        /**
         * 泛型表达式下界（下限 super）
         */
    private Type[] lowerBounds;

        /**
         * 泛型表达式上界（上界 extends）
         */
    private Type[] upperBounds;

    WildcardTypeImpl(Type[] lowerBounds, Type[] upperBounds) {
      super();
      this.lowerBounds = lowerBounds;
      this.upperBounds = upperBounds;
    }

    @Override
    public Type[] getLowerBounds() {
      return lowerBounds;
    }

    @Override
    public Type[] getUpperBounds() {
      return upperBounds;
    }
  }

    /**
     * GenericArrayType 实现类
     *
     * 泛型数组类型，用来描述 ParameterizedType、TypeVariable 类型的数组；即 List<T>[]、T[] 等；
     */
  static class GenericArrayTypeImpl implements GenericArrayType {
        /**
         * 数组元素类型
         */
    private Type genericComponentType;

    GenericArrayTypeImpl(Type genericComponentType) {
      super();
      this.genericComponentType = genericComponentType;
    }

    @Override
    public Type getGenericComponentType() {
      return genericComponentType;
    }
  }
}
