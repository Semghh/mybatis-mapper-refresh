package org.example.mybatisautorefresh;


import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * @author SEMGHH
 * @date 2024/11/6 10:55
 */
public class Permit {

    public static Unsafe getUnsafe(){
        return (Unsafe) Permit.ReflectiveStaticField(Unsafe.class,"theUnsafe");
    }

    /**
     * 从cls中找到指定静态字段的值
     * @param cls 指定类
     * @param fieldName 指定静态字段
     * @return 静态字段的值。 可能为null
     */
    public static Object ReflectiveStaticField(Class<?> cls,String fieldName){
        try {
            Field f =  cls.getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    /**
     * 从clz中找到指定字段
     * @param clz 指定类
     * @param fieldName 指定字段的名
     * @return 如果找到了返回字段Field ，找不到则返回null
     */
    public static Field GetField(Class<?> clz,String fieldName) throws NoSuchFieldException{

        Class<?> c = clz;
        Field f = null;
        while (c != null){
            try {
                f = c.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        if (f==null){
            throw new NoSuchFieldException(c.getName() + "::" + fieldName);
        }
        f.setAccessible(true);
        return f;
    }

    public static <T,M>  T getObjFromField(Class<M> clz,String fieldName ,M o) throws NoSuchFieldException, IllegalAccessException {
        Field field = GetField(clz, fieldName);
        field.setAccessible(true);
        return (T) field.get(o);
    }

}
