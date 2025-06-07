package org.eu.smileyik.luajava.type;

import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaState;

public class LuaUserdata extends LuaObject {
    public static final String TYPE_NAME = LuaType.typeName(LuaType.USERDATA);

    /**
     * Creates a reference to an object in the given index of the stack
     * <strong>SHOULD NOT USE CONSTRUCTOR DIRECTLY</strong>
     *
     * @param L
     * @param index of the object on the lua stack
     * @see LuaObject#create(LuaState, int)
     */
    protected LuaUserdata(LuaState L, int index) {
        super(L, index);
    }

    @Override
    public boolean isUserdata() {
        return true;
    }

    @Override
    public int type() {
        return LuaType.USERDATA;
    }

    @Override
    public String toString() {
        return TYPE_NAME;
    }

    @Override
    public boolean isFunction() {
        return false;
    }

    @Override
    public boolean isString() {
        return false;
    }

    @Override
    public boolean isNumber() {
        return false;
    }

    @Override
    public boolean isBoolean() {
        return false;
    }

    @Override
    public boolean isTable() {
        return false;
    }
}
