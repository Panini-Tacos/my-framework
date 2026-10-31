package JSF.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD) 

@Retention(RetentionPolicy.RUNTIME) 

public @interface WebAPI {
      public String api();
      public boolean toJSON();
}
