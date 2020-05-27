package com.hello.chapter1;

public class TryConcurrency {

    private  static  void  readFromData()
    {
        try {
            Thread.sleep(1000*10L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
