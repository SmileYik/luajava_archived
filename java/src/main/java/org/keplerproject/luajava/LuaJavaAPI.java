/*
 * $Id: LuaJavaAPI.java,v 1.5 2007-04-17 23:47:50 thiago Exp $
 * Copyright (C) 2003-2007 Kepler Project.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.keplerproject.luajava;

import java.lang.reflect.*;
import java.util.Optional;

/**
 * Class that contains functions accessed by lua.
 *
 * @author Thiago Ponte
 */
public final class LuaJavaAPI {

    private LuaJavaAPI() {
    }

    /**
     * Java implementation of the metamethod __index for normal objects
     *
     * @param luaState   int that indicates the state used
     * @param obj        Object to be indexed
     * @param methodName the name of the method
     * @return number of returned objects
     */
    public static int objectIndex(int luaState, Object obj, String methodName)
            throws LuaException {
        LuaState L = LuaStateFactory.getExistingState(luaState);

        synchronized (L) {
            int top = L.getTop();

            Object[] objs = new Object[top - 1];
            Method method = null;

            Class<?> clazz;

            if (obj instanceof Class) {
                clazz = (Class<?>) obj;
            } else {
                clazz = obj.getClass();
            }
            method = findMethod(L, clazz, methodName, objs, top);

            // If method is null means there isn't one receiving the given arguments
            if (method == null) {
                throw new LuaException("Invalid method call. No such method.");
            }

            Object ret;
            try {
                if (Modifier.isPublic(method.getModifiers())) {
                    method.setAccessible(true);
                }

                //if (obj instanceof Class)
                if (Modifier.isStatic(method.getModifiers())) {
                    ret = method.invoke(null, objs);
                } else {
                    ret = method.invoke(obj, objs);
                }
            } catch (Exception e) {
                throw new LuaException(e);
            }

            // Void function returns null
            if (ret == null) {
                return 0;
            }

            // push result
            L.pushObjectValue(ret);

            return 1;
        }
    }

    /**
     * Java function that implements the __index for Java arrays
     *
     * @param luaState int that indicates the state used
     * @param obj      Object to be indexed
     * @param index    index number of array. Since Lua index starts from 1,
     *                 the number used will be (index - 1)
     * @return number of returned objects
     */
    public static int arrayIndex(int luaState, Object obj, int index) throws LuaException {
        LuaState L = LuaStateFactory.getExistingState(luaState);

        synchronized (L) {

            if (!obj.getClass().isArray())
                throw new LuaException("Object indexed is not an array.");

            if (Array.getLength(obj) < index)
                throw new LuaException("Index out of bounds.");

            L.pushObjectValue(Array.get(obj, index - 1));

            return 1;
        }
    }

    /**
     * Java function to be called when a java Class metamethod __index is called.
     * This function returns 1 if there is a field with searchName and 2 if there
     * is a method if the searchName
     *
     * @param luaState   int that represents the state to be used
     * @param clazz      class to be indexed
     * @param searchName name of the field or method to be accessed
     * @return number of returned objects
     * @throws LuaException
     */
    public static int classIndex(int luaState, Class<?> clazz, String searchName)
            throws LuaException {
        synchronized (LuaStateFactory.getExistingState(luaState)) {
            int res;

            res = checkField(luaState, clazz, searchName);

            if (res != 0) {
                return 1;
            }

            res = checkMethod(luaState, clazz, searchName);

            if (res != 0) {
                return 2;
            }

            return 0;
        }
    }


