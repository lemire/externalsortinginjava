/**
 *
 */
package com.google.code.externalsorting;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;

/**
 * Simple class used to estimate memory usage.
 *
 * @author Eleftherios Chetzakis
 */
public final class StringSizeEstimator {


    /**
     * Private constructor to prevent instantiation.
     */
    private StringSizeEstimator() {
    }



    /**
     * Estimates the size of a {@link String} object in bytes.
     *
     * @param s The string to estimate memory footprint.
     * @return The <strong>estimated</strong> size in bytes.
     */
    public static long estimatedSizeOf(String s) {

        Unsafe unsafe=getUnsafe();
        HashSet<Field> fields=new HashSet<>();
        Class c= s.getClass();
      for(Field f : c.getDeclaredFields()){
          if((f.getModifiers()&Modifier.STATIC)==0){
              fields.add(f);
          }
      }
      long maxSize=0;
        for(Field f: fields){
            long offset= unsafe.objectFieldOffset(f);
            maxSize=Math.max(maxSize,offset);
        }
        return (maxSize/8+1)*8;
    }
/*
* Get Unsafe instance
*
* */
    private static Unsafe getUnsafe() {
        Field f = null;
        Unsafe unsafe = null;
        try {
            f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return unsafe;
    }

}
