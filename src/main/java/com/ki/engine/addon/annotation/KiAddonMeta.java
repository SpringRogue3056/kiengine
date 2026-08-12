package com.ki.engine.addon.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface KiAddonMeta {
    String id();
    String name() default "";
    String version() default "1.0.0";
    String[] authors() default {};
    String[] depend() default {};
    String[] softDepend() default {};
    String description() default "";
}
