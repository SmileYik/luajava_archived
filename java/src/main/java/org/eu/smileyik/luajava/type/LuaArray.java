package org.eu.smileyik.luajava.type;

import org.keplerproject.luajava.LuaState;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class LuaArray extends LuaTable {

    public LuaArray(LuaState L, int index) {
        super(L, index);
    }

    @Override
    public String toString() {
        return "[Lua Array]";
    }

    @Override
    public boolean isArray() {
        return true;
    }

    public int length() {
        synchronized (L) {
            push();
            int i = L.objLen(-1);
            L.pop(1);
            return i;
        }
    }

    public List<Object> asList() throws Throwable {
        return asList(Object.class);
    }

    public <T> List<T> asList(Class<T> clazz) throws Throwable {
        List<T> list = new ArrayList<>();
        forEachValue(clazz, list::add);
        return list;
    }

    public Object[] asArray() throws Throwable {
        return asArray(Object.class);
    }

    public <T> T[] asArray(Class<T> clazz) throws Throwable {
        List<T> list = asList(clazz);
        T[] t = (T[]) Array.newInstance(clazz, 0);
        return list.toArray(t);
    }

    public void forEachValue(Consumer<Object> consumer) throws Throwable {
        forEachValue(Object.class, consumer);
    }

    /**
     * just for value.
     * @param tClass   element type
     * @param consumer consumer
     * @param <T>      element type, Cannot be primitive type
     * @throws Throwable any exception
     */
    public <T> void forEachValue(Class<T> tClass, Consumer<T> consumer) throws Throwable {
        synchronized (L) {
            push();
            int len = L.objLen(-1);
            for (int i = 1; i <= len; i++) {
                L.rawGetI(-1, i);
                try {
                    Object javaObject = L.toJavaObject(-1);
                    consumer.accept(tClass.cast(javaObject));
                } catch (Throwable e) {
                    L.pop(2);
                    throw e;
                } finally {
                    L.pop(1);
                }
            }
            L.pop(1);
        }
    }

    /**
     * it's array version.
     * @param kClass   Key type, Always be <code>Integer</code>
     * @param vClass   Value type
     * @param consumer consumer
     * @param <K> Always be <code>Integer</code>, Cannot be primitive type
     * @param <V> Value type, Cannot be primitive type
     * @throws Throwable any exception.
     */
    @Override
    public <K, V> void forEach(Class<K> kClass, Class<V> vClass, BiConsumer<K, V> consumer) throws Throwable {
        synchronized (L) {
            push();
            int len = L.objLen(-1);
            for (int i = 1; i <= len; i++) {
                L.rawGetI(-1, i);
                try {
                    Object javaObject = L.toJavaObject(-1);
                    consumer.accept(kClass.cast(i - 1), vClass.cast(javaObject));
                } catch (Throwable e) {
                    L.pop(2);
                    throw e;
                } finally {
                    L.pop(1);
                }
            }
            L.pop(1);
        }
    }

    public <V> void forEach(Class<V> vClass, BiConsumer<Integer, V> consumer) throws Throwable {
        forEach(Integer.class, vClass, consumer);
    }

    /**
     * foreach table entry. it will stop if throws exception.
     * @param consumer consumer, the first type always be <code>Integer</code>
     * @throws Throwable any exception
     */
    @Override
    public void forEach(BiConsumer<Object, Object> consumer) throws Throwable {
        forEach(Object.class, Object.class, consumer);
    }
}
