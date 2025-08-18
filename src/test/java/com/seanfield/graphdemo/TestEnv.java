package com.seanfield.graphdemo;

public class TestEnv {
    public static void main(String[] args) {
        System.getenv().forEach((key, value) ->
                System.out.println(key + " = " + value)
        );
    }
}