    /**
     * Java function to be called when a java object metamethod __newindex is called.
     *
     * @param luaState  int that represents the state to be used
     * @param obj       to be used
     * @param fieldName name of the field to be set
     * @return number of returned objects
     * @throws LuaException
     */
    public static int objectNewIndex(int luaState, Object obj, String fieldName)
            throws LuaException {
        LuaState L = LuaStateFactory.getExistingState(luaState);

        synchronized (L) {
            Field field = null;
            Class<?> objClass;

            if (obj instanceof Class) {
                objClass = (Class<?>) obj;
            } else {
                objClass = obj.getClass();
            }

            try {
                field = objClass.getField(fieldName);
            } catch (Exception e) {
                throw new LuaException("Error accessing field.", e);
            }

            Class<?> type = field.getType();
            Optional<Object> setObjRet = compareTypes(L, type, 3);
            if (!setObjRet.isPresent()) {
                throw new LuaException("Invalid type.");
            }
            Object setObj = setObjRet.get();

            if (field.isAccessible())
                field.setAccessible(true);

            try {
                field.set(obj, setObj);
            } catch (IllegalArgumentException e) {
                throw new LuaException("Ilegal argument to set field.", e);
            } catch (IllegalAccessException e) {
                throw new LuaException("Field not accessible.", e);
            }
        }

        return 0;
    }


    /**
     * Java function to be called when a java array metamethod __newindex is called.
     *
     * @param luaState int that represents the state to be used
     * @param obj      to be used
     * @param index    index number of array. Since Lua index starts from 1,
     *                 the number used will be (index - 1)
     * @return number of returned objects
     * @throws LuaException
     */
    public static int arrayNewIndex(int luaState, Object obj, int index)
            throws LuaException {
        LuaState L = LuaStateFactory.getExistingState(luaState);

        synchronized (L) {
            if (!obj.getClass().isArray())
                throw new LuaException("Object indexed is not an array.");

            if (Array.getLength(obj) < index)
                throw new LuaException("Index out of bounds.");

            Class<?> type = obj.getClass().getComponentType();
            Optional<Object> setObjRet = compareTypes(L, type, 3);
            if (!setObjRet.isPresent()) {
                throw new LuaException("Invalid type.");
            }
            Object setObj = setObjRet.get();

            Array.set(obj, index - 1, setObj);
        }

        return 0;
    }


