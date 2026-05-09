package example.practice;

import java.util.Scanner;
import example.practice.user.UserStructure;


public class Main {

    static void welcomeMessage(){
        System.out.println(
                "Welcome."+"\\"+
                "We hope you enjoy the the game");
    }

    public static void main(String[] args){
        Scanner input = new Scanner(System.in);

        System.out.println("Enter Your Name: ");
        String userName = input.next();
        System.out.println();

        UserStructure Player = new UserStructure(userName);



    }
}
