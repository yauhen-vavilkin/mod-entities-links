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
  public static <T> List<String> getDifference(T s1, T s2)
    throws InvocationTargetException, IllegalAccessException {
    List<String> values = new ArrayList<>();
    for (Method method : s1.getClass().getMethods()) {
      if (method.getName().startsWith("get")) {
        var value1 = method.invoke(s1);
        var value2 = method.invoke(s2);
        if (!Objects.equals(value1, value2)) {
          var fieldName = method.getName().substring(3);
          values.add(fieldName);
        }
      }
    }
    return values;
  }
}