    /**
     * Pushes a new instance of a java Object of the type className
     *
     * @param luaState  int that represents the state to be used
     * @param className name of the class
     * @return number of returned objects
     * @throws LuaException
     */
    public static int javaNewInstance(int luaState, String className)
            throws LuaException {
        LuaState L = LuaStateFactory.getExistingState(luaState);

        synchronized (L) {
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new LuaException(e);
            }
            Object ret = getObjInstance(L, clazz);

            L.pushJavaObject(ret);

            return 1;
        }
    }

    /**
     * javaNew returns a new instance of a given clazz
     *
     * @param luaState int that represents the state to be used
     * @param clazz    class to be instanciated
     * @return number of returned objects
     * @throws LuaException
     */
    public static int javaNew(int luaState, Class<?> clazz) throws LuaException {
        LuaState L = LuaStateFactory.getExistingState(luaState);

        synchronized (L) {
            Object ret = getObjInstance(L, clazz);

            L.pushJavaObject(ret);

            return 1;
        }
    }

    /**
     * Calls the static method <code>methodName</code> in class <code>className</code>
     * that receives a LuaState as first parameter.
     *
     * @param luaState   int that represents the state to be used
     * @param className  name of the class that has the open library method
     * @param methodName method to open library
     * @return number of returned objects
     * @throws LuaException
     */
    public static int javaLoadLib(int luaState, String className, String methodName)
            throws LuaException {
        LuaState L = LuaStateFactory.getExistingState(luaState);

        synchronized (L) {
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new LuaException(e);
            }

            try {
                Method mt = clazz.getMethod(methodName, LuaState.class);
                Object obj = mt.invoke(null, L);

                if (obj instanceof Integer) {
                    return (Integer) obj;
                } else
                    return 0;
            } catch (Exception e) {
                throw new LuaException("Error on calling method. Library could not be loaded. " + e.getMessage());
            }
        }
    }

    private static Object getObjInstance(LuaState L, Class<?> clazz)
            throws LuaException {
        synchronized (L) {
            int top = L.getTop();

            Object[] objs = new Object[top - 1];

            Constructor<?>[] constructors = clazz.getConstructors();
            Constructor<?> constructor = null;

            // gets method and arguments
            for (int i = 0; i < constructors.length; i++) {
                Class<?>[] parameters = constructors[i].getParameterTypes();
                if (parameters.length != top - 1)
                    continue;

                boolean okConstruc = true;

                for (int j = 0; j < parameters.length; j++) {
                    Optional<Object> ret = compareTypes(L, parameters[j], j + 2);
                    if (ret.isPresent()) {
                        objs[j] = ret.get();
                    } else {
                        okConstruc = false;
                        break;
                    }
                }

                if (okConstruc) {
                    constructor = constructors[i];
                    break;
                }

            }

            // If method is null means there isn't one receiving the given arguments
            if (constructor == null) {
                throw new LuaException("Invalid method call. No such method.");
            }

            Object ret;
            try {
                ret = constructor.newInstance(objs);
            } catch (Exception e) {
                throw new LuaException(e);
            }

            if (ret == null) {
                throw new LuaException("Couldn't instantiate java Object");
            }

            return ret;
        }
    }

    /**
     * Checks if there is a field on the obj with the given name
     *
     * @param luaState  int that represents the state to be used
     * @param obj       object to be inspected
     * @param fieldName name of the field to be inpected
     * @return number of returned objects
     */
    public static int checkField(int luaState, Object obj, String fieldName)
            throws LuaException {
        LuaState L = LuaStateFactory.getExistingState(luaState);

        synchronized (L) {
            Field field = null;
            Class<?> objClass;

            if (obj instanceof Class) {
                objClass = (Class<?>) obj;
            } else {
                objClass = obj.getClass();
            }

            try {
                field = objClass.getField(fieldName);
            } catch (Exception e) {
                return 0;
            }

            if (field == null) {
                return 0;
            }

            Object ret = null;
            try {
                ret = field.get(obj);
            } catch (Exception e1) {
                return 0;
            }

            if (obj == null) {
                return 0;
            }

            L.pushObjectValue(ret);

            return 1;
        }
    }

    /**
     * Checks to see if there is a method with the given name.
     *
     * @param luaState   int that represents the state to be used
     * @param obj        object to be inspected
     * @param methodName name of the field to be inpected
     * @return number of returned objects
     */
    private static int checkMethod(int luaState, Object obj, String methodName) {
        LuaState L = LuaStateFactory.getExistingState(luaState);

        synchronized (L) {
            Class<?> clazz;

            if (obj instanceof Class) {
                clazz = (Class<?>) obj;
            } else {
                clazz = obj.getClass();
            }

            Method[] methods = clazz.getMethods();

            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals(methodName))
                    return 1;
            }

            return 0;
        }
    }

    /**
     * Function that creates an object proxy and pushes it into the stack
     *
     * @param luaState int that represents the state to be used
     * @param implem   interfaces implemented separated by comma (<code>,</code>)
     * @return number of returned objects
     * @throws LuaException
     */
    public static int createProxyObject(int luaState, String implem)
            throws LuaException {
        LuaState L = LuaStateFactory.getExistingState(luaState);

        synchronized (L) {
            try {
                if (!(L.isTable(2)))
                    throw new LuaException(
                            "Parameter is not a table. Can't create proxy.");

                LuaObject luaObj = L.getLuaObject(2);

                Object proxy = luaObj.createProxy(implem);
                L.pushJavaObject(proxy);
            } catch (Exception e) {
                throw new LuaException(e);
            }

            return 1;
        }
    }

    private static Optional<Object> compareTypes(LuaState L, Class<?> parameter, int idx)
            throws LuaException {
        boolean okType = true;
        Object obj = null;

        // if parameter type is Object then just cast lua type to java type.
        if (parameter == Object.class) {
            return Optional.of(L.toJavaObject(idx));
        }

        int luaType = L.type(idx);
        if (luaType == LuaState.LUA_TBOOLEAN) {
            if (
                    parameter.isPrimitive() && parameter == Boolean.TYPE ||
                            Boolean.class.isAssignableFrom(parameter)
            ) {
                obj = L.toBoolean(idx);
            } else {
                okType = false;
            }
        } else if (luaType == LuaState.LUA_TSTRING) {
            if (String.class.isAssignableFrom(parameter)) {
                obj = L.toString(idx);
            } else {
                okType = false;
            }
        } else if (luaType == LuaState.LUA_TFUNCTION) {
            if (LuaObject.class.isAssignableFrom(parameter)) {
                obj = L.getLuaObject(idx);
            } else {
                okType = false;
            }
        } else if (luaType == LuaState.LUA_TTABLE) {
            if (!LuaObject.class.isAssignableFrom(parameter)) {
                okType = false;
            } else {
                obj = L.getLuaObject(idx);
            }
        } else if (luaType == LuaState.LUA_TNUMBER) {
            Double db = L.toNumber(idx);

            obj = LuaState.convertLuaNumber(db, parameter);
            if (obj == null) {
                okType = false;
            }
        } else if (luaType == LuaState.LUA_TUSERDATA) {
            if (L.isObject(idx)) {
                Object userObj = L.getObjectFromUserdata(idx);
                if (userObj.getClass().isAssignableFrom(parameter)) {
                    obj = userObj;
                } else {
                    okType = false;
                }
            } else {
                if (LuaObject.class.isAssignableFrom(parameter)) {
                    obj = L.getLuaObject(idx);
                } else {
                    okType = false;
                }
            }
        } else if (luaType != LuaState.LUA_TNIL) {
            okType = false;
        }

//        if (!okType) {
//            throw new LuaException(String.format(
//                    "Invalid Parameter: expected %s, got %s", parameter, LuaType.typeName(luaType)
//            ));
//        }

        return okType ? Optional.ofNullable(obj) : Optional.empty();
    }

    private static Method findMethod(LuaState L, Class<?> clazz, String methodName, Object[] retObjs, int top) {
        Class<?> c = clazz;
        Object[] objs = new Object[top - 1];
        int paramsCount = top - 1;
        Method mached = null;

        while (c != null) {
            for (Method method : c.getDeclaredMethods()) {
                if (!method.getName().equals(methodName) || method.getParameterCount() != paramsCount) {
                    continue;
                }
                mached = method;
                Class<?>[] parameters = method.getParameterTypes();
                for (int i = 0; i < paramsCount; i++) {
                    Class<?> paramType = parameters[i];
                    Optional<Object> ret;
                    try {
                        ret = compareTypes(L, paramType, i + 2);
                    } catch (LuaException e) {
                        ret = Optional.empty();
                    }
                    if (ret.isPresent()) {
                        objs[i] = ret.get();
                    } else {
                        mached = null;
                        break;
                    }
                }
                if (mached != null) {
                    System.arraycopy(objs, 0, retObjs, 0, objs.length);
                    return method;
                }

            }
            c = c.getSuperclass();
        }

        return null;
    }

    private static Method getMethod(LuaState L, Class<?> clazz, String methodName, Object[] retObjs, int top) {
        Object[] objs = new Object[top - 1];

        Method[] methods = clazz.getMethods();
        Method method = null;

        // gets method and arguments
        for (int i = 0; i < methods.length; i++) {
            if (!methods[i].getName().equals(methodName))

                continue;

            Class<?>[] parameters = methods[i].getParameterTypes();
            if (parameters.length != top - 1)
                continue;

            boolean okMethod = true;

            for (int j = 0; j < parameters.length; j++) {
                Optional<Object> ret;
                try {
                    ret = compareTypes(L, parameters[j], j + 2);
                } catch (LuaException e) {
                    ret = Optional.empty();
                }
                if (ret.isPresent()) {
                    objs[j] = ret.get();
                } else {
                    okMethod = false;
                    break;
                }
            }

            if (okMethod) {
                method = methods[i];
                System.arraycopy(objs, 0, retObjs, 0, objs.length);

                break;
            }

        }

        return method;
    }
}
 