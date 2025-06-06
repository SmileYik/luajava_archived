package org.keplerproject.luajava;

public class LuajavaTest {

    static {
        LoadLibrary.load();
    }

    @org.junit.jupiter.api.Test
    public void consoleTest() {
        Console.main(new String[]{});
    }
}
