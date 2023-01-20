package org.folio.entlinks.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ObjectUtils {

  /**
   * Analyze two objects and identify which fields have different values. Comparison is based on getters return results.
   *
   * @param s1 first object.
   * @param s2 second object.
   * @return fieldNames that have different values
   */
  public static <T> List<Difference> getDifference(T s1, T s2)
    throws InvocationTargetException, IllegalAccessException {

    T obj = s1 != null ? s1 : s2;

    List<Difference> values = new ArrayList<>();
    for (Method method : obj.getClass().getMethods()) {
      if (method.getName().startsWith("get")) {
        var value1 = s1 != null ? method.invoke(s1) : null;
        var value2 = s2 != null ? method.invoke(s2) : null;
        if (!Objects.equals(value1, value2)) {
          var fieldName = method.getName().substring(3);
          values.add(new Difference(fieldName, value1, value2));
        }
      }
    }
    return values;
  }

  public record Difference(String fieldName, Object val1, Object val2) { }
}
