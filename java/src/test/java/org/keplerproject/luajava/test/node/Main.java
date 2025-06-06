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

package org.keplerproject.luajava.test.node;

import org.junit.jupiter.api.Test;
import org.keplerproject.luajava.LoadLibrary;
import org.keplerproject.luajava.LuaException;

import java.util.Iterator;
import java.util.List;


/**
 * Uses the node example with a file that is a representation of a
 * hibernate configuration XML file.
 *
 * @author thiago
 */
public class Main {

    static {
        LoadLibrary.load();
    }

    @Test
    public void test() throws LuaException {
        LuaNode node = LuaNode.proccessFile("test/hibernateConfig.lua");

        System.out.println(node.getName());
        System.out.println(node.getAttribute("name"));
        System.out.println(node.getAttribute("table"));

        List list = node.getChildren("property");

        for (Iterator iter = list.iterator(); iter.hasNext(); ) {
            LuaNode elem = (LuaNode) iter.next();
            System.out.println(elem.getName());
            System.out.println(elem.getAttribute("type"));
        }

        list = node.getChild("many-to-one").getChildren();

        for (Iterator iter = list.iterator(); iter.hasNext(); ) {
            LuaNode elem = (LuaNode) iter.next();
            System.out.println(elem.getName());
            System.out.println(elem.getValue());
        }
    }
}
