package main;

import java.math.BigInteger;

/**
 */

public class TestingProg {
    public static void main(String args[]){
        BigInteger a = BigInteger.valueOf(20);
        BigInteger b = BigInteger.valueOf(3);

        BigInteger c = getDouble(a);
        BigInteger d = c.add(b);
        BigInteger e = a.divide(b);

        System.out.println("a = " + a.toString());
        System.out.println("b = " + b.toString());
        System.out.println("c = " + c.toString());
        System.out.println("d = " + d.toString());
        System.out.println("e = " + e.toString());
    }

    static BigInteger getDouble(BigInteger a){
        return a.add(a);
    }
}
