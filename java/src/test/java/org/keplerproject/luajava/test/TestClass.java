/*
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


package org.keplerproject.luajava.test;

import org.junit.jupiter.api.Test;
import org.keplerproject.luajava.*;

public class TestClass {

    static {
        LoadLibrary.load();
    }

    @Test
    public void test() throws LuaException {
        LuaState L = LuaStateFactory.newLuaState();
        L.openBase();

        TestClassInner test = new TestClassInner(L);

        test.jf.register("javaFuncTest");

        test.Lf.LdoString(" f=javaFuncTest(); print(f) ");

        L.close();
    }


    public static class TestClassInner {
        public final LuaState Lf;

        public JavaFunction jf;

        public TestClassInner(LuaState L) {
            this.Lf = L;

            jf = new JavaFunction(L) {
                public int execute() {
                    this.L.pushString("Returned String");
                    System.out.println("Printing from Java Function");
                    return 1;
                }
            };
        }
    }
}