package JSF.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD) 

@Retention(RetentionPolicy.RUNTIME) 

public @interface UrlMapping {
      public String value();
      public String method();
}
