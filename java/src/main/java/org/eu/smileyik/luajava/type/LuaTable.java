package org.eu.smileyik.luajava.type;

import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaState;

public class LuaTable extends LuaObject {
    public static final String TYPE_NAME = LuaType.typeName(LuaType.TABLE);

    public LuaTable(LuaState L, int index) {
        super(L, index);
    }

    @Override
    public String toString() {
        return TYPE_NAME;
    }

    @Override
    public boolean isTable() {
        return true;
    }

    @Override
    public int type() {
        return LuaType.TABLE;
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
    public boolean isUserdata() {
        return false;
    }
}
